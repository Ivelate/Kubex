#version 330 core

uniform sampler2D tiles;
uniform float alpha;
uniform float daylightAmount;

in vec3 Location;
in vec3 ModelLocation;
in vec2 Properties;
in vec2 Brightness;

layout(location = 0) out float depth;

void main()
{
	if(Brightness.x<-1)depth=Location.z+Properties.x+Brightness.x;
}