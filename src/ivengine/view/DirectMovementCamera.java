package ivengine.view;

import org.lwjgl.util.vector.Matrix4f;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 *  @author Víctor Arellano Vicente (Ivelate)
 *  
 *  Camera that automatically handles moving forward / Laterally based on the angle it's looking.
 */
public class DirectMovementCamera extends MovableCamera{

	public DirectMovementCamera(float znear, float zfar, float fov, float arat) {
		super(znear, zfar, fov, arat);
	}
	public DirectMovementCamera(Matrix4f mat)
	{
		super(mat);
	}
	@Override
	public void moveForward(float amount)
	{
		this.pos.x-=Math.sin(this.yaw)*Math.cos(this.pitch)*amount;
		this.pos.y+=Math.sin(this.pitch)*amount;
		this.pos.z+=Math.cos(this.yaw)*amount*Math.cos(this.pitch);
	}
	@Override
	public void moveLateral(float amount)
	{
		this.pos.z-=Math.sin(this.yaw)*amount;
		this.pos.x-=Math.cos(this.yaw)*amount;
	}
	@Override
	public void moveUp(float amount)
	{
		this.pos.y-=amount;
	}
}
