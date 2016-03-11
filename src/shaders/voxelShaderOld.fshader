#version 330 core

#define SQRT2 1.41421356
uniform sampler2DArray tiles;
uniform sampler2DArrayShadow shadowMap;
uniform vec3 sunNormal;

uniform vec4 splitDistances;
uniform mat4 shadowMatrixes[4];

in vec3 Location;
in vec3 ModelLocation;
in vec2 Properties;
in vec2 Brightness;
const vec3[] normals=vec3[10](vec3(-1,0,0),vec3(1,0,0),vec3(0,-1,0),vec3(0,1,0),vec3(0,0,-1),vec3(0,0,1),vec3(0,1,0),vec3(0,1,0),vec3(0,1,0),vec3(0,1,0));/*vec3(-SQRT2,0,SQRT2),vec3(SQRT2,0,SQRT2),vec3(-SQRT2,0,-SQRT2),vec3(SQRT2,0,-SQRT2)};*/

layout(location = 0) out vec4 outcolor;
layout(location = 1) out vec4 normalBrightness;
/*layout(location = 2) out vec2 brightness;
layout(location = 1) out vec3 outPosition;
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
		//normal=Properties.y<0.5f? vec3(-1,0,0) : vec3(1,0,0);
		outColor=texture(tiles,
			vec3(
				Location.z,
				1-Location.y,
				floor(Properties.x+0.5)
				));
	}
	else if(Properties.y<3.5f)
	{
		//normal=Properties.y<2.5f? vec3(0,-1,0) : vec3(0,1,0);
		outColor=texture(tiles,
			vec3(
				Location.x,
				Location.z,
				floor(Properties.x+0.5)
				));
	}
	else{
		//normal=Properties.y<4.5f? vec3(0,0,-1) : Properties.y<5.5f? vec3(0,0,1): Properties.y<6.5f? vec3(0,0,1): Properties.y<7.5f? vec3(0,0,1): Properties.y<8.5f? vec3(0,0,1): vec3(0,0,1);
		outColor=texture(tiles,
			vec3(
				Location.x,
				1-Location.y,
				floor(Properties.x+0.5)
				));
	}
	if(outColor.w<0.5) discard;
	
	float z = gl_FragCoord.z / gl_FragCoord.w;
	
	int sindex=0;
	if(z>splitDistances.x)
	{
		if(z<splitDistances.y) sindex=1;
		else if(z<splitDistances.z) sindex=2;
		else sindex=3;
	}
	
	float shadowAttenuation=1;
	float dotsun=dot(sunNormal,normal);
	float sunsetdot=dot(sunNormal,vec3(0,1,0));
	if(dotsun>0 && sunsetdot>-0.2){
		//float bias = 0.000008 * splitDistances[sindex];

		float sunsetAttenuation=sunsetdot>0?1:(sunsetdot+0.2)*5;
		float bias = 0.0002;
		
		vec4 sunLocation=shadowMatrixes[sindex]*vec4(ModelLocation,1);
		//sunLocation=sunLocation/sunLocation.w;
		
		vec4 shadowLoc=vec4(sunLocation.x,sunLocation.y,float(floor(sindex+0.5f)),sunLocation.z-bias);
		float shadow=texture(shadowMap,shadowLoc);
		//outColor=((shadow - vec4(0.8,0.8,0.8,0.8)) * 5) * ((shadow - vec4(0.8,0.8,0.8,0.8)) * 5);
		shadow=shadow*sqrt(sqrt(dotsun*dotsun*dotsun))*sunsetAttenuation;
		/*sunLocation.w=sunLocation.z;
		sunLocation.z=float(floor(sindex+0.5f));

		float shadow=texture(shadowMap,sunLocation);*/
		/*if(shadow.z < sunLocation.z -bias) shadowAttenuation=0.3f;
		else{
			shadowAttenuation=min(dotsun*dotsun*2,0.4) + 0.6;
		}*/
		
		shadowAttenuation=0.7f*shadow + 0.3f;
	}
	else shadowAttenuation=0.3f;
	
	//outColor=vec4(shadowMatrixes[sindex][3][3],0,0,1);
	
	float daylightBrightness=Brightness.x/**daylightAmount*/*shadowAttenuation;
	float finalBrightness=Brightness.y>daylightBrightness?Brightness.y:daylightBrightness;
	//float finalBrightness=1;
	outColor=outColor*vec4(finalBrightness,finalBrightness,finalBrightness,1.0);
	
	/*if(sindex==0) outColor+=vec4(0.1f,0,0,1);
	if(sindex==1) outColor+=vec4(0,0.1f,0,1);
	if(sindex==2) outColor+=vec4(0,0,0.1f,1);*/
	
	outcolor=outColor;
 	/*float fog = clamp(exp(-fogdensity * z * z), 0.2, 1);
  	outcolor = mix(fogcolor*((daylightAmount-0.15)*1.17647), outColor, fog);*/
  	//outPosition=(shadow.xyz - 0.5)*2;
  	//outNormalsAndLight=vec3(Properties.y,Brightness.x,Brightness.y);
  	/*if(sunLocation.x>0.2f||sunLocation.x<-0.2f||sunLocation.y>0.2f||sunLocation.y<-0.2f||sunLocation.z>0.2f||sunLocation.z<-0.2f) outNormalsAndLight=vec3(0,0,0);
  	else outNormalsAndLight=(vec3((sunLocation.x+1)/2,(sunLocation.y+1)/2,(sunLocation.z+1)/2));*/
  	//gl_FragColor = gl_FragColor - vec4(0.35,0.35,0.35,0.35);
  	
  	
  	normalBrightness=vec4((normal.x+1)/2,(normal.y+1)/2,Brightness.x,Brightness.y);
  	//gl_FragColor =outColor;
}