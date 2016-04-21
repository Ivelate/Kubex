package kubex.utils;

import org.lwjgl.util.vector.Vector4f;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Bounding Box aligned with world axis
 */
public class BoundingBox 
{
	private float mx=Float.MAX_VALUE;
	private float px=-Float.MAX_VALUE;
	
	private float my=Float.MAX_VALUE;
	private float py=-Float.MAX_VALUE;
	
	private float mz=Float.MAX_VALUE;
	private float pz=-Float.MAX_VALUE;
	
	public void adaptForPoint(Vector4f p)
	{
		if(mx>p.x) mx=p.x;
		if(px<p.x) px=p.x;
		
		if(my>p.y) my=p.y;
		if(py<p.y) py=p.y;
		
		if(mz>p.z) mz=p.z;
		if(pz<p.z) pz=p.z;
	}

	public void adaptForSquareCorners(SquareCorners sc)
	{
		this.adaptForPoint(sc.xmym);
		this.adaptForPoint(sc.xpym);
		this.adaptForPoint(sc.xmyp);
		this.adaptForPoint(sc.xpyp);
	}
	
	public void adaptZForSquareCorners(SquareCorners sc)
	{
		if(pz<sc.xmym.z) pz=sc.xmym.z;
		
		if(pz<sc.xpym.z) pz=sc.xpym.z;
		
		if(pz<sc.xmyp.z) pz=sc.xmyp.z;
		
		if(pz<sc.xpyp.z) pz=sc.xpyp.z;
	}
	
	public void resetZAxis()
	{
		this.mz=Float.MAX_VALUE;
		this.pz=-Float.MAX_VALUE;
	}
	
	/********************************* GETTERS ********************************/
	
	public float getMx() {
		return mx;
	}

	public float getPx() {
		return px;
	}

	public float getMy() {
		return my;
	}

	public float getPy() {
		return py;
	}

	public float getMz() {
		return mz;
	}

	public float getPz() {
		return pz;
	}

}
