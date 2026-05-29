#version 150

#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

in vec3 Position;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position + ModelOffset, 1.0);
}
