package kubex.gui;

import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import ivengine.view.Camera;
import ivengine.view.CameraStateListener;
import ivengine.view.MatrixHelper;
import kubex.utils.BoundaryChecker;
import kubex.utils.BoundingBox;
import kubex.utils.BoundingBoxBoundaryChecker;
import kubex.utils.SquareCorners;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Shadows manager class. Calculates the frustrum division and, for each frame, each ortho matrix associated with its corresponding frustrum division.
 */
public class ShadowsManager implements CameraStateListener
{
	private static final Matrix4f screenLocationMat=MatrixHelper.createScaleTranslationMatrix(0.5f, 0.5f, 0.5f, 0.5f);
	private final int nsplits;
	private final float[] dsplits;
	private final SquareCorners[] splitsCorners;
	
	private Matrix4f[] splitsOrthoProjections;
	private BoundingBox[] splitsBoundingBoxes;
	private Matrix4f savedSunViewMatrix=null;
	
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
		this.splitsBoundingBoxes=new BoundingBox[this.nsplits];
		for(int i=0;i<this.splitsOrthoProjections.length;i++) this.splitsOrthoProjections[i]=new Matrix4f();
		
		//Store camera projmat inv
		fillSplitsCorners(standardCam.getProjectionMatrix());
	}
	
	/**
	 * Calculates the frustrum split distances
	 */
	public static float[] calculateSplits(float near,float far,int splits,float lambda)
	{
		float[] dsplits=new float[splits+1];
		
		float fsplits=splits;
		
		//From http.developer.nvidia.com/GPUGems3/gpugems3_ch10.html
		for(int s=0;s<splits;s++)
		{
			float cuni=near+ (far-near)*(s/fsplits);
			float clog=(float)(near*Math.pow(far/near,s/fsplits));
			dsplits[s]=lambda*cuni + (1-lambda)*clog;
		}
		dsplits[splits]=far;	
		
		return dsplits;
	}
	
	/**
	 * For each split distance stores its corners coordinates
	 */
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
		
		Matrix4f pi=Matrix4f.invert(projMat, null);
		for(int i=0;i<this.splitsCorners.length;i++)
		{
			splitsCorners[i]=new SquareCorners(	Matrix4f.transform(pi, new Vector4f(-1,-1,depthSplits[i],1), null),
												Matrix4f.transform(pi, new Vector4f(1,-1,depthSplits[i],1), null),
												Matrix4f.transform(pi, new Vector4f(-1,1,depthSplits[i],1), null),
												Matrix4f.transform(pi, new Vector4f(1,1,depthSplits[i],1), null));
		}
	}
	
	/**
	 * Calculates the ortho matrix for each split of the frustrum
	 */
	public void calculateCascadeShadows(Matrix4f sunViewMatrix,SquareCorners worldCornersLow,SquareCorners worldCornersHigh)
	{
		this.savedSunViewMatrix=sunViewMatrix;
		
		//Get inverse of view mat multiplied by sun view matrix, so M(cv->sv)
		Matrix4f mcvsv=MatrixHelper.getAffineInverse(this.camera.getViewMatrix(),null);
		Matrix4f.mul(sunViewMatrix, mcvsv, mcvsv);
		
		SquareCorners worldCornersLowShadow=SquareCorners.mul(worldCornersLow, sunViewMatrix); //Gets the world space world low coordinates (If the world were envolved on a bounding box, the y- coordinates of those box)
																							   //and transforms them to camera space, from the sun point of view
		SquareCorners worldCornersHighShadow=SquareCorners.mul(worldCornersHigh, sunViewMatrix); //Gets the world space world top coordinates and transforms them to camera space 
		
		SquareCorners nearCorners=SquareCorners.mul(splitsCorners[0], mcvsv); //Near plane frustrum points to world space

		for(int i=0;i<nsplits;i++)
		{
			SquareCorners farCorners=SquareCorners.mul(splitsCorners[i+1], mcvsv); //Next split frustrum points to world space
			
			//Having both near and far corners of the split, we can construct the bounding box
			BoundingBox bb=new BoundingBox();
			bb.adaptForSquareCorners(farCorners); 
			bb.adaptForSquareCorners(nearCorners); 

			bb.adaptZForSquareCorners(worldCornersLowShadow); //The Z coordinate of the box has to envolve all possible world in wich light can pass, or some possible occludes will be left out
			bb.adaptZForSquareCorners(worldCornersHighShadow);

			this.splitsOrthoProjections[i]=MatrixHelper.createOrthoMatix(bb.getPx(), bb.getMx(), bb.getPy(), bb.getMy(),bb.getPz(), bb.getMz()); //Creates an ortho matrix using this bounding box

			Matrix4f.mul(this.splitsOrthoProjections[i],sunViewMatrix, this.splitsOrthoProjections[i]); //Creates the final view projection matrix for the split
			
			this.splitsBoundingBoxes[i]=bb;
			nearCorners=farCorners;
		}
	}
	
	/**
	 * Gets the ortho projection matrix for the split <split>
	 */
	public Matrix4f getOrthoProjectionForSplit(int split)
	{
		if(split<0||split>=this.nsplits) return null;
		
		return this.splitsOrthoProjections[split];
	}
	
	/**
	 * Gets the ortho projection matrix for the split <split> adjusted to coordinates between 0 and 1. Used to fetch from a texture instead as fetching from the screen.
	 */
	public Matrix4f getOrthoProjectionForSplitScreenAdjusted(int split)
	{
		if(split<0||split>=this.nsplits) return null;
		
		Matrix4f ret=new Matrix4f();
		Matrix4f.mul(screenLocationMat, this.splitsOrthoProjections[split], ret);
		return ret;
	}
	
	/**
	 * Gets the bounding box used in the split <split>
	 */
	public BoundaryChecker getBoundaryCheckerForSplit(int split)
	{
		return new BoundingBoxBoundaryChecker(this.splitsBoundingBoxes[split], savedSunViewMatrix);
	}
	
	/**
	 * Gets the split distances
	 */
	public float[] getSplitDistances()
	{
		return this.dsplits;
	}
	
	/**
	 * Gets the number of the splits
	 */
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
