#version 330

// Attribute
#define POSITION    0

layout (location = POSITION) in vec4 position;

uniform float loopDuration;
uniform float time;

void main()
{
    float timeScale = 3.14159f * 2.0f / loopDuration;

    float currentTime = mod(time, loopDuration);
    vec4 totalOffset = vec4(cos(currentTime * timeScale) * 0.5f, sin(currentTime * timeScale) * 0.5f, 0.0f, 0.0f);

    gl_Position = position + totalOffset;
}