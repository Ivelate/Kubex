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