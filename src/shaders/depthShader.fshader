#version 330 core

uniform sampler2DArray tiles;
layout(location = 0) out float depth;

in vec2 Properties;
in vec3 Location;

void main()
{
	vec4 outColor;
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
}