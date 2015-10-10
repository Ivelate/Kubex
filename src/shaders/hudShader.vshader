#version 330 core

in vec2 location;
in vec4 color;

out vec4 Color;

void main(){
	Color=color;
	gl_Position=vec4(location.xy,0.0,1.0);
}