package ivengine.view;

import org.lwjgl.util.vector.Matrix4f;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 *  @author Víctor Arellano Vicente (Ivelate)
 *  
 *  Inverts the projection matrix of a Camera, and stores it. If the projection matrix of the camera changes, the inverse will be recalculated *
 */
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
