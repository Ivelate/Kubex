#version 330 core

uniform sampler2D colorTex;
uniform sampler2D positionTex;
uniform sampler2D normalAndLightTex;
uniform sampler2D shadowMap;

/*uniform vec3 cameraPos;
uniform mat4 projViewMatrixRelLight;

uniform vec3 lights[50];
uniform int numLights;*/

in vec2 pos;

vec2 poissonDisk[4] = vec2[](
  vec2( -0.94201624, -0.39906216 ),
  vec2( 0.94558609, -0.76890725 ),
  vec2( -0.094184101, -0.92938870 ),
  vec2( 0.34495938, 0.29387760 )
);

layout(location = 0) out vec4 outcolor;
void main()
{
	/*vec3 normal;
	vec3 normalAndLight=texture2D(normalAndLightTex,vec2((pos.x+1)/2,(pos.y+1)/2)).xyz;
	vec3 shadow=texture2D(shadowMap,vec2((pos.x+1)/2,(pos.y+1)/2)).xyz;
	vec3 color=texture2D(colorTex,vec2((pos.x+1)/2,(pos.y+1)/2)).xyz;
	if(normalAndLight.x < 0.5f) normal=vec3(1,0,0);
	else if(normalAndLight.x < 1.5f) normal=vec3(-1,0,0);
	else if(normalAndLight.x < 2.5f) normal=vec3(0,1,0);
	else if(normalAndLight.x < 3.5f) normal=vec3(0,-1,0);
	else if(normalAndLight.x < 4.5f) normal=vec3(0,0,1);
	else normal=vec3(0,0,-1);*/
	
	/*if(pos.x<0){
	if(pos.y>0) outcolor=texture2D(colorTex,vec2(pos.x+1,pos.y));
	else outcolor=texture2D(positionTex,vec2(pos.x+1,pos.y+1));
	}
	else {
	if(pos.y>0) outcolor=texture2D(shadowMap,vec2(pos.x,pos.y));
	else outcolor=texture2D(normalAndLightTex,vec2(pos.x,pos.y+1));
	}*/
	outcolor=texture2D(colorTex,vec2((pos.x+1)/2,(pos.y+1)/2));

	//outcolor=vec4(texture2D(shadowMap,vec2((pos.x+1)/2,(pos.y+1)/2)).xyz,1);
	//outcolor=vec4(shadow.xyz,1);
	/*if(texture2D(normal,vec2(pos.x,pos.y)).xyz == vec3(0,0,0)) discard;
	
	vec3 Position=texture2D(position,vec2(pos.x,pos.y)).xyz;
	vec3 Color=texture2D(color,vec2(pos.x,pos.y)).xyz;
	vec3 Normal=texture2D(normal,vec2(pos.x,pos.y)).xyz;
	vec3 LocationRelLight=(projViewMatrixRelLight *  vec4(Position.xyz,1.0)).xyz;
	
	float bias = 0.05;
	float vis=0;
	for (int i=0;i<4;i++){
		vec2 shadowLoc=vec2((LocationRelLight.x+1)/2,(LocationRelLight.y+1)/2);
		float shadow=texture(shadowMap,vec3(shadowLoc+poissonDisk[i]/1000.0,(LocationRelLight.z+1)/2 -bias));
  		vis+=shadow/4;
	}
	
	outcolor = vec4(Color.xyz * (vis+0.2)/1.2,1.0);
	
	vec4 imageP = texture2D( color, pos );
    vec4 positionP = texture2D( position, pos );
    vec4 normalP = texture2D( normal, pos);
    
    vec3 outcol=vec3(0,0,0);
    
    for(int i=0;i<numLights;i++)
    {
    vec3 light = lights[i];
    vec3 lightDir = light - positionP.xyz ;
    
    vec3 normalVec = normalize(normalP.xyz);
    lightDir = normalize(lightDir);
    
    vec3 eyeDir = normalize(cameraPos-positionP.xyz);
    vec3 vHalfVector = normalize(lightDir.xyz+eyeDir);
    
     outcol = outcol + (max(dot(normalVec,lightDir),0) * imageP + 
                   pow(max(dot(normalVec,vHalfVector),0.0), 100) * 1.5).xyz;
     }
      outcolor=vec4(max(outcol.xyz,outcolor.xyz),1.0);*/
}