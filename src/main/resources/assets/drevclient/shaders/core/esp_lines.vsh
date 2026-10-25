#version 330

#moj_import <minecraft:globals.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec4 Color;
in vec3 Normal;
in float LineWidth;

out vec4 vertexColor;

// Screen-space thick lines without fog, so ESP stays visible at any distance.
void main() {
    vec4 startClip = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vec4 endClip = ProjMat * ModelViewMat * vec4(Position + Normal, 1.0);

    vec3 startNdc = startClip.xyz / startClip.w;
    vec3 endNdc = endClip.xyz / endClip.w;

    vec2 dir = normalize((endNdc.xy - startNdc.xy) * ScreenSize);
    vec2 offset = vec2(-dir.y, dir.x) * LineWidth / ScreenSize;
    if (offset.x < 0.0) {
        offset = -offset;
    }

    float side = (gl_VertexID % 2 == 0) ? 1.0 : -1.0;
    gl_Position = vec4((startNdc + vec3(offset * side, 0.0)) * startClip.w, startClip.w);
    vertexColor = Color;
}
