#version 330

// Outputs
#define FRAG_COLOR  0

in vec2 mapping;

layout (location = FRAG_COLOR) out vec4 outputColor;

uniform float sphereRadius;
uniform vec3 cameraSpherePos;

layout(std140) uniform;

uniform Material
{
    vec4 diffuseColor;
    vec4 specularColor;
    float specularShininess;
} mtl;

struct PerLight
{
    vec4 cameraSpaceLightPos;
    vec4 lightIntensity;
};

const int NUMBER_OF_LIGHTS = 2;

uniform Light
{
    vec4 ambientIntensity;
    float lightAttenuation;
    PerLight lights[NUMBER_OF_LIGHTS];
} lgt;


float calcAttenuation(in vec3 cameraSpacePosition, in vec3 cameraSpaceLightPos, out vec3 lightDirection)
{
    vec3 lightDifference =  cameraSpaceLightPos - cameraSpacePosition;
    float lightDistanceSqr = dot(lightDifference, lightDifference);
    lightDirection = lightDifference * inversesqrt(lightDistanceSqr);

    return (1 / (1.0 + lgt.lightAttenuation * lightDistanceSqr));
}

uniform Projection
{
    mat4 cameraToClipMatrix;
};

vec4 computeLighting(in PerLight lightData, in vec3 cameraSpacePosition, in vec3 cameraSpaceNormal)
{
    vec3 lightDir;
    vec4 lightIntensity;
    if(lightData.cameraSpaceLightPos.w == 0.0)
    {
        lightDir = vec3(lightData.cameraSpaceLightPos);
        lightIntensity = lightData.lightIntensity;
    }
    else
    {
        float atten = calcAttenuation(cameraSpacePosition, lightData.cameraSpaceLightPos.xyz, lightDir);
        lightIntensity = atten * lightData.lightIntensity;
    }

    vec3 surfaceNormal = normalize(cameraSpaceNormal);
    float cosAngIncidence = dot(surfaceNormal, lightDir);
    cosAngIncidence = cosAngIncidence < 0.0001 ? 0.0 : cosAngIncidence;

    vec3 viewDirection = normalize(-cameraSpacePosition);

    vec3 halfAngle = normalize(lightDir + viewDirection);
    float angleNormalHalf = acos(dot(halfAngle, surfaceNormal));
    float exponent = angleNormalHalf / mtl.specularShininess;
    exponent = -(exponent * exponent);
    float gaussianTerm = exp(exponent);

    gaussianTerm = cosAngIncidence != 0.0 ? gaussianTerm : 0.0;

    vec4 lighting = mtl.diffuseColor * lightIntensity * cosAngIncidence;
    lighting += mtl.specularColor * lightIntensity * gaussianTerm;

    return lighting;
}

void impostor(out vec3 cameraPos, out vec3 cameraNormal)
{
	float lensqr = dot(mapping, mapping);
	if(lensqr > 1.0)
            discard;
		
	cameraNormal = vec3(mapping, sqrt(1.0 - lensqr));
	cameraPos = (cameraNormal * sphereRadius) + cameraSpherePos;
}

void main()
{
    vec3 cameraPos;
    vec3 cameraNormal;

    impostor(cameraPos, cameraNormal);

    vec4 accumLighting = mtl.diffuseColor * lgt.ambientIntensity;
    for(int light = 0; light < NUMBER_OF_LIGHTS; light++)
    {
        accumLighting += computeLighting(lgt.lights[light], cameraPos, cameraNormal);
    }

    outputColor = sqrt(accumLighting); //2.0 gamma correction
}