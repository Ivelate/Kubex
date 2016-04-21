package kubex.entity;

import org.lwjgl.input.Mouse;

import ivengine.view.Camera;
import kubex.blocks.Block;
import kubex.blocks.BlockLibrary;
import kubex.gui.Chunk;
import kubex.gui.GlobalTextManager;
import kubex.gui.WorldFacade;
import kubex.utils.InputHandler;
import kubex.utils.KeyToggleListener;
import kubex.utils.KeyValueListener;
import kubex.utils.VoxelUtils;

/**
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Player class. Contains all data refering to the player, its velocity, position, cube selected, state, etc.
 * Contains a collision engine used against the world when moving.
 */
public class Player implements KeyToggleListener, KeyValueListener
{
	private static final double DEFAULT_SPEED=5; 		//5m/s when walking
	private static final double FLY_SPEED=30;			//30m/s when flying
	private static final double DEFAULT_JUMPSPEED=6; 	//jumping at 6m/s speed
	private static final double FLY_JUMPSPEED=15;		//When flying, jump velocity is rised to 15m/s
	private static final double DEFAULT_HEIGHT=1.8f;	//Player height is 1.8m
	private static final double DEFAULT_HEIGHT_APROX=1.79f;
	private static final double EYE_POS=1.65f;			//Players eyes are positioned at 1.65m from his feet
	private static final double DEFAULT_SIZE=0.4f;		//The player is a 0.4 width on each side. So, 0.8m total. Total measures: 0.8x1.8x0.8
	private static final double DEFAULT_SIZE_APROX=0.39f;
	private static final double DEFAULT_MOUSE_SENSIVITY=150;
	private static final double PIMEDIOS=(float)(Math.PI/2);
	private static final double SQRT2=(float)Math.sqrt(2);
	private static final double MAX_RAYCAST_DISTANCE=5; //Player can place or delete blocks up to 5 blocks away.
	
	private Camera cam;
	private double xpos,ypos,zpos;
	private float pitch,yaw;
	private double yvel=0;
	private double xvel=0;
	private double zvel=0;
	private double currentSpeed=DEFAULT_SPEED;
	private boolean flying=false; //If the player is flying
	private boolean action_deleteNextBlock=false;
	private boolean action_createNextBlock=false;
	private boolean grounded=false; //If the player is touching the ground
	
	private float mouseSensitivityMult=1;
	
	private byte[] cubeShortcuts;
	
	private byte selectedBlock;
	
	
	public Player(double ix,double iy,double iz,float pitch,float yaw,byte[] cubeShortcuts,byte selectedBlock,float mouseSensitivity,Camera cam)
	{
		this.mouseSensitivityMult=mouseSensitivity;
		this.cubeShortcuts=cubeShortcuts;
		this.selectedBlock=selectedBlock;
		this.cam=cam;
		this.xpos=ix;this.ypos=iy;this.zpos=iz;
		this.pitch=pitch;this.yaw=yaw;
		InputHandler.addKeyToggleListener(InputHandler.SHIFT_VALUE,this);
		InputHandler.addKeyToggleListener(InputHandler.MOUSE_BUTTON1_VALUE,this);
		InputHandler.addKeyToggleListener(InputHandler.MOUSE_BUTTON2_VALUE,this);
		
		for(int i=0;i<cubeShortcuts.length;i++) InputHandler.addKeyToggleListener(InputHandler.NUM_0_VALUE+i,this);
		
		InputHandler.addKeyValueListener(InputHandler.MOUSE_WHEEL_VALUE, this);
	}
	/**
	 * Updates the player state, moving as requested. 
	 */
	public void update(float tEl,WorldFacade wf)
	{
		handleEvents(wf);

		double yAxisPressed=InputHandler.isWPressed()||InputHandler.isSPressed()?SQRT2:1; //Used to mantain right modulus velocity when moving both forwards and laterally, for example
		double xAxisPressed=InputHandler.isAPressed()||InputHandler.isDPressed()?SQRT2:1;
		boolean underwater=isUnderwater(wf,EYE_POS/2);
		currentSpeed=flying?FLY_SPEED:underwater?DEFAULT_SPEED/2:DEFAULT_SPEED; //Speed on water is 1/2 the normal speed
		
		//If player is pressing W against a wall, he/she will start to climb it. If the player were falling too fast when he got to the wall, he/she will fail to grab it and will not climb, but fall.
		boolean climbing=false;
		if(InputHandler.isWPressed()) {
			if(moveForward(currentSpeed*tEl/xAxisPressed,wf)&&this.yvel>-1){
				if(this.yvel<0) this.yvel+=tEl;
				this.moveY(this.ypos+currentSpeed*2/3*tEl, wf); //Climbing speed is 2/3 of the normal speed
				climbing=true;
			}
		}
		if(InputHandler.isAPressed()) moveLateral(-currentSpeed*tEl/yAxisPressed,wf);
		if(InputHandler.isSPressed()) moveForward(-currentSpeed*tEl/xAxisPressed,wf);
		if(InputHandler.isDPressed()) moveLateral(currentSpeed*tEl/yAxisPressed,wf);
		this.addPitch(-(float)((Mouse.getDY()*this.mouseSensitivityMult)/DEFAULT_MOUSE_SENSIVITY));
		this.addYaw((float)((Mouse.getDX()*this.mouseSensitivityMult)/DEFAULT_MOUSE_SENSIVITY));
		
		//Gravity
		if(!grounded&&!climbing) {
			if(underwater){ //Underwater falling speed is disminished. It has drag, too
				this.yvel-=wf.getWorldGravity()*tEl/3;
				if(this.yvel<-wf.getWorldGravity()/3) this.yvel=-wf.getWorldGravity()/3;
			}
			else this.yvel-=wf.getWorldGravity()*tEl; //Free fall speed doesn't have drag and keeps accelerating until a contact with the ground is made
		}
		this.moveY(this.ypos+(yvel*tEl),wf);
		
		//Jumping
		if(InputHandler.isSPACEPressed()) 
		{
			if(this.grounded){
				this.yvel=DEFAULT_JUMPSPEED;
				this.grounded=false;
			}
			//When underwater or flying you don't need to be grounded to jump
			else if(underwater){
				this.yvel=isUnderwater(wf)?DEFAULT_JUMPSPEED/2:DEFAULT_JUMPSPEED;
				this.grounded=false;
			}
			if(flying) this.yvel=FLY_JUMPSPEED;
		}

		updateCamera(wf);

		wf.reloadPlayerFOV((int)Math.floor(this.getX()/Chunk.CHUNK_DIMENSION), 0, (int)Math.floor(this.getZ()/Chunk.CHUNK_DIMENSION));
	}
	
	/**
	 * Returns true if player is underwater
	 */
	public boolean isUnderwater(WorldFacade wf)
	{
		return isUnderwater(wf,EYE_POS);
	}
	public boolean isUnderwater(WorldFacade wf,double yoffset)
	{
		return BlockLibrary.isLiquid(wf.getContent(this.xpos, this.ypos+yoffset, this.zpos));
	}
	
	/**
	 * Returns the average light the player is exposed to, from all directions. When considering natural or artificial light in the same block, the maximum of both will be the one taken.
	 */
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
	
	/**
	 * Handles deleting block in front of the player or placing block in front of the player events.
	 */
	private void handleEvents(WorldFacade wf)
	{
		if(action_deleteNextBlock)
		{
			action_deleteNextBlock=false;
			RaycastResult res=raycast(this.pitch,this.yaw,this.xpos,this.ypos+EYE_POS,this.zpos,wf,MAX_RAYCAST_DISTANCE);
			if(res!=null)
			{
				Chunk c=wf.getChunkByIndex(res.getChunkX(), res.getChunkY(), res.getChunkZ());
				//The block can only be destroyed if it is not the indestructible block or if the player is flying (some sort of godmode)
				if(c!=null&&(this.flying||c.getCubeAt(res.getX(), res.getY(), res.getZ())!=27)) 
					c.setCubeAt(res.getX(), res.getY(), res.getZ(),(byte)(0));	 //Deletes the raycasted block, switching it with air
			}
		}
		//Place selected block in the raycasted place, if it is possible (Player is not occuping it)
		if(action_createNextBlock)
		{
			action_createNextBlock=false;
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
					double cubeAbsX=c.getX()*Chunk.CHUNK_DIMENSION + fx;
					double cubeAbsY=c.getY()*Chunk.CHUNK_DIMENSION + fy;
					double cubeAbsZ=c.getZ()*Chunk.CHUNK_DIMENSION + fz;
					
					//Inserting cube only if the player is not inside its boundaries
					if(!(	cubeAbsX<this.getX()+DEFAULT_SIZE&&cubeAbsX+1>this.getX()-DEFAULT_SIZE &&
							cubeAbsY<this.getY()+DEFAULT_HEIGHT&&cubeAbsY+0.5f>this.getY() &&
							cubeAbsZ<this.getZ()+DEFAULT_SIZE&&cubeAbsZ+1>this.getZ()-DEFAULT_SIZE )) c.setCubeAt(fx, fy, fz,(byte)(this.selectedBlock));	
				}
			}
		}
	}
	
	/**
	 * Moves the player forward an amount of <amount> m. Each axis moved will be based on the player current cam yaw. Manages collisions
	 */
	protected boolean moveForward(double amount,WorldFacade wf)
	{
		boolean cx=moveX(this.xpos+Math.sin(this.yaw)*amount,wf);
		boolean cz=moveZ(this.zpos-Math.cos(this.yaw)*amount,wf);
		
		return cx||cz;
	}
	/**
	 * Moves the player laterally an amount of <amount> m. Each axis moved will be based on the player current cam yaw. Manages collisions
	 */
	protected void moveLateral(double amount,WorldFacade wf)
	{
		moveZ(this.zpos+Math.sin(this.yaw)*amount,wf);
		moveX(this.xpos+Math.cos(this.yaw)*amount,wf);
	}
	
	/**
	 * Adds a pitch <amount> to the camera
	 */
	void addPitch(float amount)
	{
		this.pitch+=amount;
		if(this.pitch>PIMEDIOS) this.pitch=(float)(PIMEDIOS);
		else if(this.pitch<-PIMEDIOS) this.pitch=(float)(-PIMEDIOS);
	}
	
	/**
	 * Adds a yaw <amount> to the camera
	 */
	private void addYaw(float amount)
	{
		this.yaw+=amount;
	}
	
	/**
	 * Updates the camera matrixes with the current pitch, yaw and player position. As camera is centered in the origin (Rendering with player position as world center), the world center will be moved
	 * instead.
	 */
	private void updateCamera(WorldFacade wf)
	{
		wf.updateCameraCenter(this.xpos,this.ypos+EYE_POS,this.zpos);
		this.cam.setPitch(pitch);
		this.cam.setYaw(yaw);
	}
	
	/**
	 * Moves a player X coordinate to the x pos <to>, managing collisions and high speed cases, managing collisions on each different block traveled in this movement tick
	 */
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
	
	/**
	 * Moves a player Y coordinate to the y pos <to>, managing collisions and high speed cases, managing collisions on each different block traveled in this movement tick
	 */
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
	
	/**
	 * Moves a player Z coordinate to the z pos <to>, managing collisions and high speed cases, managing collisions on each different block traveled in this movement tick
	 */
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
	
	/**
	 * Returns player x position
	 */
	public double getX()
	{
		return this.xpos;
	}
	
	/**
	 * Returns player y position
	 */
	public double getY()
	{
		return this.ypos;
	}
	
	/**
	 * Returns player z position
	 */
	public double getZ()
	{
		return this.zpos;
	}
	
	/**
	 * Returns the current cube shortcuts
	 */
	public byte[] getCubeShortcuts()
	{
		return this.cubeShortcuts;
	}
	
	/**
	 * Returns the current selected cube
	 */
	public byte getCubeSelected()
	{
		return this.selectedBlock;
	}
	
	/**
	 * Manages keyboard toggle events
	 */
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
		case InputHandler.NUM_0_VALUE:
		case InputHandler.NUM_1_VALUE:
		case InputHandler.NUM_2_VALUE:
		case InputHandler.NUM_3_VALUE:
		case InputHandler.NUM_4_VALUE:
		case InputHandler.NUM_5_VALUE:
		case InputHandler.NUM_6_VALUE:
		case InputHandler.NUM_7_VALUE:
		case InputHandler.NUM_8_VALUE:
		case InputHandler.NUM_9_VALUE:
			int val=code-InputHandler.NUM_0_VALUE;
			if(InputHandler.isCTRLPressed())
			{
				this.cubeShortcuts[val]=this.selectedBlock;
				GlobalTextManager.insertText("Assigned to shortcut "+val+": "+BlockLibrary.getName((byte)this.selectedBlock));
			}
			else
			{
				this.selectedBlock=this.cubeShortcuts[val];
				GlobalTextManager.insertText("Shortcut "+val+": "+BlockLibrary.getName((byte)this.selectedBlock));
			}
			break;
		}
		
	}
	
	/**
	 * Manages mouse wheel inputs, used to switch between selected blocks.
	 */
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
	
	/**
	 * Raycasts a ray from the current position <ix> <iy> <iz> with angles <pitch> <yaw> until a the ray collides with a cube
	 * or <maxdist> is reached.
	 */
	public static RaycastResult raycast(double pitch,double yaw,double ix,double iy,double iz,WorldFacade wf,double maxdist)
	{
		double dy=-Math.sin(pitch);
		double subx=Math.cos(pitch);
		double dz=-Math.cos(yaw)*subx;
		double dx=Math.sin(yaw)*subx;

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

			mul+=0.001;
			cx=cx+(dx*mul);
			cy=cy+(dy*mul);
			cz=cz+(dz*mul);

			Chunk c=wf.getChunkByIndex((int)Math.floor(cx/Chunk.CHUNK_DIMENSION), (int)Math.floor(cy/Chunk.CHUNK_DIMENSION), (int)Math.floor(cz/Chunk.CHUNK_DIMENSION));
			if(c!=null)
			{
				int cubex=VoxelUtils.trueMod(cx, Chunk.CHUNK_DIMENSION);
				int cubey=VoxelUtils.trueMod(cy, Chunk.CHUNK_DIMENSION);
				int cubez=VoxelUtils.trueMod(cz, Chunk.CHUNK_DIMENSION);
				byte cube=c.getCubeAt(cubex,cubey,cubez);

				if(BlockLibrary.isSolid(cube))
				{
					return new RaycastResult(currentFace,c.getX(),c.getY(),c.getZ(),cubex,cubey,cubez);
				}
			}
			currentDist=Math.sqrt((cx-ix)*(cx-ix) + (cy-iy)*(cy-iy) +(cz-iz)*(cz-iz));
		}
		return null;
	}
	
	/**
	 * Raycast result struct
	 */
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
