#version 150

uniform mat4 invertedViewRotationMatrix;

in vec2 location;

out vec3 Location;

void main(){
	vec4 loc=(invertedViewRotationMatrix*vec4(location.x,location.y,1.0f,1.0f)).xyzw;
	Location=vec3(loc.xyz)/loc.w;
	
	gl_Position=vec4(location.xy,1.0,1.0);
}