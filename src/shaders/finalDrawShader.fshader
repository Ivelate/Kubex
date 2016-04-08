#version 330 core

uniform sampler2D nightTexture;
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

vec4 getSkyColorApproximation(vec3 Location)
{
	vec4 nightColor;
	
	float zenith = acos(Location.y / sqrt(Location.x*Location.x + Location.y*Location.y + Location.z*Location.z));
   	float len=zenith/3.1415926;
	float azimuth = atan(Location.x, Location.z);
	float xt=cos(azimuth)*len;
   	float yt=sin(azimuth)*len;
   	
   	if(sqrt(xt*xt + yt*yt)>0.5f) {
   		float normLight=((daylightAmount-0.35)*1.5384);
   		return vec4(0.2*normLight,0.4*normLight,0.75*normLight,1);
   	}
   	 
   	 //return vec4(1,0,0,1);	
   	float attenuation=clamp((daylightAmount-0.5)*2,0,1);
   	return mix(texture2D(nightTexture,vec2(xt+0.5f,yt+0.5f)),vec4(0.12,0.2,0.39,1),attenuation);
}
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
	
	bool underwater=false;
	
	for(int i=0;i<liquidLayersTexLength;i++)
	{ 
		float dw=texture(liquidLayersTex,vec3(pos.x,pos.y,floor(i+0.5))).x;
		if(dw==1) break;
		
		if(begind<0) {
			begind=-mnearfar / ((dw * snearfar) - cfar);
			underwater=true;
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
			underwater=false;
		}
	}
	if(begind>=0){
		float finald=trueDepth;
		waterd+=(finald-begind);
		begind=-1;
	}
		
	bool water=waterd>0;
	
	//START ILLUMINATION
	
	outcolor=texture2D(colorTex,vec2(pos.x,pos.y));
	
	float shadowAttenuation=1;
	if(z<1&&!underwater)
	{
		//SHADOWS
		int sindex=0;
		if(trueDepth>splitDistances.x)
		{
			if(trueDepth<splitDistances.y) sindex=1;
			else if(trueDepth<splitDistances.z) sindex=2;
			else sindex=3;
		}
	
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
  	}
  	float waterShadowAttenuation=1;
  	if(water)
  	{
  		trueDepth=firstWaterDepth;
		normal=firstWaterNormal;
		worldPosition=FarFaceLocation*trueDepth/cfar;
		int sindex=0;
		if(trueDepth>splitDistances.x)
		{
			if(trueDepth<splitDistances.y) sindex=1;
			else if(trueDepth<splitDistances.z) sindex=2;
			else sindex=3;
		}
	
		float dotsun=dot(sunNormal,normal);
		float sunsetdot=dot(sunNormal,vec3(0,1,0));
		if(dotsun>0 && sunsetdot>-0.2)
		{
			float sunsetAttenuation=sunsetdot>0?1.0:(sunsetdot+0.2)*5;
			vec3 smallnormal=normal * length(worldPosition)*0.01;
			vec4 sunLocation=shadowMatrixes[sindex]*vec4(worldPosition+smallnormal,1);
			vec4 shadowLoc=vec4(sunLocation.x,sunLocation.y,float(floor(sindex+0.5f)),sunLocation.z);
			if(texture(shadowMap,shadowLoc)<0.5){
				waterShadowAttenuation=0.3;
			}
		}
		else waterShadowAttenuation=0.3;
  	}
  	
	shadowAttenuation=underwater?waterShadowAttenuation:shadowAttenuation;
	float daylightBrightness=Brightness.x*daylightAmount*shadowAttenuation;
	float finalBrightness=Brightness.y>daylightBrightness?Brightness.y:daylightBrightness;
	
	outcolor=outcolor*vec4(finalBrightness,finalBrightness,finalBrightness,1.0);
	float fog = clamp(exp(-fogdensity * trueDepth * trueDepth), 0.2, 1);
  	outcolor = mix(fogcolor*((daylightAmount-0.15)*1.17647), outcolor, fog);
  		
	if(water)
	{
		if(z==1) outcolor=getSkyColorApproximation(lookVector); 
		vec4 crefracted=vec4((outcolor.xyz*exp(-vec3(0.46,0.09,0.06)*(waterd/*+(1-Brightness.x)*16*/))).xyz,1);
		outcolor=mix(vec4(0.05,0.05,0.1,1),crefracted,exp(-0.01*waterd));
	}
	
	outcolor.w=water? (waterShadowAttenuation>0.5?(0.8*(clamp((daylightAmount-0.55)*4,0,1))):0) : 1;/*water? ((z==1?0:0.4) + (waterShadowAttenuation>0.5?0.2:0)): 1*/;
	//if(outcolor.w>0.89&&outcolor.w<0.91) outcolor=vec4(1,0,0,0.9);
	//outcolor=getSkyColorApproximation(lookVector);
}