#version 330 core

in vec2 location;
out vec2 pos;

void main(){
	pos=vec2((location.x+1)/2,(location.y+1)/2);
	gl_Position=vec4(location.xy,0.0,1.0);
}