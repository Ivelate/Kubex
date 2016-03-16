#version 330 core

/************************************ SKY PARAMETERS **********************************************/
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

uniform sampler2D texture;
uniform sampler2D colorTex;
uniform sampler2D brightnessNormalTex;
uniform sampler2D baseFboDepthTex;
uniform sampler2D miscTex;
uniform sampler2DArray liquidLayersTex;
uniform int liquidLayersTexLength;
uniform float cnear;
uniform float cfar;
uniform float cwidth;
uniform float cheight;
uniform vec3 WorldPosition;

uniform float time;

uniform float daylightAmount;

uniform float fresnelR0=((1-1.33f)/(2.33f))*((1-1.33f)/(2.33f)); //Air-water

uniform vec3 sunNormal;

uniform mat4 viewMatrix;
uniform mat4 projectionMatrix;
in vec2 pos;
in vec3 FarFaceLocation;
in vec3 FarFaceCamViewLocation;

layout(location = 0) out vec4 outcolor;

bool traceScreenSpaceRay(vec3 csOrig,vec3 csDir,mat4 proj,sampler2D csZBuffer,float width,float height,float cnear,float cfar,float mnearfar,float snearfar,float zThickness,float maxDistance,out vec2 hitPixel);

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
	else nightcolor = texture2D(texture,vec2(xt+0.5f,yt+0.5f));
	
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





/******************************* MAIN ************************************************************/

void main()
{
	float mnearfar=cnear*cfar;
	float snearfar=cfar-cnear;
	float z=texture(baseFboDepthTex,vec2(pos.x,pos.y)).x;
	vec3 lookVector=normalize(FarFaceLocation);
	
	if(z==1) outcolor=getSkyColor(lookVector);
	else{
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
			firstWaterNormal=normalize(vec3(0,1,0));
			water=true;
		}
	}
	
	//START ILLUMINATION
	
	outcolor=texture2D(colorTex,vec2(pos.x,pos.y));
	float inw=outcolor.w;
	if(water)
	{
		//float normaldistx=(sin(time + worldPosition.x/7 + worldPosition.z/13)+1)/200;
		//float normaldistz=(cos(time/1.5  + worldPosition.x/7 + worldPosition.z/13)+1)/200;
		//vec3 realNormal=normalize(vec3(firstWaterNormal.x+normaldistx,firstWaterNormal.y,firstWaterNormal.z+normaldistz)); //Normal perturbation (Waves)
		vec3 waterWorldPos=FarFaceLocation*firstWaterDepth/cfar;
		vec3 texNormal=texture(miscTex,vec2((waterWorldPos.x+WorldPosition.x+time/3)/5,(waterWorldPos.z+WorldPosition.z)/5)).xyz;
		vec3 realNormal=normalize(vec3((texNormal.r-0.5)*0.07,texNormal.b,(texNormal.g-0.5)*0.07));
		vec3 specularNormal=normalize(vec3((texNormal.r-0.5)*0.3,texNormal.b,(texNormal.g-0.5)*0.3));
		vec3 refractionNormal=normalize(vec3((texNormal.r-0.5)*0.02,texNormal.b,(texNormal.g-0.5)*0.02));
		vec4 refOutColor=texture2D(colorTex,vec2(pos.x+refractionNormal.x,pos.y+refractionNormal.z));
		outcolor=refOutColor.w<0.9?refOutColor:outcolor;

		float waterCos=-dot(lookVector,specularNormal); 
		float fresnel=fresnelR0 + (1-fresnelR0)*pow(1-waterCos,5); //Fresnel calculation
		
		//Caculating reflected color.
		vec3 viewNormal=(viewMatrix*vec4(realNormal.xyz,0)).xyz; //Normal vector, view space
		vec3 viewDir=normalize(FarFaceCamViewLocation);
		vec3 halfvec=normalize(normalize(-FarFaceLocation)+sunNormal);
		float especular=clamp(dot(halfvec,specularNormal),0,1); //Blinn-phong
		especular=pow(especular,60);
		vec3 viewPos=FarFaceCamViewLocation*firstWaterDepth/cfar;
		//vec3 viewPosP=(viewMatrix*vec4(FarFaceLocation*firstWaterDepth/cfar,1)).xyz;
		vec2 hitPixel=vec2(0,0);
		bool collision=traceScreenSpaceRay(viewPos,reflect(viewDir,viewNormal).xyz,projectionMatrix,baseFboDepthTex,
											cwidth,cheight,cnear,cfar,mnearfar,snearfar,1,2000,hitPixel);
											
		vec4 reflected;
		if(collision) reflected=texture(colorTex,vec2(hitPixel.x/cwidth,hitPixel.y/cheight));
		else reflected=getSkyColor(reflect(lookVector,realNormal).xyz);
	
		outcolor=mix(outcolor,reflected,fresnel);
		
		//If water is shadowed, no specular
		if(especular>0.005)
		{
			float shadowed=inw*1.25;
			if(shadowed>0.1) outcolor=clamp(outcolor + /*vec4(sindex==0?1:0,sindex==1?1:0,sindex==2?1:0,0)*/vec4(especular,especular,especular,0)*shadowed,vec4(0,0,0,0),vec4(1,1,1,1));
		}
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