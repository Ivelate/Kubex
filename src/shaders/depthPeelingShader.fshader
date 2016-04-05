#version 330 core
uniform sampler2D lowerLimitDepth;
uniform sampler2DArray upperLimitDepth;
uniform int upperLimitIndex;

uniform int xres;
uniform int yres;

in vec3 Normal;

//layout(location = 0) out float depth;
layout(location = 0) out vec3 normal;

void main()
{
	vec3 loc=vec3(gl_FragCoord.x / xres , gl_FragCoord.y / yres , gl_FragCoord.z);
	//depth=texture2D(lowerLimitDepth,pos.xy).x;
	if(	loc.z>=texture(lowerLimitDepth,vec2(loc.x,loc.y)).x || 
		(upperLimitIndex!=-1 && loc.z<=texture(upperLimitDepth,vec3(loc.xy,floor(upperLimitIndex+0.5))).x) ) discard;
		
	if(upperLimitIndex==-1) normal=Normal;
}