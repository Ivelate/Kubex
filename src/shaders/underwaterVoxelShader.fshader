#version 330 core
uniform sampler2DArray tiles;
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
	if(Properties.y<1.5f)
	{
		outColor=texture(tiles,
			vec3(
				Location.z,
				1-Location.y,
				floor(Properties.x+0.5)
				));
	}
	else if(Properties.y<3.5f)
	{
		outColor=texture(tiles,
			vec3(
				Location.x,
				Location.z,
				floor(Properties.x+0.5)
				));
	}
	else{
		outColor=texture(tiles,
			vec3(
				Location.x,
				1-Location.y,
				floor(Properties.x+0.5)
				));
	}
	if(outColor.w<0.5) discard;

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