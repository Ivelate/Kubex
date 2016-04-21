package kubex.utils;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Equivalent of the Vector3f class but using doubles, with high precision
 */
public class Vector3d 
{
	public double x;
	public double y;
	public double z;
	public Vector3d()
	{
		this.x=0;
		this.y=0;
		this.z=0;
	}
	public Vector3d(double x,double y,double z)
	{
		this.x=x;
		this.y=y;
		this.z=z;
	}
}
