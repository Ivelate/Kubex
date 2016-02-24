#version 330 core

uniform sampler2D colorTex;
uniform sampler2D baseFboDepthTex;
uniform sampler2DArray liquidLayersTex;
uniform int liquidLayersTexLength;

uniform vec4 invProjZ;
uniform vec4 sunCamera;
in vec2 pos;

layout(location = 0) out vec4 outcolor;
void main()
{
//outcolor=texture2D(baseFboDepthTex,vec2(pos.x,pos.y)).x<1?vec4(1,0,0,1):vec4(0,0,0,0);
	//if(pos.x<0.5) outcolor=(vec4(texture(liquidLayersTex,vec3(pos.x,pos.y,floor(1.5))).xyz,1)-0.99)*100;
	//else outcolor=(vec4(texture(baseFboDepthTex,vec2(pos.x,pos.y)).xyz,1)-0.99)*100;
	
	float waterd=0;
	float begind=-1;

	for(int i=0;i<liquidLayersTexLength;i++)
	{
		float dw=texture(liquidLayersTex,vec3(pos.x,pos.y,floor(i+0.5))).x;
		if(dw==1) break;
		if(begind<0) begind=1/(invProjZ.x*pos.x + invProjZ.y*pos.y + invProjZ.z*dw + invProjZ.w);
		else
		{
			float finald=1/(invProjZ.x*pos.x + invProjZ.y*pos.y + invProjZ.z*dw + invProjZ.w);
			waterd+=(finald-begind);
			begind=-1;
		}
	}
	if(begind>=0){
		float dw=texture(baseFboDepthTex,vec2(pos.x,pos.y)).x;
		float finald=1/(invProjZ.x*pos.x + invProjZ.y*pos.y + invProjZ.z*dw + invProjZ.w);
		waterd+=(finald-begind);
		begind=-1;
	}
	//bool water=texture(liquidLayersTex,vec3(pos.x,pos.y,floor(0.5))).x < 1;
	vec4 extinction=exp(- vec4(waterd,waterd,waterd,0)/vec4(4.5,75,300,1));
	outcolor=texture2D(colorTex,vec2(pos.x,pos.y))*extinction;
	//outcolor.x=max(0,outcolor.x);outcolor.y=max(0,outcolor.y);outcolor.z=max(0,outcolor.z);
	
	//if(water) outcolor=outcolor-vec4(0.5,0.5,-0.5,0);
}