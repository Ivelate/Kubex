//This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/. 
//
//Author: Víctor Arellano Vicente (Ivelate)
//
//Hud fragment shader. Simply prints the color provided by the input

#version 330 core

in vec4 Color;
void main(){
	gl_FragColor = Color;
}