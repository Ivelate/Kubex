#version 330 core

uniform mat4 vpMatrix;
uniform mat4 modelMatrix;

in vec3 location;

void main()
{
	vec4 modelLocation=modelMatrix * vec4(location.xyz,1.0);
	gl_Position=vpMatrix * modelLocation;
}