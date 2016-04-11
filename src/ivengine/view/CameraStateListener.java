package ivengine.view;

/**
 *  @author Víctor Arellano Vicente (Ivelate)
 *  
 *  For objects that needs to be notified if the projection matrix of a camera changes
 */
public interface CameraStateListener 
{
	public void onProjectionMatrixChange(Camera c);
}
