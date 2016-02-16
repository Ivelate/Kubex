package monecruft.utils;

import org.lwjgl.util.vector.Vector4f;

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
		//if(mz>sc.xmym.z) mz=sc.xmym.z;
		if(pz<sc.xmym.z) pz=sc.xmym.z;
		
		//if(mz>sc.xpym.z) mz=sc.xpym.z;
		if(pz<sc.xpym.z) pz=sc.xpym.z;
		
		//if(mz>sc.xmyp.z) mz=sc.xmyp.z;
		if(pz<sc.xmyp.z) pz=sc.xmyp.z;
		
		//if(mz>sc.xpyp.z) mz=sc.xpyp.z;
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
