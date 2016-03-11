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

const float fogdensity = .00001;
const vec4 fogcolor = vec4(0.7, 0.9, 1.0, 1.0);
uniform float daylightAmount;

uniform float fresnelR0=((1-1.33f)/(2.33f))*((1-1.33f)/(2.33f)); //Air-water

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

bool traceScreenSpaceRay(vec3 csOrig,vec3 csDir,mat4 proj,sampler2D csZBuffer,float width,float height,float cnear,float cfar,float mnearfar,float snearfar,float zThickness,float maxDistance,out vec2 hitPixel);

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
	
	//START ILLUMINATION
	
	outcolor=texture2D(colorTex,vec2(pos.x,pos.y));
	
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
		float dotsun=dot(sunNormal,normal);
		float sunsetdot=dot(sunNormal,vec3(0,1,0));
		if(dotsun>0 && sunsetdot>-0.2)
		{
			float sunsetAttenuation=sunsetdot>0?1:(sunsetdot+0.2)*5;
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
		
		float fog = clamp(exp(-fogdensity * trueDepth * trueDepth), 0.2, 1);
  		outcolor = mix(fogcolor*((daylightAmount-0.15)*1.17647), outcolor, fog);
  	}
	
	if(water)
	{
		float waterCos=-dot(lookVector,firstWaterNormal);
		float fresnel=fresnelR0 + (1-fresnelR0)*pow(1-waterCos,5);
		//fresnel=mix(fresnel,0,clamp(sqrt(abs((pos.x-0.5)*2)),0,0.5));
		//Caculating reflected color.
		vec3 viewNormal=(viewMatrix*vec4(firstWaterNormal.xyz,0)).xyz;
		vec3 viewDir=normalize(FarFaceCamViewLocation);
		vec3 viewPos=FarFaceCamViewLocation*firstWaterDepth/cfar;
		vec3 viewPosP=(viewMatrix*vec4(FarFaceLocation*firstWaterDepth/cfar,1)).xyz;
		vec2 hitPixel=vec2(0,0);
		bool collision=traceScreenSpaceRay(viewPos,reflect(viewDir,viewNormal).xyz,projectionMatrix,baseFboDepthTex,
											cwidth,cheight,cnear,cfar,mnearfar,snearfar,1,2000,hitPixel);
		vec4 reflected=vec4(0.2941f,0.4156f,0.6666f,1);
		vec4 reflectedt=reflected;

		if(collision) reflectedt=texture(colorTex,vec2(hitPixel.x/cwidth,hitPixel.y/cheight));
	
		reflected=reflectedt;
		//reflected=mix(reflectedt,reflected,clamp((abs((pos.x-0.5)*2)-0.5)*2,0,1));
		
		vec4 crefracted=vec4((outcolor.xyz*exp(-vec3(0.46,0.09,0.06)*(waterd/*+(1-Brightness.x)*16*/))).xyz,1);
	
		outcolor=mix(vec4(0.05,0.05,0.1,1),crefracted,exp(-0.01*waterd));
	
		outcolor=mix(outcolor,reflected,fresnel);
	}
	
	//outcolor=vec4(outcolor.x * FarFaceCamViewLocation.x/FarFaceCamViewLocation.z,outcolor.y * FarFaceCamViewLocation.y/FarFaceCamViewLocation.z,0,1);
}
float distanceSquared(vec2 P0, vec2 P1)
{
	vec2 diff=P1-P0;
	return dot(diff,diff);
}
bool traceScreenSpaceRay(vec3 csOrig,vec3 csDir,mat4 proj,sampler2D csZBuffer,float width,float height,float cnear,float cfar,float mnearfar,float snearfar,float zThickness,float maxDistance,out vec2 hitPixel)
{

	// Clip to the near plane VERY FUCKING IMPORTANT BECAUSE OR FRIEND PROJECTION MATRIX FUCKS UP WHEN DIVIDING BY w<0
	float rayLength = ((csOrig.z + csDir.z * maxDistance) > -cnear) ?
		(-cnear - csOrig.z) / csDir.z : maxDistance;

	vec3 csEndPoint = csOrig + csDir * rayLength;
	hitPixel = vec2(-1, -1);

	// Project into screen space
	vec4 H0 = proj * vec4(csOrig, 1.0), H1 = proj * vec4(csEndPoint, 1.0);
	float k0 = 1.0 / H0.w, k1 = 1.0 / H1.w;
	vec3 Q0 = csOrig * k0, Q1 = csEndPoint * k1;

	// Screen-space endpoints
	vec2 P0 = H0.xy * k0, P1 = H1.xy * k1;
	//Porting to REAL screen space
	P0.x=((P0.x+1)/2)*width; P0.y=((P0.y+1)/2)*height;
	P1.x=((P1.x+1)/2)*width; P1.y=((P1.y+1)/2)*height;
	//Q0.z=(Q0.z+1)/2; Q1.z=(Q1.z+1)/2; 
	
	P1 += vec2((distanceSquared(P0, P1) < 0.0001) ? 0.01 : 0.0);
	vec2 delta = P1 - P0;
	
	bool permute = false;
	if (abs(delta.x) < abs(delta.y)) {
		permute = true;
		delta = delta.yx; P0 = P0.yx; P1 = P1.yx;
	}

	float stepDir = sign(delta.x), invdx = stepDir / delta.x;

	// Track the derivatives of Q and k.
	vec3 dQ = (Q1 - Q0) * invdx;
	//return dQ;
	float dk = (k1 - k0) * invdx;
	vec2 dP = vec2(stepDir, delta.y * invdx);

	/*dP *= stride; dQ *= stride; dk *= stride;
	P0 += dP * jitter; Q0 += dQ * jitter; k0 += dk * jitter;*/
	float prevZMaxEstimate = csOrig.z;
	
	// Slide P from P0 to P1, (now-homogeneous) Q from Q0 to Q1, k from k0 to k1
	vec3 Q = Q0; float k = k0, stepCount = 0.0, end = P1.x * stepDir;
	for (vec2 P = P0;
		((P.x * stepDir) <= end);
		P += dP, Q.z += dQ.z, k += dk) {

		// Project back from homogeneous to camera space
		hitPixel = permute ? P.yx : P;

		// The depth range that the ray covers within this loop iteration.
		// Assume that the ray is moving in increasing z and swap if backwards.
		float rayZMin = prevZMaxEstimate;
		// Compute the value at 1/2 pixel into the future
		float rayZMax = (dQ.z * 0.5 + Q.z) / (dk * 0.5 + k);
		prevZMaxEstimate = rayZMax;
		if (rayZMin > rayZMax) { 
			float aux=rayZMin;
			rayZMin=rayZMax;
			rayZMax=aux;
		}

		if(hitPixel.x>=width||hitPixel.x<0||hitPixel.y>=height||hitPixel.y<0) break;
		float sceneMaxZ = mnearfar / (((texture(csZBuffer, vec2(hitPixel.x/width,hitPixel.y/height)).x) * snearfar) - cfar);
		float sceneMinZ=sceneMaxZ - zThickness;
		
		if (((rayZMax >= sceneMinZ) && (rayZMin <= sceneMaxZ))) {
		
			return true;
			break; // Breaks out of both loops, since the inner loop is a macro
		}
	}	
	
	return false;
}