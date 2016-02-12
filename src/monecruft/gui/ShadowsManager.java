package monecruft.gui;

import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector4f;

import ivengine.view.Camera;
import ivengine.view.CameraStateListener;
import ivengine.view.MatrixHelper;
import monecruft.utils.BoundingBox;
import monecruft.utils.SquareCorners;

public class ShadowsManager implements CameraStateListener
{
	private static final Matrix4f screenLocationMat=MatrixHelper.createScaleTranslationMatrix(0.5f, 0.5f, 0.5f, 0.5f);
	private final int nsplits;
	private final float[] dsplits;
	private final SquareCorners[] splitsCorners;
	
	private Matrix4f[] splitsOrthoProjections;
	
	private Camera camera;
	
	public ShadowsManager(float near,float far,int splits,Camera standardCam)
	{
		this(ShadowsManager.calculateSplits(near, far, splits,0.5f),standardCam);
	}
	public ShadowsManager(float[] dsplits,Camera standardCam)
	{
		this.nsplits=dsplits.length-1;
		this.dsplits=dsplits;
		this.camera=standardCam;
		this.splitsCorners=new SquareCorners[dsplits.length];
		this.splitsOrthoProjections=new Matrix4f[dsplits.length];
		for(int i=0;i<this.splitsOrthoProjections.length;i++) this.splitsOrthoProjections[i]=new Matrix4f();
		
		//Store camera projmat inv
		fillSplitsCorners(standardCam.getProjectionMatrix());
	}
	public static float[] calculateSplits(float near,float far,int splits,float lambda)
	{
		float[] dsplits=new float[splits+1];
		
		float fsplits=splits;
		
		//From http://http.developer.nvidia.com/GPUGems3/gpugems3_ch10.html
		for(int s=0;s<splits;s++)
		{
			float cuni=near+ (far-near)*(s/fsplits);
			float clog=(float)(near*Math.pow(far/near,s/fsplits));
			dsplits[s]=lambda*cuni + (1-lambda)*clog;
			//System.out.println("SPLIT "+s+ " val "+this.dsplits[s]);
		}
		dsplits[splits]=far;	
		
		return dsplits;
	}
	
	private void fillSplitsCorners(Matrix4f projMat)
	{
		float[] depthSplits=new float[this.dsplits.length];
		Vector4f buf=new Vector4f();
		for(int s=0;s<this.dsplits.length;s++)
		{
			buf.x=0;buf.y=0;buf.z=-this.dsplits[s];buf.w=1;
			Matrix4f.transform(projMat, buf, buf);
			depthSplits[s]=buf.z/buf.w;
		}
		
		/*System.out.println(Matrix4f.transform(projMat, new Vector4f(10,10,-0.1f,1), null));
		System.out.println(Matrix4f.transform(projMat, new Vector4f(10,10,-10,1), null));
		System.out.println(Matrix4f.transform(projMat, new Vector4f(10,10,-100,1), null));
		System.out.println(Matrix4f.transform(projMat, new Vector4f(10,10,-300,1), null));
		System.out.println(Matrix4f.transform(projMat, new Vector4f(10,10,-10000,1), null));*/
		
		Matrix4f pi=Matrix4f.invert(projMat, null);
		for(int i=0;i<this.splitsCorners.length;i++)
		{
			//System.out.println(depthSplits[i]);
			splitsCorners[i]=new SquareCorners(	Matrix4f.transform(pi, new Vector4f(-1,-1,depthSplits[i],1), null),
												Matrix4f.transform(pi, new Vector4f(1,-1,depthSplits[i],1), null),
												Matrix4f.transform(pi, new Vector4f(-1,1,depthSplits[i],1), null),
												Matrix4f.transform(pi, new Vector4f(1,1,depthSplits[i],1), null));
		}
	}
	
	public void calculateCascadeShadows(Matrix4f sunViewMatrix,SquareCorners worldCornersLow,SquareCorners worldCornersHigh)
	{
		//Get inverse of view mat multiplied by sun view matrix, so M(cv->sv)
		Matrix4f mcvsv=MatrixHelper.getAffineInverse(this.camera.getViewMatrix(),null);
		Matrix4f.mul(sunViewMatrix, mcvsv, mcvsv);
		
		SquareCorners worldCornersLowShadow=SquareCorners.mul(worldCornersLow, sunViewMatrix);
		SquareCorners worldCornersHighShadow=SquareCorners.mul(worldCornersHigh, sunViewMatrix);
		
		SquareCorners nearCorners=SquareCorners.mul(splitsCorners[0], mcvsv);
		for(int i=0;i<nsplits;i++)
		{
			SquareCorners farCorners=SquareCorners.mul(splitsCorners[i+1], mcvsv); System.out.println(farCorners.xmym);
			System.out.println(farCorners.xpym);
			System.out.println(farCorners.xmyp);
			System.out.println(farCorners.xpyp);
			
			BoundingBox bb=new BoundingBox();
			bb.adaptForSquareCorners(farCorners); System.out.println(bb.getPx()+" "+bb.getMx());
			bb.adaptForSquareCorners(nearCorners); System.out.println(bb.getPx()+" "+bb.getMx());
			bb.resetZAxis();
			System.out.println(worldCornersLow.xmym);
			bb.adaptZForSquareCorners(worldCornersLowShadow); System.out.println(bb.getPz()+" "+bb.getMz());
			bb.adaptZForSquareCorners(worldCornersHighShadow);
			System.out.println(bb.getPz()+" "+bb.getMz());
			System.out.println("______________________");
			this.splitsOrthoProjections[i]=MatrixHelper.createOrthoMatix(bb.getPx(), bb.getMx(), bb.getPy(), bb.getMy(),bb.getPz(), bb.getMz());
			Matrix4f.mul(this.splitsOrthoProjections[i],sunViewMatrix, this.splitsOrthoProjections[i]);
			
			nearCorners=farCorners;
		}
	}
	
	public Matrix4f getOrthoProjectionForSplit(int split)
	{
		if(split<0||split>=this.nsplits) return null;
		
		return this.splitsOrthoProjections[split];
	}
	public Matrix4f getOrthoProjectionForSplitScreenAdjusted(int split)
	{
		if(split<0||split>=this.nsplits) return null;
		
		Matrix4f ret=new Matrix4f();
		Matrix4f.mul(screenLocationMat, this.splitsOrthoProjections[split], ret);
		return ret;
	}
	public float[] getSplitDistances()
	{
		return this.dsplits;
	}
	public int getNumberSplits()
	{
		return this.nsplits;
	}
	/**
	 * Assuming near and far unchanged, only the focus.
	 */
	@Override
	public void onProjectionMatrixChange(Camera c) 
	{
		//Store camera projmat inv
		fillSplitsCorners(c.getProjectionMatrix());
	}
	
}
