//This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/. 
//
//Author: Víctor Arellano Vicente (Ivelate)
//
//Second deferred pass shader underwater. Calculates the sky and the shadows outside the water, along with the lighting on each point

#version 330 core

struct YyxColor
{
	float y;
	float x;
	float Y;
};
struct PerezCoefficients
{
	float A;
	float B;
	float C;
	float D;
	float E;
};
struct PerezYxyCoefficients
{
	PerezCoefficients Y;
	PerezCoefficients x;
	PerezCoefficients y;
};

uniform YyxColor zenitalAbs;
uniform PerezYxyCoefficients coeff;
uniform float solar_zenith;
uniform float solar_azimuth;

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

uniform float currentLight;
const vec4 waterFogColor= vec4(0.06,0.15,0.2,1);
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

/**
  * SKY METHODS
  */
float Perez(float zenith, float gamma, PerezCoefficients coeffs)
{
    return  (1 + coeffs.A*exp(coeffs.B/cos(zenith)))*
            (1 + coeffs.C*exp(coeffs.D*gamma)+coeffs.E*pow(cos(gamma),2));
}
 
vec4 RGB(float Y, float x, float y)
{
    float X = x/y*Y;
    float Z = (1-x-y)/y*Y;
    vec4 rgb;
    rgb.a = 1;
    rgb.r =  3.2406f * X - 1.5372f * Y - 0.4986f * Z;
    rgb.g = -0.9689f * X + 1.8758f * Y + 0.0415f * Z;
    rgb.b =  0.0557f * X - 0.2040f * Y + 1.0570f * Z;
    return rgb;
}
 
float Gamma(float zenith, float azimuth)
{
    return acos(sin(solar_zenith)*sin(zenith)*cos(azimuth-solar_azimuth)+cos(solar_zenith)*cos(zenith));
}
vec4 getSkyColor(vec3 Location)
{
	vec4 nightcolor;
	float azimuth = atan(Location.x, Location.z);
   	float zenith = acos(Location.y / sqrt(Location.x*Location.x + Location.y*Location.y + Location.z*Location.z));

   	float len=zenith/3.1415926;
   	 	
   	float xt=cos(azimuth)*len;
   	float yt=sin(azimuth)*len;
   	 	
   	if(sqrt(xt*xt + yt*yt)>0.5f) {
   		float normLight=((daylightAmount-0.15)*1.17647);
   		return vec4(0.2*normLight,0.4*normLight,0.75*normLight,1);
   		nightcolor=vec4(0,0,0,1.0f);
   	}
	else nightcolor = texture2D(nightTexture,vec2(xt+0.5f,yt+0.5f));
	
	float attenuation=clamp((solar_zenith-1.1)*1.19f,0,1);
	nightcolor=nightcolor*attenuation;
	
	
	vec4 daycolor;	
	if(solar_zenith>1.94f){
		 daycolor = vec4(0,0,0,1);
	}
	else{
		float gamma = Gamma(zenith, azimuth);   
   		zenith = min(zenith, 3.1415926f/2.0f);
    	float Yp = zenitalAbs.Y * Perez(zenith, gamma, coeff.Y) / Perez(0, solar_zenith, coeff.Y);
    	float xp = zenitalAbs.x * Perez(zenith, gamma, coeff.x) / Perez(0, solar_zenith, coeff.x);
    	float yp = zenitalAbs.y * Perez(zenith, gamma, coeff.y) / Perez(0, solar_zenith, coeff.y);
 
   		daycolor = RGB(Yp, xp, yp);
    }
    
    return daycolor + nightcolor;
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
	bool underwater=true;
	
	for(int i=0;i<liquidLayersTexLength;i++)
	{ 
		float dw=texture(liquidLayersTex,vec3(pos.x,pos.y,floor(i+0.5))).x;
		if(dw==1) break;
		
		if(begind<0) {
			underwater=false;
			begind=-mnearfar / ((dw * snearfar) - cfar);
			if(i==0){
				firstWaterDepth=begind;
				firstWaterNormal=normalize(vec3(0,1,0));
			}
		}
		else
		{
			underwater=true;
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
	
	outcolor=z!=1?texture2D(colorTex,vec2(pos.x,pos.y)): getSkyColor(lookVector);

	if(z<1)
	{
		//SHADOWS
		int sindex=0;
		if(trueDepth>splitDistances.x)
		{
			if(trueDepth<splitDistances.y) sindex=1;
			else if(trueDepth<splitDistances.z) sindex=2;
			else sindex=3;
		}
	
		float shadowAttenuation=1;
		
		//The shadows will only be applied if the point isnt underwater. There are no shadows on underwater objects, to smooth the sea bed
		if(!underwater&&waterd>0.01)
		{
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
	
		//Calculates the brightness for each point
		float daylightBrightness=Brightness.x*daylightAmount*shadowAttenuation;
		float finalBrightness=Brightness.y>daylightBrightness?Brightness.y:daylightBrightness;
	
		outcolor=outcolor*vec4(finalBrightness,finalBrightness,finalBrightness,1.0);
		
		float fog = clamp(exp(-fogdensity * trueDepth * trueDepth), 0.2, 1);
  		outcolor = mix(fogcolor*((daylightAmount-0.15)*1.17647), outcolor, fog);
  	}
	if(z==1&&!water) outcolor=waterFogColor*(0.3+currentLight*0.7);

	//Applying water scattering fog and absorption
	vec4 crefracted=vec4((outcolor.xyz*exp(-vec3(0.3,0.06,0.04)*(trueDepth-waterd))).xyz,1);
	outcolor=mix(waterFogColor*(0.3+currentLight*0.7),crefracted,exp(-0.04*(trueDepth-waterd)));
	
	outcolor.w=outw;
}