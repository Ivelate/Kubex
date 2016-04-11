package ivengine.view;

import org.lwjgl.util.vector.Matrix4f;

/**
 *  @author Víctor Arellano Vicente (Ivelate)
 *  
 *  Wrapper for DirectoMovementCamera implementation
 */
public abstract class MovableCamera extends Camera{

	public MovableCamera(float znear, float zfar, float fov, float arat) {
		super(znear, zfar, fov, arat);
	}
	public MovableCamera(Matrix4f mat)
	{
		super(mat);
	}
	public abstract void moveForward(float amount);
	public abstract void moveLateral(float amount);
	public abstract void moveUp(float amount);

}
