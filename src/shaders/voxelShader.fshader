#version 330 core
uniform sampler2D tiles;
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
	if(Properties.y<1.5f)
	{
		normal=Properties.y<0.5f? vec3(-1,0,0) : vec3(1,0,0);
		outColor=texture2D(tiles,
			vec2(
				(adjFract(Location.z)+Properties.x)/16,
				((1-adjFract(Location.y))+floor(((Properties.x)+0.001)/16))/16
				));
		/*outColor=texture2D(tiles,
			vec2(
				(adjFract(Location.x)+Properties.x)/16,
				(adjFract(Location.z)+floor(((Properties.x)+0.001)/16))/16
				));//**vec4(1.0,1.0,1.0,alpha);vec4(adjFract(Location.x/10),adjFract(Location.y/10),adjFract(Location.z/10),1.0);
		//outColor=vec4(0.5f,1.0f*(Location.y/32),0.5f,alpha);*/
	}
	else if(Properties.y<3.5f)
	{
		normal=Properties.y<2.5f? vec3(0,-1,0) : vec3(0,1,0);
		outColor=texture2D(tiles,
			vec2(
				(adjFract(Location.x)+Properties.x)/16,
				(adjFract(Location.z)+floor(((Properties.x)+0.001)/16))/16
				));
	}
	else{
		normal=Properties.y<4.5f? vec3(0,0,-1) : vec3(0,0,1);
		outColor=texture2D(tiles,
			vec2(
				(adjFract(Location.x)+Properties.x)/16,
				((1-adjFract(Location.y))+floor(((Properties.x)+0.001)/16))/16
				));
	}
	if(outColor.w<0.1) discard;
	
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

	if(dotsun>0 && dot(sunNormal,vec3(0,1,0))>0){
		//float bias = 0.000008 * splitDistances[sindex];
		float bias = 0.0002;
		vec4 sunLocation=shadowMatrixes[sindex]*vec4(ModelLocation,1);
		//sunLocation=sunLocation/sunLocation.w;
		
		vec4 shadowLoc=vec4(sunLocation.x,sunLocation.y,float(floor(sindex+0.5f)),sunLocation.z-bias);
		float shadow=texture(shadowMap,shadowLoc);
		//outColor=((shadow - vec4(0.8,0.8,0.8,0.8)) * 5) * ((shadow - vec4(0.8,0.8,0.8,0.8)) * 5);
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
	
	float daylightBrightness=Brightness.x*daylightAmount*shadowAttenuation;
	float finalBrightness=Brightness.y>daylightBrightness?Brightness.y:daylightBrightness;
	outColor=outColor*vec4(finalBrightness,finalBrightness,finalBrightness,1.0);
	
	/*if(sindex==0) outColor+=vec4(0.1f,0,0,1);
	if(sindex==1) outColor+=vec4(0,0.1f,0,1);
	if(sindex==2) outColor+=vec4(0,0,0.1f,1);*/
	
 	float fog = clamp(exp(-fogdensity * z * z), 0.2, 1);
  	outcolor = mix(fogcolor*((daylightAmount-0.15)*1.17647), outColor, fog);
  	//outPosition=(shadow.xyz - 0.5)*2;
  	//outNormalsAndLight=vec3(Properties.y,Brightness.x,Brightness.y);
  	/*if(sunLocation.x>0.2f||sunLocation.x<-0.2f||sunLocation.y>0.2f||sunLocation.y<-0.2f||sunLocation.z>0.2f||sunLocation.z<-0.2f) outNormalsAndLight=vec3(0,0,0);
  	else outNormalsAndLight=(vec3((sunLocation.x+1)/2,(sunLocation.y+1)/2,(sunLocation.z+1)/2));*/
  	//gl_FragColor = gl_FragColor - vec4(0.35,0.35,0.35,0.35);
  	
  	
  	
  	//gl_FragColor =outColor;
}