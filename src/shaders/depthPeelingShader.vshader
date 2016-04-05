#version 330 core

uniform mat4 vpMatrix;
uniform mat4 modelMatrix;

in vec3 location;
in vec3 normal;

out vec3 Normal;

//out vec3 loc;
void main()
{
	Normal=normal;
	vec4 modelLocation=modelMatrix * vec4(location.xyz,1.0);
	vec4 finalLoc=vpMatrix * modelLocation;
	//loc=vec3((finalLoc.x+1)/2/finalLoc.w,(finalLoc.y+1)/2/finalLoc.w,(finalLoc.z+1)/2/finalLoc.w);
	//loc=((finalLoc.xyz / finalLoc.w) + vec3(1,1,1))/2;
	gl_Position=finalLoc;
}