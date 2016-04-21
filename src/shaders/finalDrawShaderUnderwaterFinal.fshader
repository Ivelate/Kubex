//This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/. 
//
//Author: Víctor Arellano Vicente (Ivelate)
//
//Third deferred pass shader underwater. Calculates refraction and total reflection of the water surface

#version 330 core

uniform sampler2D colorTex;
uniform sampler2D brightnessNormalTex;
uniform sampler2D baseFboDepthTex;
uniform sampler2D miscTex;
uniform sampler2D miscTex2;
uniform sampler2DArray liquidLayersTex;
uniform int liquidLayersTexLength;
uniform float cnear;
uniform float cfar;
uniform float cwidth;
uniform float cheight;
uniform vec3 WorldPosition;

uniform float time;

const vec4 waterFogColor= vec4(0.06,0.15,0.2,1);
uniform float currentLight;
uniform float daylightAmount;

uniform float fresnelR0=((1-1.33f)/(2.33f))*((1-1.33f)/(2.33f)); //Air-water

uniform vec3 sunNormal;

uniform mat4 viewMatrix;
uniform mat4 projectionMatrix;
in vec2 pos;
in vec3 FarFaceLocation;
in vec3 FarFaceCamViewLocation;

layout(location = 0) out vec4 outcolor;

/******************************* MAIN ************************************************************/

void main()
{
	float mnearfar=cnear*cfar;
	float snearfar=cfar-cnear;
	
	float z=texture(baseFboDepthTex,vec2(pos.x,pos.y)).x;
	vec3 lookVector=normalize(FarFaceLocation);
	
	float trueDepth=-mnearfar / ((z * snearfar) - cfar);
	
	vec3 normal=vec3(texture(brightnessNormalTex,vec2(pos.x,pos.y)).xy * 2 -vec2(1,1),0);
	float zsq=1-(normal.x*normal.x + normal.y*normal.y);
	if(zsq>0){
		normal.z=sqrt(zsq);
		if(dot(normal,lookVector)>0) normal.z=-normal.z;	
	}
	
	vec2 Brightness=texture(brightnessNormalTex,vec2(pos.x,pos.y)).zw;
	
	vec3 worldPosition=FarFaceLocation*trueDepth/cfar;
	
	//RAYTRACE WATER DISTANCE
	bool water=false;
	
	float firstWaterDepth;
	vec3 firstWaterNormal;
	
	if(liquidLayersTexLength>0){
		float dw=texture(liquidLayersTex,vec3(pos.x,pos.y,floor(0.5))).x;
		if(dw<1){
			firstWaterDepth=-mnearfar / ((dw * snearfar) - cfar);
			firstWaterNormal=-normalize((texture(miscTex2,vec2(pos.x,pos.y)).xyz - vec3(0.5f,0.5f,0.5f))*2);
			water=true;
		}
	}
	
	//START ILLUMINATION
	
	outcolor=texture2D(colorTex,vec2(pos.x,pos.y));
	
	float inw=outcolor.w;
	//If water surface can be seen
	if(water)
	{
		//Equal to the finalDrawShaderReflections part, excepting for some constants valor changes
		vec3 waterWorldPos=FarFaceLocation*firstWaterDepth/cfar;
		//Transforming to water space folks
		//Getting perpendicular vectors
		vec3 v=normalize(abs(firstWaterNormal.z) >0.99? cross(firstWaterNormal,vec3(0,1,0)) : cross(firstWaterNormal,vec3(0,0,1)));
		vec3 u=normalize(cross(v,firstWaterNormal));
		
		vec3 flowVector=/*firstWaterNormal.y>0.99?vec3(1,0,0) :*/ cross(firstWaterNormal,cross(firstWaterNormal,vec3(0,-1,0)));
		float flowLength=sqrt(length(flowVector));
		if(flowLength<0.01) flowVector=v;
		else flowVector=normalize(flowVector);
		float flowx=dot(flowVector,v)*(flowLength+0.2);
		float flowy=dot(flowVector,u)*(flowLength+0.2);
		
		float texx=dot(v,(waterWorldPos.xyz + WorldPosition.xyz));
		float texy=dot(u,(waterWorldPos.xyz + WorldPosition.xyz));
		
		vec3 texNormal=texture(miscTex,vec2((texx + time*flowx )/5,(texy + time*flowy)/5)).xyz;			//texture(miscTex,vec2((waterWorldPos.x+WorldPosition.x+time/3)/5,(waterWorldPos.z+WorldPosition.z)/5)).xyz;
		texNormal=(texNormal - vec3(0.5,0.5,0)).xzy;

		vec3 specularNormalTexNormal=vec3(texNormal.x*(0.3+flowLength/2),texNormal.y,texNormal.z*(0.3+flowLength/2));
		vec3 refractionNormalTexNormal=vec3(texNormal.x*0.04,texNormal.y,texNormal.z*0.04);

		vec3 specularNormal=-normalize(vec3(dot(specularNormalTexNormal,v),dot(specularNormalTexNormal,firstWaterNormal),dot(specularNormalTexNormal,u)));
		vec3 refractionNormal=normalize(vec3(dot(refractionNormalTexNormal,v),dot(refractionNormalTexNormal,firstWaterNormal),dot(refractionNormalTexNormal,u)));
		
		vec2 refractionLoc=vec2(pos.x+refractionNormalTexNormal.x,pos.y+refractionNormalTexNormal.z);
		vec4 refOutColor=texture2D(colorTex,refractionLoc);
		outcolor=refOutColor.w<0.9&&refractionLoc.x>=0&&refractionLoc.x<=1&&refractionLoc.y>=0&&refractionLoc.y<=1?refOutColor:outcolor;

		float waterCos=abs(dot(lookVector,specularNormal)); 

		//The only different part. The fresnel has my formula.There is not reflections, but simply more scattering
		float fresnel=clamp(fresnelR0 + (1-fresnelR0) * pow(1.33-waterCos,10),0,1);
											
		vec4 reflected;
		reflected=mix(waterFogColor*(0.3+currentLight*0.7),outcolor,exp(-0.04*(firstWaterDepth*2)));
	
		outcolor=mix(outcolor,reflected,fresnel);
	}
}