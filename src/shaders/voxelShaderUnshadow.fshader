#version 330 core

#define SQRT2 1.41421356
uniform sampler2DArray tiles;
uniform sampler2DArrayShadow shadowMap;
uniform float daylightAmount;
uniform vec3 sunNormal;

uniform vec4 splitDistances;
uniform mat4 shadowMatrixes[4];

in vec3 Location;
in vec3 ModelLocation;
in vec2 Properties;
in vec2 Brightness;
const vec4 fogcolor = vec4(0.7, 0.9, 1.0, 1.0);
const float fogdensity = .00001;
const vec3[] normals=vec3[10](vec3(-1,0,0),vec3(1,0,0),vec3(0,-1,0),vec3(0,1,0),vec3(0,0,-1),vec3(0,0,1),vec3(0,1,0),vec3(0,1,0),vec3(0,1,0),vec3(0,1,0));/*vec3(-SQRT2,0,SQRT2),vec3(SQRT2,0,SQRT2),vec3(-SQRT2,0,-SQRT2),vec3(SQRT2,0,-SQRT2)};*/

layout(location = 0) out vec4 outcolor;
/*layout(location = 1) out vec3 outPosition;
layout(location = 2) out vec3 outNormalsAndLight;*/

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
	else
	{
		outColor=texture(tiles,
			vec3(
				Location.x,
				1-Location.y,
				floor(Properties.x+0.5)
				));
	}
	if(outColor.w<0.5) discard;
	
	
	float z = gl_FragCoord.z / gl_FragCoord.w;
	
	outColor=vec4(1,1,1,1);
	
	float shadowAttenuation=1;
	
	float daylightBrightness=Brightness.x*daylightAmount*shadowAttenuation;
	float finalBrightness=Brightness.y>daylightBrightness?Brightness.y:daylightBrightness;
	outColor=outColor*vec4(finalBrightness,finalBrightness,finalBrightness,1.0);
	
 	float fog = clamp(exp(-fogdensity * z * z), 0.2, 1);
  	outcolor = mix(fogcolor*((daylightAmount-0.15)*1.17647), outColor, fog);
 }