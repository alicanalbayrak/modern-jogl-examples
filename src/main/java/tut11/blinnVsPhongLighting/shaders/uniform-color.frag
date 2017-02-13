#version 330

// Outputs
#define FRAG_COLOR  0

uniform vec4 objectColor;

layout (location = FRAG_COLOR) out vec4 outputColor;

void main()
{
    outputColor = objectColor;
}