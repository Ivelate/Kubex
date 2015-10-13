#version 330 core

uniform mat4 vpMatrix;
uniform mat4 modelMatrix;
uniform mat4 sunMvpMatrix;

in vec3 location;
in float properties;
in vec2 brightness;

out vec3 Location;
out vec2 Properties;
out vec2 Brightness;
out vec3 sunLocation;

void main(){
	//Properties=modelMatrix*vec4(properties.xyz,1.0);
	float normal=round(properties/1000);
	Properties=vec2(properties-normal*1000,normal);
	Location=location;
	Brightness=brightness;
	vec4 sunLoc=sunMvpMatrix *modelMatrix* vec4(location.xyz,1.0);
	sunLocation=vec3(sunLoc.x/sunLoc.w,sunLoc.y/sunLoc.w,sunLoc.z/sunLoc.w);
	gl_Position=vpMatrix * modelMatrix * vec4(location.xyz,1.0);
}