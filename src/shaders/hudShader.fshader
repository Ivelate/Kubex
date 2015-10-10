#version 330 core
//uniform sampler2D hud;
in vec4 Color;
void main(){
	gl_FragColor = Color;//texture2D(hud,Texcoord);
}