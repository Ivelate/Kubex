#version 330 core

uniform sampler2D colorTex;
uniform sampler2D brightnessNormalTex;
uniform sampler2D baseFboDepthTex;
uniform sampler2DArray liquidLayersTex;
uniform sampler2DArrayShadow shadowMap;
uniform int liquidLayersTexLength;
uniform float cnear;
uniform float cfar;
uniform float cwidth;
uniform float cheight;

uniform float time;

const float fogdensity = .00001;
const vec4 fogcolor = vec4(0.6, 0.74, 0.8, 1.0);
uniform float daylightAmount;

uniform vec3 sunNormal;
uniform vec4 splitDistances;
uniform mat4 shadowMatrixes[4];
const vec2 poissonDisk[4] = vec2[](
  				vec2( -0.94201624, -0.39906216 ),
  				vec2( 0.94558609, -0.76890725 ),
  				vec2( -0.094184101, -0.92938870 ),
  				vec2( 0.34495938, 0.29387760 )
			);

uniform mat4 viewMatrix;
uniform mat4 projectionMatrix;
in vec2 pos;
in vec3 FarFaceLocation;
in vec3 FarFaceCamViewLocation;

layout(location = 0) out vec4 outcolor;

void main()
{
	float mnearfar=cnear*cfar;
	float snearfar=cfar-cnear;
	float z=texture(baseFboDepthTex,vec2(pos.x,pos.y)).x;
	float trueDepth=-mnearfar / ((z * snearfar) - cfar);
	
	vec3 lookVector=normalize(FarFaceLocation);
	
	vec3 normal=vec3(texture(brightnessNormalTex,vec2(pos.x,pos.y)).xy * 2 -vec2(1,1),0);
	float zsq=1-(normal.x*normal.x + normal.y*normal.y);
	if(zsq>0){
		normal.z=sqrt(zsq);
		if(dot(normal,lookVector)>0) normal.z=-normal.z;	
	}
	
	vec2 Brightness=texture(brightnessNormalTex,vec2(pos.x,pos.y)).zw;
	
	vec3 worldPosition=FarFaceLocation*trueDepth/cfar;
	
	//RAYTRACE WATER DISTANCE
	float waterd=0;
	float begind=-1;
	
	float firstWaterDepth;
	vec3 firstWaterNormal;
	
	for(int i=0;i<liquidLayersTexLength;i++)
	{ 
		float dw=texture(liquidLayersTex,vec3(pos.x,pos.y,floor(i+0.5))).x;
		if(dw==1) break;
		
		if(begind<0) {
			begind=-mnearfar / ((dw * snearfar) - cfar);
			if(i==0){
				firstWaterDepth=begind;
				firstWaterNormal=normalize(vec3(0,1,0));
			}
		}
		else
		{
			float finald=-mnearfar / ((dw * snearfar) - cfar);
			
			//REMINDER OF THE PAST: 1/(invProjZ.x*pos.x + invProjZ.y*pos.y + invProjZ.z*dw + invProjZ.w); //Badbadbad, getting w, assuming 1=z , normalizing z based upon w, so 1/...
			
			waterd+=(finald-begind);
			begind=-1;
		}
	}
	if(begind>=0){
		float finald=trueDepth;
		waterd+=(finald-begind);
		begind=-1;
	}
		
	bool water=waterd>0;
	float outw=water?0.8:1.0;
	
	//START ILLUMINATION
	
	outcolor=texture2D(colorTex,vec2(pos.x,pos.y));
	
	if(z<1)
	{
		if(water) {
			trueDepth=firstWaterDepth;
			normal=firstWaterNormal;
			worldPosition=FarFaceLocation*trueDepth/cfar;
		}
		//SHADOWS
		int sindex=0;
		if(trueDepth>splitDistances.x)
		{
			if(trueDepth<splitDistances.y) sindex=1;
			else if(trueDepth<splitDistances.z) sindex=2;
			else sindex=3;
		}
	
		float shadowAttenuation=1;
		float dotsun=dot(sunNormal,normal);
		float sunsetdot=dot(sunNormal,vec3(0,1,0));
		if(dotsun>0 && sunsetdot>-0.2)
		{
			float sunsetAttenuation=sunsetdot>0?1.0:(sunsetdot+0.2)*5;
			vec3 smallnormal=normal * length(worldPosition)*0.01;
			vec4 sunLocation=shadowMatrixes[sindex]*vec4(worldPosition+smallnormal,1);
			float shadow=0;
			for(int i=0;i<4;i++) {
				vec4 shadowLoc=vec4(sunLocation.x+poissonDisk[i].x/2500,sunLocation.y+poissonDisk[i].y/2500,float(floor(sindex+0.5f)),sunLocation.z);
				shadow+=texture(shadowMap,shadowLoc)/4;
			}
			shadow=shadow*sqrt(sqrt(dotsun*dotsun*dotsun))*sunsetAttenuation;
				
			shadowAttenuation=0.7f*shadow + 0.3f;
		}
		else shadowAttenuation=0.3f;
	
		float daylightBrightness=Brightness.x*daylightAmount*shadowAttenuation;
		float finalBrightness=Brightness.y>daylightBrightness?Brightness.y:daylightBrightness;
	
		if(!water) outcolor=outcolor*vec4(finalBrightness,finalBrightness,finalBrightness,1.0);
		else outw=(shadowAttenuation-0.3f)*1.14;
		
		float fog = clamp(exp(-fogdensity * trueDepth * trueDepth), 0.2, 1);
  		outcolor = mix(fogcolor*((daylightAmount-0.15)*1.17647), outcolor, fog);
  	}
	
	if(water)
	{
		vec4 crefracted=vec4((outcolor.xyz*exp(-vec3(0.46,0.09,0.06)*(waterd/*+(1-Brightness.x)*16*/))).xyz,1);
		outcolor=mix(vec4(0.05,0.05,0.1,1),crefracted,exp(-0.01*waterd));
	}
	
	outcolor.w=outw;
}