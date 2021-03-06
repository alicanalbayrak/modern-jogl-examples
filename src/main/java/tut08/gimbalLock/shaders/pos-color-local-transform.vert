#version 330

// Attribute
#define POSITION    0
#define COLOR       1

layout (location = POSITION) in vec4 position;
layout (location = COLOR) in vec4 color;

smooth out vec4 theColor;

uniform mat4 cameraToClipMatrix;
uniform mat4 modelToCameraMatrix;

void main()
{
    vec4 cameraPos = modelToCameraMatrix * position;
    gl_Position = cameraToClipMatrix * cameraPos;
    theColor = color;
}
