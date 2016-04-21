//This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/. 
//
//Author: Víctor Arellano Vicente (Ivelate)
//
//Global deferred vertex shader. Transforms the position to texture coordinates

#version 330 core

in vec2 location;
in vec3 farFaceLocation;
in vec3 farFaceCamViewLocation;

out vec2 pos;
out vec3 FarFaceLocation;
out vec3 FarFaceCamViewLocation;

void main(){
	FarFaceLocation=farFaceLocation;
	FarFaceCamViewLocation=farFaceCamViewLocation;
	pos=vec2((location.x+1)/2,(location.y+1)/2);
	gl_Position=vec4(location.xy,0.0,1.0);
}