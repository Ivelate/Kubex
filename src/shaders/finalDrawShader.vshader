#version 330 core

in vec2 location;
out vec2 pos;

void main(){
	pos=location;
	gl_Position=vec4(location.xy,0.0,1.0);
}