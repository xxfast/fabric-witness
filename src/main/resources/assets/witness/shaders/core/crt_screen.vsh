#version 330

// The world `text` shader plus a position on the screen's glass, for the scanlines and static the
// fragment shader lays over a lit screen frame (rules/minecraft/05-1-screen-frame.md#the-picture).

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:sample_lightmap.glsl>
#moj_import <minecraft:globals.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;

uniform sampler2D Sampler2;

out float sphericalVertexDistance;
out float cylindricalVertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;
out vec2 glassPosition;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    sphericalVertexDistance = fog_spherical_distance(Position);
    cylindricalVertexDistance = fog_cylindrical_distance(Position);
    vertexColor = Color * sample_lightmap(Sampler2, UV2);
    texCoord0 = UV0;

    // Position is camera-relative, and the camera sits at CameraBlockPos - CameraOffset (terrain.vsh
    // adds the same pair the other way round). Anchoring to the world keeps the bands still while the
    // player moves; wrapping the block part at 1024 keeps the floats small enough for a 1/64 block grain.
    vec3 world = Position + vec3(CameraBlockPos & 1023) - CameraOffset;
    // Frames only ever face a horizontal direction, so the glass's across axis is x or z and its up axis
    // is always y. x + z is the across coordinate for either, give or take a constant.
    glassPosition = vec2(world.x + world.z, world.y);
}
