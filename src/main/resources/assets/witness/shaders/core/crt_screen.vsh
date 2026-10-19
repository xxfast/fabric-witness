#version 330
#extension GL_ARB_separate_shader_objects : require

// The world `text` shader plus a position on the screen's glass, for the scanlines and static the
// fragment shader lays over a lit screen frame (rules/minecraft/05-1-screen-frame.md#the-picture).

#include <minecraft:fog.glsl>
#include <minecraft:sample_lightmap.glsl>
#include <minecraft:globals.glsl>
#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 UV0;
layout(location = 3) in ivec2 UV2;

uniform sampler2D Sampler2;

layout(location = 0) out float sphericalVertexDistance;
layout(location = 1) out float cylindricalVertexDistance;
layout(location = 2) out vec4 vertexColor;
layout(location = 3) out vec2 texCoord0;
layout(location = 4) out vec2 glassPosition;

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
