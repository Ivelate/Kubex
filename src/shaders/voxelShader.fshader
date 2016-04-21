//This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/. 
//
//Author: Víctor Arellano Vicente (Ivelate)
//
//Terrain voxel shader. Renders the default color, normal, brightness and depth to textures

#version 330 core

uniform sampler2DArray tiles;

in vec3 Location;
in vec3 ModelLocation;
in vec2 Properties;
in vec2 Brightness;
const vec3[] normals=vec3[10](vec3(-1,0,0),vec3(1,0,0),vec3(0,-1,0),vec3(0,1,0),vec3(0,0,-1),vec3(0,0,1),vec3(-1,1,0),vec3(1,1,0),vec3(-1,-1,0),vec3(1,-1,0));

layout(location = 0) out vec4 outcolor;
layout(location = 1) out vec4 normalBrightness;

float adjFract(float val)
{
return (fract(val)*0.999f) + 0.0005;
}
void main(){
vec4 outColor;
vec3 normal;
	normal=normals[int( Properties.y+0.5f)]; //Decompresses the normal
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