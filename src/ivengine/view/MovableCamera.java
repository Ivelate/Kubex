package ivengine.view;

import org.lwjgl.util.vector.Matrix4f;

public abstract class MovableCamera extends Camera{

	public MovableCamera(float znear, float zfar, float fov, float arat) {
		super(znear, zfar, fov, arat);
		// TODO Auto-generated constructor stub
	}
	public MovableCamera(Matrix4f mat)
	{
		super(mat);
	}
	public abstract void moveForward(float amount);
	public abstract void moveLateral(float amount);
	public abstract void moveUp(float amount);

}
