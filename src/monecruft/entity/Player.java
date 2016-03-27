package monecruft.entity;

import org.lwjgl.input.Mouse;

import monecruft.blocks.Block;
import monecruft.blocks.BlockLibrary;
import monecruft.gui.Chunk;
import monecruft.gui.GlobalTextManager;
import monecruft.gui.WorldFacade;
import monecruft.utils.InputHandler;
import monecruft.utils.KeyToggleListener;
import monecruft.utils.KeyValueListener;
import monecruft.utils.VoxelUtils;
import ivengine.view.Camera;

public class Player implements KeyToggleListener, KeyValueListener
{
	//private static final float DEFAULT_SPEED=8;
	private static final double DEFAULT_SPEED=5;
	private static final double FLY_SPEED=30;
	private static final double DEFAULT_JUMPSPEED=6;
	private static final double FLY_JUMPSPEED=15;
	private static final double DEFAULT_HEIGHT=1.8f;
	private static final double DEFAULT_HEIGHT_APROX=1.79f;
	private static final double EYE_POS=1.65f;
	private static final double DEFAULT_SIZE=0.4f;
	private static final double DEFAULT_SIZE_APROX=0.39f;
	private static final double DEFAULT_MOUSE_SENSIVITY=100;
	private static final double PIMEDIOS=(float)(Math.PI/2);
	private static final double SQRT2=(float)Math.sqrt(2);
	private static final double MAX_RAYCAST_DISTANCE=5;
	private static final int DEFAULT_SELECTED_BLOCK=2;
	
	private Camera cam;
	private double xpos,ypos,zpos;
	private float pitch,yaw;
	private double yvel=0;
	private double xvel=0;
	private double zvel=0;
	private double currentSpeed=DEFAULT_SPEED;
	private boolean flying=false;
	private boolean action_deleteNextBlock=false;
	private boolean action_createNextBlock=false;
	private boolean grounded=false;
	
	private int selectedBlock=DEFAULT_SELECTED_BLOCK;
	
	
	public Player(double ix,double iy,double iz,float pitch,float yaw,Camera cam)
	{
		this.cam=cam;
		this.xpos=ix;this.ypos=iy;this.zpos=iz;
		this.pitch=pitch;this.yaw=yaw;
		InputHandler.addKeyToggleListener(InputHandler.SHIFT_VALUE,this);
		InputHandler.addKeyToggleListener(InputHandler.MOUSE_BUTTON1_VALUE,this);
		InputHandler.addKeyToggleListener(InputHandler.MOUSE_BUTTON2_VALUE,this);
		InputHandler.addKeyValueListener(InputHandler.MOUSE_WHEEL_VALUE, this);
	}
	public void update(float tEl,WorldFacade wf)
	{
		handleEvents(wf);
		//System.out.println(this.xpos+" "+this.ypos+" "+this.zpos);
		double yAxisPressed=InputHandler.isWPressed()||InputHandler.isSPressed()?SQRT2:1;
		double xAxisPressed=InputHandler.isAPressed()||InputHandler.isDPressed()?SQRT2:1;
		boolean underwater=isUnderwater(wf,EYE_POS/2);
		currentSpeed=flying?FLY_SPEED:underwater?DEFAULT_SPEED/2:DEFAULT_SPEED;
		
		boolean climbing=false;
		if(InputHandler.isWPressed()) {
			if(moveForward(currentSpeed*tEl/xAxisPressed,wf)){
				this.moveY(this.ypos+currentSpeed*2/3*tEl, wf);
				climbing=true;
			}
		}
		if(InputHandler.isAPressed()) moveLateral(-currentSpeed*tEl/yAxisPressed,wf);
		if(InputHandler.isSPressed()) moveForward(-currentSpeed*tEl/xAxisPressed,wf);
		if(InputHandler.isDPressed()) moveLateral(currentSpeed*tEl/yAxisPressed,wf);
		this.addPitch(-(float)(Mouse.getDY()/DEFAULT_MOUSE_SENSIVITY));
		this.addYaw((float)(Mouse.getDX()/DEFAULT_MOUSE_SENSIVITY));
		//Gravity
		if(!grounded&&!climbing) {
			if(underwater){
				this.yvel-=wf.getWorldGravity()*tEl/2;
				if(this.yvel<-wf.getWorldGravity()/2) this.yvel=-wf.getWorldGravity()/2;
			}
			else this.yvel-=wf.getWorldGravity()*tEl;
		}
		this.moveY(this.ypos+(yvel*tEl),wf);
		//Jump (Calc in next loop)
		if(InputHandler.isSPACEPressed()) 
		{
			if(this.grounded){
				this.yvel=DEFAULT_JUMPSPEED;
				this.grounded=false;
			}
			else if(underwater){
				this.yvel=isUnderwater(wf)?DEFAULT_JUMPSPEED/2:DEFAULT_JUMPSPEED;
				this.grounded=false;
			}
			if(flying) this.yvel=FLY_JUMPSPEED;
		}
		//yvel*=wf.getWorldAirFriction();
		//if(wf.getContent(this.xpos, this.ypos, this.zpos)!=0) {this.ypos=(int)(ypos)+1;this.yvel=0;}
		//else if(wf.getContent(this.xpos, this.ypos+DEFAULT_HEIGHT, this.zpos)!=0){this.ypos=(int)(ypos+DEFAULT_HEIGHT)-DEFAULT_HEIGHT;this.yvel=0;}
		updateCamera(wf);
		//if(InputHandler.isSHIFTPressed()) this.cam.moveUp(-1f);
		wf.reloadPlayerFOV((int)Math.floor(this.getX()/Chunk.CHUNK_DIMENSION), 0, (int)Math.floor(this.getZ()/Chunk.CHUNK_DIMENSION));
		
		//Raycasting
		//RaycastResult res=raycast(this.pitch,this.yaw,this.xpos,this.ypos,this.zpos,wf,MAX_RAYCAST_DISTANCE);
		
		
	}
	public boolean isUnderwater(WorldFacade wf)
	{
		return isUnderwater(wf,EYE_POS);
	}
	public boolean isUnderwater(WorldFacade wf,double yoffset)
	{
		return BlockLibrary.isLiquid(wf.getContent(this.xpos, this.ypos+yoffset, this.zpos));
	}
	public float getAverageLightExposed(WorldFacade wf)
	{
		float zw=(float)(this.zpos-(Math.floor(this.zpos)));
		if(zw>0.5){
			return (1.5f-zw)*getAverageLightInX(wf,xpos,ypos+EYE_POS,zpos) + (zw-0.5f)*getAverageLightInX(wf,xpos,ypos+EYE_POS,zpos+1);
		}
		else{
			return (zw+0.5f)*getAverageLightInX(wf,xpos,ypos+EYE_POS,zpos) + (0.5f-zw)*getAverageLightInX(wf,xpos,ypos+EYE_POS,zpos-1);
		}
	}
	private float getAverageLightInX(WorldFacade wf,double xpos,double ypos,double zpos)
	{
		float xw=(float)(this.xpos-(Math.floor(this.xpos)));
		if(xw>0.5){
			return (1.5f-xw)*getAverageLightInY(wf,xpos,ypos+EYE_POS,zpos) + (xw-0.5f)*getAverageLightInY(wf,xpos+1,ypos+EYE_POS,zpos);
		}
		else{
			return (xw+0.5f)*getAverageLightInY(wf,xpos,ypos+EYE_POS,zpos) + (0.5f-xw)*getAverageLightInY(wf,xpos-1,ypos+EYE_POS,zpos);
		}
	}
	//|TODO problems
	private float getAverageLightInY(WorldFacade wf,double xpos,double ypos,double zpos)
	{
		float fxpos=(float)xpos;float fypos=(float)ypos;float fzpos=(float)zpos;
		float yw=(float)(ypos-(Math.floor(ypos)));
		if(yw>0.5){
			return (1.5f-yw)*wf.getContentMaxLight(fxpos, fypos, fzpos) + (yw-0.5f)*wf.getContentMaxLight(fxpos, fypos+1, fzpos);
		}
		else{
			return (yw+0.5f)*wf.getContentMaxLight(fxpos, fypos, fzpos) + (0.5f-yw)*wf.getContentMaxLight(fxpos, fypos-1, fzpos);
		}
	}
	private void handleEvents(WorldFacade wf)
	{
		if(action_deleteNextBlock)
		{
			action_deleteNextBlock=false;
			//Chunk c=wf.getChunkByIndex((int)(this.xpos)/Chunk.CHUNK_DIMENSION, (int)(this.ypos-1)/Chunk.CHUNK_DIMENSION, (int)(this.zpos)/Chunk.CHUNK_DIMENSION);
			//if(c!=null) c.setCubeAt(VoxelUtils.trueMod(this.xpos,Chunk.CHUNK_DIMENSION), VoxelUtils.trueMod(this.ypos-1,Chunk.CHUNK_DIMENSION), VoxelUtils.trueMod(this.zpos,Chunk.CHUNK_DIMENSION),(byte)(0));
			RaycastResult res=raycast(this.pitch,this.yaw,this.xpos,this.ypos+EYE_POS,this.zpos,wf,MAX_RAYCAST_DISTANCE);
			if(res!=null)
			{
				Chunk c=wf.getChunkByIndex(res.getChunkX(), res.getChunkY(), res.getChunkZ());
				System.out.println("CHUNK "+c.getX()+" "+c.getY()+" "+c.getZ());
				System.out.println("CUBE "+res.getX()+" "+res.getY()+" "+res.getZ());
				if(c!=null) c.setCubeAt(res.getX(), res.getY(), res.getZ(),(byte)(0));	
			}
		}
		if(action_createNextBlock)
		{
			action_createNextBlock=false;
			//Chunk c=wf.getChunkByIndex((int)(this.xpos)/Chunk.CHUNK_DIMENSION, (int)(this.ypos-1)/Chunk.CHUNK_DIMENSION, (int)(this.zpos)/Chunk.CHUNK_DIMENSION);
			//if(c!=null) c.setCubeAt(VoxelUtils.trueMod(this.xpos,Chunk.CHUNK_DIMENSION), VoxelUtils.trueMod(this.ypos-1,Chunk.CHUNK_DIMENSION), VoxelUtils.trueMod(this.zpos,Chunk.CHUNK_DIMENSION),(byte)(0));
			RaycastResult res=raycast(this.pitch,this.yaw,this.xpos,this.ypos+EYE_POS,this.zpos,wf,MAX_RAYCAST_DISTANCE);
			if(res!=null)
			{
				Chunk c=null;
				int fx=-1;
				int fy=-1;
				int fz=-1;
				switch(res.face)
				{
				case XP:
					if(res.getX()==Chunk.CHUNK_DIMENSION-1){
						c=wf.getChunkByIndex(res.getChunkX()+1, res.getChunkY(), res.getChunkZ());
						fx=0; fy=res.getY(); fz=res.getZ();
					} else {
						c=wf.getChunkByIndex(res.getChunkX(), res.getChunkY(), res.getChunkZ());
						fx=res.getX()+1; fy=res.getY(); fz=res.getZ();
					}
					break;
				case XM:
					if(res.getX()==0){
						c=wf.getChunkByIndex(res.getChunkX()-1, res.getChunkY(), res.getChunkZ());
						fx=Chunk.CHUNK_DIMENSION-1; fy=res.getY(); fz=res.getZ();
					} else {
						c=wf.getChunkByIndex(res.getChunkX(), res.getChunkY(), res.getChunkZ());
						fx=res.getX()-1; fy=res.getY(); fz=res.getZ();
					}
					break;
				case YP:
					if(res.getY()==Chunk.CHUNK_DIMENSION-1){
						c=wf.getChunkByIndex(res.getChunkX(), res.getChunkY()+1, res.getChunkZ());
						fx=res.getX(); fy=0; fz=res.getZ();
					} else {
						c=wf.getChunkByIndex(res.getChunkX(), res.getChunkY(), res.getChunkZ());
						fx=res.getX(); fy=res.getY()+1; fz=res.getZ();
					}
					break;
				case YM:
					if(res.getY()==0){
						c=wf.getChunkByIndex(res.getChunkX(), res.getChunkY()-1, res.getChunkZ());
						fx=res.getX(); fy=Chunk.CHUNK_DIMENSION-1; fz=res.getZ();
					} else {
						c=wf.getChunkByIndex(res.getChunkX(), res.getChunkY(), res.getChunkZ());
						fx=res.getX(); fy=res.getY()-1; fz=res.getZ();
					}
					break;
				case ZP:
					if(res.getZ()==Chunk.CHUNK_DIMENSION-1){
						c=wf.getChunkByIndex(res.getChunkX(), res.getChunkY(), res.getChunkZ()+1);
						fx=res.getX(); fy=res.getY(); fz=0;
					} else {
						c=wf.getChunkByIndex(res.getChunkX(), res.getChunkY(), res.getChunkZ());
						fx=res.getX(); fy=res.getY(); fz=res.getZ()+1;
					}
					break;
				case ZM:
					if(res.getZ()==0){
						c=wf.getChunkByIndex(res.getChunkX(), res.getChunkY(), res.getChunkZ()-1);
						fx=res.getX(); fy=res.getY(); fz=Chunk.CHUNK_DIMENSION-1;
					} else {
						c=wf.getChunkByIndex(res.getChunkX(), res.getChunkY(), res.getChunkZ());
						fx=res.getX(); fy=res.getY(); fz=res.getZ()-1;
					}
					break;
				}
				if(c!=null) {
					System.out.println("CHUNK "+c.getX()+" "+c.getY()+" "+c.getZ());
					System.out.println("CUBE "+res.getX()+" "+res.getY()+" "+res.getZ());
					c.setCubeAt(fx, fy, fz,(byte)(this.selectedBlock));	
				}
			}
		}
	}
	protected boolean moveForward(double amount,WorldFacade wf)
	{
		boolean cx=moveX(this.xpos+Math.sin(this.yaw)*amount,wf);
		boolean cz=moveZ(this.zpos-Math.cos(this.yaw)*amount,wf);
		
		return cx||cz;
	}
	protected void moveLateral(double amount,WorldFacade wf)
	{
		moveZ(this.zpos+Math.sin(this.yaw)*amount,wf);
		moveX(this.xpos+Math.cos(this.yaw)*amount,wf);
	}
	void addPitch(float amount)
	{
		this.pitch+=amount;
		if(this.pitch>PIMEDIOS) this.pitch=(float)(PIMEDIOS);
		else if(this.pitch<-PIMEDIOS) this.pitch=(float)(-PIMEDIOS);
	}
	private void addYaw(float amount)
	{
		this.yaw+=amount;
	}
	private void updateCamera(WorldFacade wf)
	{
		wf.updateCameraCenter(this.xpos,this.ypos+EYE_POS,this.zpos);
		//this.cam.moveTo((float)this.xpos, (float)this.ypos+EYE_POS, (float)this.zpos);
		this.cam.setPitch(pitch);
		this.cam.setYaw(yaw);
	}
	private boolean moveX(double to,WorldFacade wf){
		this.grounded=false;
		double step=this.xpos;
		boolean end=false;
		if(to<step){
			while(!end){
				if(to<step-0.9) step=step-0.9;
				else{
					step=to;
					end=true;
				}
				if(!stepX(step,wf)) {end=false; break;}
			}
		}else{
			while(!end){
				if(to>step+0.9) step=step+0.9;
				else{
					step=to;
					end=true;
				}
				if(!stepX(step,wf)) {end=false; break;}
			}
		}
		
		return !end;
	}
	private boolean moveY(double to,WorldFacade wf){
		double step=this.ypos;
		boolean end=false;
		if(to<step){
			while(!end){
				if(to<step-0.9) step=step-0.9;
				else{
					step=to;
					end=true;
				}
				if(!stepY(step,wf)) {end=false; break;}
			}
		}else{
			while(!end){
				if(to>step+0.9) step=step+0.9;
				else{
					step=to;
					end=true;
				}
				if(!stepY(step,wf)) {end=false; break;}
			}
		}
		
		return !end;
	}
	private boolean moveZ(double to,WorldFacade wf){
		this.grounded=false;
		double step=this.zpos;
		boolean end=false;
		if(to<step){
			while(!end){
				if(to<step-0.9) step=step-0.9;
				else{
					step=to;
					end=true;
				}
				if(!stepZ(step,wf)) {end=false; break;}
			}
		}else{
			while(!end){
				if(to>step+0.9) step=step+0.9;
				else{
					step=to;
					end=true;
				}
				if(!stepZ(step,wf)) {end=false; break;}
			}
		}
		
		return !end;
	}
	private boolean stepX(double to,WorldFacade wf)
	{
		if(to<this.xpos)
		{
			if(!BlockLibrary.isSolid(wf.getContent(to-DEFAULT_SIZE, this.ypos, this.zpos+DEFAULT_SIZE_APROX))&&
					!BlockLibrary.isSolid(wf.getContent(to-DEFAULT_SIZE, this.ypos, this.zpos-DEFAULT_SIZE_APROX))&&
					!BlockLibrary.isSolid(wf.getContent(to-DEFAULT_SIZE, this.ypos+(DEFAULT_HEIGHT_APROX/2), this.zpos+DEFAULT_SIZE_APROX))&&
					!BlockLibrary.isSolid(wf.getContent(to-DEFAULT_SIZE, this.ypos+(DEFAULT_HEIGHT_APROX/2), this.zpos-DEFAULT_SIZE_APROX))&&
					!BlockLibrary.isSolid(wf.getContent(to-DEFAULT_SIZE, this.ypos+(DEFAULT_HEIGHT_APROX), this.zpos+DEFAULT_SIZE_APROX))&&
					!BlockLibrary.isSolid(wf.getContent(to-DEFAULT_SIZE, this.ypos+(DEFAULT_HEIGHT_APROX), this.zpos-DEFAULT_SIZE_APROX))) {this.xpos=to;return true;}
			else {this.xpos=(int)(Math.floor(to-DEFAULT_SIZE))+1+DEFAULT_SIZE;this.xvel=0;return false;}
		}else{
			if(!BlockLibrary.isSolid(wf.getContent(to+DEFAULT_SIZE, this.ypos, this.zpos+DEFAULT_SIZE_APROX))&&
					!BlockLibrary.isSolid(wf.getContent(to+DEFAULT_SIZE, this.ypos, this.zpos-DEFAULT_SIZE_APROX))&&
					!BlockLibrary.isSolid(wf.getContent(to+DEFAULT_SIZE, this.ypos+(DEFAULT_HEIGHT_APROX/2), this.zpos+DEFAULT_SIZE_APROX))&&
					!BlockLibrary.isSolid(wf.getContent(to+DEFAULT_SIZE, this.ypos+(DEFAULT_HEIGHT_APROX/2), this.zpos-DEFAULT_SIZE_APROX))&&
					!BlockLibrary.isSolid(wf.getContent(to+DEFAULT_SIZE, this.ypos+(DEFAULT_HEIGHT_APROX), this.zpos+DEFAULT_SIZE_APROX))&&
					!BlockLibrary.isSolid(wf.getContent(to+DEFAULT_SIZE, this.ypos+(DEFAULT_HEIGHT_APROX), this.zpos-DEFAULT_SIZE_APROX))) {this.xpos=to;return true;}
			else {this.xpos=(int)(Math.floor(to+DEFAULT_SIZE))-DEFAULT_SIZE; this.xvel=0;return false;}
		}
	}
	private boolean stepY(double to,WorldFacade wf)
	{
		this.grounded=false;
		if(to<this.ypos)
		{
			if(!BlockLibrary.isSolid(wf.getContent(this.xpos+DEFAULT_SIZE_APROX, to,this.zpos ))&&
					!BlockLibrary.isSolid(wf.getContent(this.xpos, to,this.zpos+DEFAULT_SIZE_APROX ))&&
					!BlockLibrary.isSolid(wf.getContent(this.xpos-DEFAULT_SIZE_APROX, to,this.zpos ))&&
					!BlockLibrary.isSolid(wf.getContent(this.xpos, to,this.zpos-DEFAULT_SIZE_APROX ))) {this.ypos=to;return true;}
			else {this.ypos=(int)(Math.floor(to))+1;this.yvel=0;this.grounded=true;return false;}
		}else{
			if(!BlockLibrary.isSolid(wf.getContent(this.xpos+DEFAULT_SIZE_APROX, to+DEFAULT_HEIGHT, this.zpos))&&
					!BlockLibrary.isSolid(wf.getContent(this.xpos, to+DEFAULT_HEIGHT,this.zpos+DEFAULT_SIZE_APROX ))&&
					!BlockLibrary.isSolid(wf.getContent(this.xpos-DEFAULT_SIZE_APROX, to+DEFAULT_HEIGHT,this.zpos ))&&
					!BlockLibrary.isSolid(wf.getContent(this.xpos, to+DEFAULT_HEIGHT,this.zpos-DEFAULT_SIZE_APROX ))) {this.ypos=to;return true;}
			else {this.ypos=(int)(Math.floor(to+DEFAULT_HEIGHT))-DEFAULT_HEIGHT;this.yvel=0;return false;}
		}
	}
	private boolean stepZ(double to,WorldFacade wf)
	{
		if(to<this.zpos)
		{
			if(!BlockLibrary.isSolid(wf.getContent(this.xpos+DEFAULT_SIZE_APROX, this.ypos, to-DEFAULT_SIZE))&&
					!BlockLibrary.isSolid(wf.getContent(this.xpos-DEFAULT_SIZE_APROX, this.ypos, to-DEFAULT_SIZE))&&
					!BlockLibrary.isSolid(wf.getContent(this.xpos+DEFAULT_SIZE_APROX, this.ypos+(DEFAULT_HEIGHT_APROX/2), to-DEFAULT_SIZE))&&
					!BlockLibrary.isSolid(wf.getContent(this.xpos-DEFAULT_SIZE_APROX, this.ypos+(DEFAULT_HEIGHT_APROX/2), to-DEFAULT_SIZE))&&
					!BlockLibrary.isSolid(wf.getContent(this.xpos+DEFAULT_SIZE_APROX, this.ypos+(DEFAULT_HEIGHT_APROX), to-DEFAULT_SIZE))&&
					!BlockLibrary.isSolid(wf.getContent(this.xpos-DEFAULT_SIZE_APROX, this.ypos+(DEFAULT_HEIGHT_APROX), to-DEFAULT_SIZE))) {this.zpos=to; return true;}
			else {this.zpos=(int)(Math.floor(to-DEFAULT_SIZE))+1+DEFAULT_SIZE;this.zvel=0;return false;}
		}else{
			if(!BlockLibrary.isSolid(wf.getContent(this.xpos+DEFAULT_SIZE_APROX, this.ypos, to+DEFAULT_SIZE))&&
					!BlockLibrary.isSolid(wf.getContent(this.xpos-DEFAULT_SIZE_APROX, this.ypos, to+DEFAULT_SIZE))&&
					!BlockLibrary.isSolid(wf.getContent(this.xpos+DEFAULT_SIZE_APROX, this.ypos+(DEFAULT_HEIGHT_APROX/2), to+DEFAULT_SIZE))&&
					!BlockLibrary.isSolid(wf.getContent(this.xpos-DEFAULT_SIZE_APROX, this.ypos+(DEFAULT_HEIGHT_APROX/2), to+DEFAULT_SIZE))&&
					!BlockLibrary.isSolid(wf.getContent(this.xpos+DEFAULT_SIZE_APROX, this.ypos+(DEFAULT_HEIGHT_APROX), to+DEFAULT_SIZE))&&
					!BlockLibrary.isSolid(wf.getContent(this.xpos-DEFAULT_SIZE_APROX, this.ypos+(DEFAULT_HEIGHT_APROX), to+DEFAULT_SIZE))) {this.zpos=to; return true;}
			else {this.zpos=(int)(Math.floor(to+DEFAULT_SIZE))-DEFAULT_SIZE;this.zvel=0;return false;}
		}
	}
	public double getX()
	{
		return this.xpos;
	}
	public double getY()
	{
		return this.ypos;
	}
	public double getZ()
	{
		return this.zpos;
	}
	@Override
	public void notifyKeyToggle(int code) {
		switch(code)
		{
		case InputHandler.SHIFT_VALUE:
			if(flying)
			{
				flying=false;
				this.currentSpeed=DEFAULT_SPEED;
			}
			else
			{
				flying=true;
				this.currentSpeed=FLY_SPEED;
			}
			break;
		case InputHandler.MOUSE_BUTTON2_VALUE:
			this.action_deleteNextBlock=true;
			break;
		case InputHandler.MOUSE_BUTTON1_VALUE:
			this.action_createNextBlock=true;
			break;
		}
		
	}
	@Override
	public void notifyKeyIncrement(int code, int value) {
		switch(code)
		{
		case InputHandler.MOUSE_WHEEL_VALUE:
			this.selectedBlock+=value;
			while(this.selectedBlock<0) this.selectedBlock+=BlockLibrary.size();
			while(this.selectedBlock>=BlockLibrary.size()) this.selectedBlock-=BlockLibrary.size();
			GlobalTextManager.insertText(BlockLibrary.getName((byte)this.selectedBlock)+"");
			break;
		}
	}
	
	public static RaycastResult raycast(double pitch,double yaw,double ix,double iy,double iz,WorldFacade wf,double maxdist)
	{
		double dy=-Math.sin(pitch);
		double subx=Math.cos(pitch);
		double dz=-Math.cos(yaw)*subx;
		double dx=Math.sin(yaw)*subx;
		System.out.println(dx+","+dy+","+dz);
		System.out.println(ix+" "+iy+" "+iz);
		double cx=ix;
		double cy=iy;
		double cz=iz;
		double nextx,nexty,nextz;
		double currentDist=0;
		Block.facecode currentFace;
		while(currentDist<maxdist)
		{
			if(dx>0){
				nextx=Math.ceil(cx)-cx;
				if(nextx==0) nextx=nextx+1;
			}
			else{
				nextx=Math.floor(cx)-cx;
				if(nextx==0) nextx=nextx-1;
			}
			if(dy>0){
				nexty=Math.ceil(cy)-cy;
				if(nexty==0) nexty=nexty+1;
			}
			else{
				nexty=Math.floor(cy)-cy;
				if(nexty==0) nexty=nexty-1;
			}
			if(dz>0){
				nextz=Math.ceil(cz)-cz;
				if(nextz==0) nextz=nextz+1;
			}
			else{
				nextz=Math.floor(cz)-cz;
				if(nextz==0) nextz=nextz-1;
			}
			double mulx,muly,mulz;
			mulx= nextx/dx;
			muly= nexty/dy;
			mulz= nextz/dz;
			double mul;
			if(mulx<muly&&mulx<mulz){
				mul=mulx;
				currentFace=dx>0?Block.facecode.XM : Block.facecode.XP;
			}
			else if(muly<mulx&&muly<mulz){
				mul=muly;
				currentFace=dy>0?Block.facecode.YM : Block.facecode.YP;
			}
			else{
				mul=mulz;
				currentFace=dz>0?Block.facecode.ZM : Block.facecode.ZP;
			}
			System.out.println(mul);
			mul+=0.001;
			cx=cx+(dx*mul);
			cy=cy+(dy*mul);
			cz=cz+(dz*mul);
			System.out.println(cx+","+cy+","+cz);
			Chunk c=wf.getChunkByIndex((int)Math.floor(cx/Chunk.CHUNK_DIMENSION), (int)Math.floor(cy/Chunk.CHUNK_DIMENSION), (int)Math.floor(cz/Chunk.CHUNK_DIMENSION));
			if(c!=null)
			{
				int cubex=VoxelUtils.trueMod(cx, Chunk.CHUNK_DIMENSION);
				int cubey=VoxelUtils.trueMod(cy, Chunk.CHUNK_DIMENSION);
				int cubez=VoxelUtils.trueMod(cz, Chunk.CHUNK_DIMENSION);
				byte cube=c.getCubeAt(cubex,cubey,cubez);
				System.out.println("Chunk "+c.getX()+","+c.getY()+","+c.getZ());
				System.out.println("Found "+cubex+","+cubey+","+cubez);
				System.out.println("Val: "+cube);
				if(BlockLibrary.isSolid(cube))
				{
					return new RaycastResult(currentFace,c.getX(),c.getY(),c.getZ(),cubex,cubey,cubez);
				}
			}
			currentDist=Math.sqrt((cx-ix)*(cx-ix) + (cy-iy)*(cy-iy) +(cz-iz)*(cz-iz));
		}
		return null;
	}
	public static class RaycastResult
	{
		private int px,py,pz;
		private int cx,cy,cz;
		private Block.facecode face;
		public RaycastResult(Block.facecode facecode,int cx,int cy,int cz,int px,int py,int pz)
		{
			this.face=facecode;
			this.px=px;
			this.py=py;
			this.pz=pz;
			this.cx=cx;
			this.cy=cy;
			this.cz=cz;
		}
		public int getX()
		{
			return this.px;
		}
		public int getY()
		{
			return this.py;
		}
		public int getZ()
		{
			return this.pz;
		}
		public int getChunkX()
		{
			return this.cx;
		}
		public int getChunkY()
		{
			return this.cy;
		}
		public int getChunkZ()
		{
			return this.cz;
		}
		public Block.facecode getFaceCode()
		{
			return this.face;
		}
	}
}
