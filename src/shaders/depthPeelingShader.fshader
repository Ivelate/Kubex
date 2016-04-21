//This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/. 
//
//Author: Víctor Arellano Vicente (Ivelate)
//
//Depth peeling shader

#version 330 core
uniform sampler2D lowerLimitDepth;
uniform sampler2DArray upperLimitDepth;
uniform int upperLimitIndex;

uniform int xres;
uniform int yres;

in vec3 Normal;

layout(location = 0) out vec3 normal;

void main()
{
	vec3 loc=vec3(gl_FragCoord.x / xres , gl_FragCoord.y / yres , gl_FragCoord.z); //Gets the vector of position in the screen, normalized
	
	//We perform an additional depth sorting based on the last layer texture used
	if(	loc.z>=texture(lowerLimitDepth,vec2(loc.x,loc.y)).x || 
		(upperLimitIndex!=-1 && loc.z<=texture(upperLimitDepth,vec3(loc.xy,floor(upperLimitIndex+0.5))).x) ) discard;
		
	//If this is the first water layer, we draw the normal too	
	if(upperLimitIndex==-1) normal=Normal;
}