//This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/. 
//
//Author: Víctor Arellano Vicente (Ivelate)
//
//Depth peeling vertex shader

#version 330 core

uniform mat4 vpMatrix;
uniform mat4 modelMatrix;

in vec3 location;
in vec3 normal;

out vec3 Normal;

void main()
{
	Normal=normal;
	vec4 modelLocation=modelMatrix * vec4(location.xyz,1.0);
	vec4 finalLoc=vpMatrix * modelLocation;
	gl_Position=finalLoc;
}