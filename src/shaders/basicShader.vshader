#version 330 core

uniform mat4 vpMatrix;
uniform mat4 modelMatrix;

in vec3 location;
in float properties;

out vec2 Properties;
out vec3 Location;

void main()
{
	float normal=round(properties/1000);
	Location=location;
	Properties=vec2(properties-normal*1000,normal);
	vec4 modelLocation=modelMatrix * vec4(location.xyz,1.0);
	gl_Position=vpMatrix * modelLocation;
}