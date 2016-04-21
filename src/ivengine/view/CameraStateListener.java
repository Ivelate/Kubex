package ivengine.view;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 *  @author Víctor Arellano Vicente (Ivelate)
 *  
 *  For objects that needs to be notified if the projection matrix of a camera changes
 */
public interface CameraStateListener 
{
	public void onProjectionMatrixChange(Camera c);
}
