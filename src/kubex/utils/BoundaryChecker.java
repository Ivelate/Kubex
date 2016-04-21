package kubex.utils;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Boundary checker interface. Custom object usable when aplying culling to the chunks. The most common one are the bounding box checkers, coming from the shadows bounding boxes,
 * applying culling when calculating shadows
 */
public interface BoundaryChecker 
{
	/**
	 * Returns true if a sphere with position <x> <y> <z> and radius <radius> has some part of it inside the boundaries delimited by the concrete object implementing this interface
	 */
	public boolean sharesBoundariesWith(float x,float y,float z,float radius);
	
	/**
	 * Returns true if GL_CULL_FACE needs to be applied when using this boundary checker
	 */
	public boolean applyCullFace();
}
