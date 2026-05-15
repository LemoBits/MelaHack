#version 150

uniform vec2 uSize;
uniform float Time;

out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);

    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));

    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    mat2 m = mat2(1.6, 1.2, -1.2, 1.6);

    for (int i = 0; i < 5; i++) {
        value += amplitude * noise(p);
        p = m * p + vec2(2.9, 3.7);
        amplitude *= 0.5;
    }

    return value;
}

void main() {
    vec2 uv = gl_FragCoord.xy / uSize.xy;
    vec2 p = uv * 2.0 - 1.0;
    p.x *= uSize.x / uSize.y;

    float t = Time * 0.045;
    vec2 warp = vec2(
        fbm(p * 1.7 + vec2(t, -t * 0.6)),
        fbm(p * 1.7 + vec2(-t * 0.5, t * 0.8))
    );

    vec2 q = p + (warp - 0.5) * 0.55;
    float glow = smoothstep(1.15, 0.0, length(q + vec2(0.08 * sin(t * 1.4), -0.06 * cos(t * 1.1))));
    float bandA = smoothstep(0.18, 0.88, fbm(q * 2.4 + vec2(t * 1.2, -t * 0.7)));
    float bandB = smoothstep(0.22, 0.92, fbm(q * 3.8 - vec2(t * 0.6, t * 0.4)));
    float streak = smoothstep(0.3, 0.95, fbm(vec2(q.x * 4.5, q.y * 1.1 + sin(t + q.x * 2.0))));

    vec3 base = mix(vec3(0.010, 0.045, 0.030), vec3(0.020, 0.170, 0.145), bandA);
    vec3 accent = mix(vec3(0.005, 0.120, 0.280), vec3(0.015, 0.520, 0.700), bandB);
    vec3 mist = vec3(0.010, 0.240, 0.330) * streak;

    vec3 color = mix(base, accent, 0.55);
    color += mist * 0.35;
    color += glow * vec3(0.03, 0.22, 0.18);
    color *= 0.72 + 0.28 * smoothstep(1.15, 0.0, length(p));

    fragColor = vec4(color, 1.0);
}
