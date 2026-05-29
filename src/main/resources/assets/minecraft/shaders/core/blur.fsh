#version 150

uniform sampler2D InputSampler;
layout(std140) uniform ThunderHackCustom {
    vec4 color1;
    vec4 color2;
    vec4 color3;
    vec4 color4;
    vec2 uSize;
    vec2 uSize2;
    vec2 uLocation;
    vec2 InputResolution;
    float radius;
    float blend;
    float alpha;
    float outline;
    float glow;
    float thickness;
    float start;
    float end;
    float time;
    float Time;
    float Quality;
    float Brightness;
};
in vec2 texCoord;

out vec4 fragColor;

float roundedBoxSDF(vec2 center, vec2 size, float radius) {
    return length(max(abs(center) - size + radius, 0.0)) - radius;
}

vec4 blur() {
    #define TAU 6.28318530718

    vec2 Radius = Quality / InputResolution.xy;

    vec2 uv = gl_FragCoord.xy / InputResolution.xy;
    vec4 Color = texture(InputSampler, uv);

    float step =  TAU / 16.0;

    for (float d = 0.0; d < TAU; d += step) {
        for (float i = 0.2; i <= 1.0; i += 0.2) {
            Color += texture(InputSampler, uv + vec2(cos(d), sin(d)) * Radius * i);
        }
    }

    Color /= 80;
    return (Color + color1) * Brightness;
}

void main() {
    vec2 halfSize = uSize / 2.0;
    float smoothedAlpha =  (1.0 - smoothstep(0.0, 1.0, roundedBoxSDF(gl_FragCoord.xy - uLocation - halfSize, halfSize, radius)));
    fragColor = vec4(blur().rgb, smoothedAlpha);
}
