//This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/. 
//
//Author: Víctor Arellano Vicente (Ivelate)
//
//Hud standard vertex shader.

#version 330 core

in vec2 location;
in vec4 color;

out vec4 Color;

void main(){
	Color=color;
	gl_Position=vec4(location.xy,0.0,1.0);
}