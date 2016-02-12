#version 330 core

uniform mat4 vpMatrix;
uniform mat4 modelMatrix;
uniform mat4 sunMvpMatrix;

in vec3 location;
in float properties;
in vec2 brightness;

out vec3 Location;
out vec3 ModelLocation;
out vec2 Properties;
out vec2 Brightness;

void main(){
	//Properties=modelMatrix*vec4(properties.xyz,1.0);
	float normal=round(properties/1000);
	Properties=vec2(properties-normal*1000,normal);
	Location=location;
	Brightness=brightness;
	vec4 modelLocation=modelMatrix * vec4(location.xyz,1.0);
	gl_Position=vpMatrix * modelLocation;
	ModelLocation=vec3(modelLocation.xyz);
	//ModelLocation=vec3(modelLocation.x/modelLocation.w,modelLocation.y/modelLocation.w,modelLocation.z/modelLocation.w);
}