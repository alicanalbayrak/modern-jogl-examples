#version 330

// Attribute
#define FRAG_COLOR  0

smooth in vec4 theColor;

layout (location = FRAG_COLOR) out vec4 outputColor;

void main()
{
    outputColor = theColor;
}
