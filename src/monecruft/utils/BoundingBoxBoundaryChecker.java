package monecruft.utils;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

public class BoundingBoxBoundaryChecker implements BoundaryChecker
{
	private BoundingBox bb;
	
	private Matrix4f viewMatrix;
	
	private Vector4f aux=new Vector4f();
	
	public BoundingBoxBoundaryChecker(BoundingBox bb,Matrix4f viewMatrix)
	{
		this.bb=bb;
		this.viewMatrix=viewMatrix;
	}

	@Override
	public boolean sharesBoundariesWith(float x, float y, float z, float radius) 
	{
		aux.x=x;aux.y=y;aux.z=z;aux.w=1;
		Matrix4f.transform(this.viewMatrix, this.aux, this.aux);
		
		if(aux.x-radius>bb.getPx()||aux.x+radius<bb.getMx()) return false;
		
		if(aux.y-radius>bb.getPy()||aux.y+radius<bb.getMy()) return false;
		
		if(aux.z-radius>bb.getPz()||aux.z+radius<bb.getMz()) return false;
		
		return true;
	}
	
	@Override
	public boolean applyCulling()
	{
		return false;
	}
}
