#version 330 core

/************************************ SKY PARAMETERS **********************************************/

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
	if(water)
	{
		
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
		vec3 realNormalTexNormal=vec3(texNormal.x*0.2,texNormal.y,texNormal.z*0.2);
		vec3 specularNormalTexNormal=vec3(texNormal.x*(0.3+flowLength/2),texNormal.y,texNormal.z*(0.3+flowLength/2));
		vec3 refractionNormalTexNormal=vec3(texNormal.x*0.04,texNormal.y,texNormal.z*0.04);

		vec3 realNormal=normalize(vec3(dot(realNormalTexNormal,v),dot(realNormalTexNormal,firstWaterNormal),dot(realNormalTexNormal,u)));

		vec3 specularNormal=-normalize(vec3(dot(specularNormalTexNormal,v),dot(specularNormalTexNormal,firstWaterNormal),dot(specularNormalTexNormal,u)));
		vec3 refractionNormal=normalize(vec3(dot(refractionNormalTexNormal,v),dot(refractionNormalTexNormal,firstWaterNormal),dot(refractionNormalTexNormal,u)));
		
		vec2 refractionLoc=vec2(pos.x+refractionNormalTexNormal.x,pos.y+refractionNormalTexNormal.z);
		vec4 refOutColor=texture2D(colorTex,refractionLoc); //<---------------- HERE IS THE SALSA
		outcolor=refOutColor.w<0.9&&refractionLoc.x>=0&&refractionLoc.x<=1&&refractionLoc.y>=0&&refractionLoc.y<=1?refOutColor:outcolor;

		float waterCos=abs(dot(lookVector,specularNormal)); 
		//float fresnel=fresnelR0 + (1-fresnelR0)*pow(1-waterCos,5); //Fresnel calculation
		//MY APPROXIMATION YEAH
		float fresnel=clamp(fresnelR0 + (1-fresnelR0) * pow(1.33-waterCos,10),0,1);
		
		//Caculating reflected color.
		vec3 viewNormal=(viewMatrix*vec4(realNormal.xyz,0)).xyz; //Normal vector, view space
		vec3 viewDir=normalize(FarFaceCamViewLocation);
		vec3 refractedSunlightVector=normalize(refract(sunNormal,specularNormal,1/1.33));
		float especular=clamp(dot(refractedSunlightVector,lookVector),0,1); //Blinn-phong
		especular=0;//pow(especular,5); //<----------------------- NO UNDERWATER ESPECULAR
		vec3 viewPos=FarFaceCamViewLocation*firstWaterDepth/cfar;
											
		vec4 reflected;
		reflected=mix(waterFogColor*(0.3+currentLight*0.7),outcolor,exp(-0.04*(firstWaterDepth*2)));
	
		outcolor=mix(outcolor,reflected,fresnel);
		
		//If water is shadowed, no specular
		if(especular>0.005)
		{
			float shadowed=inw*1.25;
			if(shadowed>0.1) outcolor=clamp(outcolor + vec4(especular,especular,especular,0)*shadowed,vec4(0,0,0,0),vec4(1,1,1,1));
		}
	}
}
float distanceSquared(vec2 P0, vec2 P1)
{
	vec2 diff=P1-P0;
	return dot(diff,diff);
}
bool traceScreenSpaceRay(vec3 csOrig,vec3 csDir,mat4 proj,sampler2D csZBuffer,float width,float height,float cnear,float cfar,float mnearfar,float snearfar,float zThickness,float maxDistance,out vec2 hitPixel)
{

	// Clip to the near plane VERY FUCKING IMPORTANT BECAUSE OUR FRIEND PROJECTION MATRIX FUCKS UP WHEN DIVIDING BY w<0
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
		
			return sceneMaxZ-0.05>-cfar;
			break; // Breaks out of both loops, since the inner loop is a macro
		}
	}	
	
	return false;
}