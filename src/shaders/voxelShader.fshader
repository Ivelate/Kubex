#version 330 core

#define SQRT2 1.41421356
uniform sampler2DArray tiles;

in vec3 Location;
in vec3 ModelLocation;
in vec2 Properties;
in vec2 Brightness;
const vec3[] normals=vec3[10](vec3(-1,0,0),vec3(1,0,0),vec3(0,-1,0),vec3(0,1,0),vec3(0,0,-1),vec3(0,0,1),vec3(0,1,0),vec3(0,1,0),vec3(0,1,0),vec3(0,1,0));/*vec3(-SQRT2,0,SQRT2),vec3(SQRT2,0,SQRT2),vec3(-SQRT2,0,-SQRT2),vec3(SQRT2,0,-SQRT2));*/

layout(location = 0) out vec4 outcolor;
layout(location = 1) out vec4 normalBrightness;

float adjFract(float val)
{
return (fract(val)*0.999f) + 0.0005;
}
void main(){
vec4 outColor;
vec3 normal;
	normal=normals[int( Properties.y+0.5f)];
	if(Properties.y<1.5f)
	{
		outColor=texture(tiles,
			vec3(
				Location.z,
				1-Location.y,
				floor(Properties.x+0.5)
				));
	}
	else if(Properties.y<3.5f)
	{
		outColor=texture(tiles,
			vec3(
				Location.x,
				Location.z,
				floor(Properties.x+0.5)
				));
	}
	else{
		outColor=texture(tiles,
			vec3(
				Location.x,
				1-Location.y,
				floor(Properties.x+0.5)
				));
	}
	if(outColor.w<0.5) discard;
	
	outcolor=outColor;

  	normalBrightness=vec4((normal.x+1)/2,(normal.y+1)/2,Brightness.x,Brightness.y);
}