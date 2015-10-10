#version 330 core

in vec2 location;

void main(){
	gl_Position=vec4(location.xy,1.0,1.0);
}