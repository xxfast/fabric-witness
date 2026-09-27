#version 330

// A lit screen frame's picture: the world `text` shader, with scanlines and static multiplied in
// (rules/minecraft/05-1-screen-frame.md#the-picture).

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:globals.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
in vec2 glassPosition;

out vec4 fragColor;

// Two bands per block pixel.
const float SCANLINES_PER_BLOCK = 32.0;

// The darkest part of a band against the picture.
const float SCANLINE_DEPTH = 0.125;

// A speck is a quarter of a block pixel across.
const float SPECKS_PER_BLOCK = 64.0;

// The furthest a speck strays from the picture, lighter or darker.
const float STATIC_STRENGTH = 0.02;

const float TAU = 6.2831853;

float hash(vec3 p) {
    p = fract(p * vec3(0.1031, 0.1030, 0.0973));
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

// 1 while each feature covers a few screen pixels, 0 once it would be under one. Past that point a
// band or a speck can only alias, so it fades out rather than shimmering at range.
float resolved(vec2 coordinate, float featuresPerBlock) {
    vec2 perPixel = fwidth(coordinate) * featuresPerBlock;
    return 1.0 - smoothstep(0.2, 0.45, max(perPixel.x, perPixel.y));
}

void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
    if (color.a < 0.1) {
        discard;
    }

    float band = 0.5 - 0.5 * cos(TAU * glassPosition.y * SCANLINES_PER_BLOCK);
    float scanline = 1.0 - SCANLINE_DEPTH * band * resolved(glassPosition.yy, SCANLINES_PER_BLOCK);

    // GameTime is the day's fraction, so this is the tick: the grain changes once a tick.
    float tick = floor(GameTime * 24000.0);
    vec2 speck = floor(glassPosition * SPECKS_PER_BLOCK);
    float noise = hash(vec3(speck, tick)) * 2.0 - 1.0;
    float grain = 1.0 + STATIC_STRENGTH * noise * resolved(glassPosition, SPECKS_PER_BLOCK);

    color.rgb *= scanline * grain;
    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}
