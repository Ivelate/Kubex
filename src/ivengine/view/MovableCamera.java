package ivengine.view;

import org.lwjgl.util.vector.Matrix4f;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
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
