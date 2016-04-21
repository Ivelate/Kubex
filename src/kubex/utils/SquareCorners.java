package kubex.utils;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector4f;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Used to contain a set of 4 points, allowing some opperations over them
 */
public class SquareCorners 
{
	public final Vector4f xmym,xpym,xmyp,xpyp;
	
	public SquareCorners(Vector4f xmym,Vector4f xpym,Vector4f xmyp,Vector4f xpyp)
	{
			this.xmym=toHomCoord(xmym);
			this.xpym=toHomCoord(xpym); 
			this.xmyp=toHomCoord(xmyp); 
			this.xpyp=toHomCoord(xpyp); 
	}
	
	private Vector4f toHomCoord(Vector4f v)
	{
		v.x=v.x/v.w;
		v.y=v.y/v.w;
		v.z=v.z/v.w;
		v.w=1;
		return v;
	}
	
	public static SquareCorners mul(SquareCorners sc,Matrix4f mat)
	{
		return new SquareCorners(	Matrix4f.transform(mat, sc.xmym, null),
									Matrix4f.transform(mat, sc.xpym, null),
									Matrix4f.transform(mat, sc.xmyp, null),
									Matrix4f.transform(mat, sc.xpyp, null));
	}
	public static SquareCorners add(SquareCorners sc,float x,float y,float z)
	{
		Vector4f despl=new Vector4f(x,y,z,0);
		return new SquareCorners(	Vector4f.add(sc.xmym, despl, null),
									Vector4f.add(sc.xpym, despl, null),
									Vector4f.add(sc.xmyp, despl, null),
									Vector4f.add(sc.xpyp, despl, null));
	}
}
