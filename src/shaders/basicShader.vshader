//This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/. 
//
//Author: Víctor Arellano Vicente (Ivelate)
//
//Standard vertex shader used when calculating shadows. As the shadows needs to fetch the texture of each polygon to check if it is transparent and light can pass through
//them or not, both the texture used and the cube normal have to be unpacked from the input


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