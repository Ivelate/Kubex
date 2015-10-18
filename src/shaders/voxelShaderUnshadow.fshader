#version 330 core
uniform sampler2D tiles;
uniform float alpha;
uniform float daylightAmount;

in vec3 Location;
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
	outColor=outColor*vec4(1,1,1,alpha);
	
	float daylightBrightness=Brightness.x*daylightAmount;
	float finalBrightness=Brightness.y>daylightBrightness?Brightness.y:daylightBrightness;
	outColor=outColor*vec4(finalBrightness,finalBrightness,finalBrightness,1.0);
	
	float z = gl_FragCoord.z / gl_FragCoord.w;
 	float fog = clamp(exp(-fogdensity * z * z), 0.2, 1);
  	outcolor = mix(fogcolor*((daylightAmount-0.15)*1.17647), outColor, fog);
}