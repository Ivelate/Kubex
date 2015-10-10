#version 330 core
struct Color
{
	float r;
	float g;
	float b;
};
uniform Color color;
void main(){
	gl_FragColor = vec4(color.r,color.g,color.b,1.0f);
}