#version 330 core
uniform sampler2D tiles;
uniform float alpha;
uniform float daylightAmount;
uniform float currentLight;
in vec3 Location;
in vec2 Properties;
in vec2 Brightness;
const vec4 fogcolor = vec4(0.375f, 0.75f, 1.0f, 1.0);
const float fogDensity = .01;


float adjFract(float val)
{
return (fract(val)*0.999f) + 0.0005;
}
void main(){
vec4 outColor;
	float fogdensity=fogDensity+((1-currentLight)*0.1);
	if(Properties.y>0)
	{
		outColor=texture2D(tiles,
			vec2(
				(adjFract(Location.x)+Properties.x)/16,
				(adjFract(Location.z)+floor(((Properties.x)+0.001)/16))/16
				))*vec4(1.0,1.0,1.0,alpha);//*vec4(adjFract(Location.x/10),adjFract(Location.y/10),adjFract(Location.z/10),1.0);
		//outColor=vec4(0.5f,1.0f*(Location.y/32),0.5f,alpha);
	}
	else
	{
		outColor=texture2D(tiles,
			vec2(
				(adjFract(Location.x+Location.z)+Properties.x)/16,
				((1-adjFract(Location.y))+floor((Properties.x+0.001)/16))/16
				))*vec4(1.0,1.0,1.0,alpha);//*vec4(adjFract(Properties.x/10),adjFract(Properties.y/10),adjFract(Properties.z/10),1.0);
		//outColor=vec4(0.4f,0.9f,0.4f,alpha);
	}
	if(outColor.w<0.1) discard;
	float daylightBrightness=Brightness.x*daylightAmount;
	float finalBrightness=Brightness.y>daylightBrightness?Brightness.y:daylightBrightness;
	outColor=outColor*vec4(finalBrightness*0.8f,finalBrightness*0.8f,finalBrightness*1.2f,1.0);
	
	float z = gl_FragCoord.z / gl_FragCoord.w;
 	float fog = clamp(exp(-fogdensity * sqrt(z *z*z)), 0, 1);

  	//gl_FragColor = mix(fogcolor*currentLight, mix(fogcolor*currentLight,outColor,finalBrightness), fog);
  	vec4 final = mix(fogcolor*currentLight,outColor, fog);
  	float surffog=fog*4;
  	if(surffog>1) surffog=1;
  	final.w=mix(1,final.w, surffog);
  	gl_FragColor=final;
}