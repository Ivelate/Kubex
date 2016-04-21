//This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/. 
//
//Author: Víctor Arellano Vicente (Ivelate)
//
//Terrain voxel vertex shader.

#version 330 core

uniform mat4 vpMatrix;
uniform mat4 modelMatrix;
uniform mat4 sunMvpMatrix;

in vec3 location;
in float properties;
in vec2 brightness;

out vec3 Location;
out vec3 ModelLocation;
out vec2 Properties;
out vec2 Brightness;

void main(){
	float normal=round(properties/1000);
	Properties=vec2(properties-normal*1000,normal);
	Location=location;
	Brightness=brightness;
	vec4 modelLocation=modelMatrix * vec4(location.xyz,1.0);
	gl_Position=vpMatrix * modelLocation;
	ModelLocation=vec3(modelLocation.xyz);
}