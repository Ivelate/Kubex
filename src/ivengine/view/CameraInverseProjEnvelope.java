package ivengine.view;

import org.lwjgl.util.vector.Matrix4f;

public class CameraInverseProjEnvelope implements CameraStateListener
{
	private Camera cam;
	private Matrix4f invProj=new Matrix4f();
	
	public CameraInverseProjEnvelope(Camera c)
	{
		this.cam=c;
		this.cam.addCameraStateListener(this);
		onProjectionMatrixChange(this.cam);
	}

	@Override
	public void onProjectionMatrixChange(Camera c) 
	{
		Matrix4f.invert(c.getProjectionMatrix(), this.invProj);
	}
	
	public Matrix4f getInvProjMatrix()
	{
		return this.invProj;
	}
}
