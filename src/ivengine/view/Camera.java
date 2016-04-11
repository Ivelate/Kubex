package ivengine.view;

import java.util.LinkedList;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

/**
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * @since 8-12-2013 17:00
 * 
 * Camera used to handle projection matrix and view matrix and simplify interactions with them
 * Contains helper methods to handle rotation and movement, applying this changes directly to the view matrix.
 *
 */
public class Camera
{
	private Matrix4f projMat;
	private Matrix4f viewMat;
	
	private Matrix4f projViewMat;
	
	protected Vector3f pos=new Vector3f(0,0,0);
	
	protected float pitch=0;
	protected float yaw=0;
	protected float roll=0;
	
	protected boolean viewChanged=true;
	
	private LinkedList<CameraStateListener> stateChangeListeners=new LinkedList<CameraStateListener>(); //List of objects notified if the projection matrix of this camera changes
	
	
	public Camera(float znear,float zfar,float fov,float arat)
	{
		//Create perspective matrix from this data
		this(MatrixHelper.createProjectionMatrix(znear, zfar, fov, arat));
	}
	public Camera(Matrix4f projMat)
	{
		this.projMat=projMat;
		this.viewMat=new Matrix4f();
		
		this.projViewMat=new Matrix4f();
		Matrix4f.mul(this.projMat, this.viewMat, this.projViewMat);
	}
	
	/**
	 * Updates view matrix (And sets changed to false)
	 */
	private void updateViewMatrix()
	{
		this.viewMat=new Matrix4f();
		Matrix4f.rotate(this.roll, MatrixHelper.zAxis, 
				this.viewMat, this.viewMat);
		Matrix4f.rotate(this.pitch, MatrixHelper.xAxis, 
				this.viewMat, this.viewMat);
		Matrix4f.rotate(this.yaw, MatrixHelper.yAxis, 
				this.viewMat, this.viewMat);
		Matrix4f.translate(this.pos, this.viewMat, this.viewMat);
		
		//Update projView
		this.projViewMat=new Matrix4f();
		Matrix4f.mul(this.projMat, this.viewMat, this.projViewMat);
		
		this.viewChanged=false;
	}
	
	/**
	 * Updates projection matrix
	 */
	public void updateProjection(float znear,float zfar,float fov,float arat)
	{
		this.projMat=MatrixHelper.createProjectionMatrix(znear, zfar, fov, arat);
		//Update projView
		this.projViewMat=new Matrix4f();
		Matrix4f.mul(this.projMat, this.viewMat, this.projViewMat);
	}
	public void updateProjection(Matrix4f mat)
	{
		this.projMat=mat;
		//Notify listeners
		for(CameraStateListener csl:this.stateChangeListeners) csl.onProjectionMatrixChange(this);
		//Update projView
		this.projViewMat=new Matrix4f();
		Matrix4f.mul(this.projMat, this.viewMat, this.projViewMat);
	}
	
	/**
	 * Moves cam to position.
	 */
	public void moveTo(float x,float y,float z)
	{
		if(-x!=this.pos.x||-y!=this.pos.y||-z!=this.pos.z){
			this.pos.x=-x;
			this.pos.y=-y;
			this.pos.z=-z;
			this.viewChanged=true;
		}
	}
	
	public void moveX(float x)
	{
		this.pos.x-=x;
		this.viewChanged=true;
	}
	public void moveY(float y)
	{
		this.pos.y-=y;
		this.viewChanged=true;
	}
	public void moveZ(float z)
	{
		this.pos.z-=z;
		this.viewChanged=true;
	}
	public void addPitch(float av)
	{
		this.pitch+=av;
		this.viewChanged=true;
	}
	public void addYaw(float av)
	{
		this.yaw+=av;
		this.viewChanged=true;
	}
	public void addRoll(float av)
	{
		this.roll+=av;
		this.viewChanged=true;
	}
	public void setPitch(float np)
	{
		if(np!=this.pitch){
			this.pitch=np;
			this.viewChanged=true;
		}
	}
	public void setYaw(float ny)
	{
		if(ny!=this.yaw){
			this.yaw=ny;
			this.viewChanged=true;
		}
	}
	public void setRoll(float nr)
	{
		if(nr!=this.roll){
			this.roll=nr;
			this.viewChanged=true;
		}
	}
	
	public void addCameraStateListener(CameraStateListener csl){
		this.stateChangeListeners.add(csl);
	}
	/*					GETTERS					*/
	
	public float getPitch() {
		return pitch;
	}
	public float getYaw() {
		return yaw;
	}
	public float getRoll() {
		return roll;
	}
	public float getX(){
		return this.pos.x;
	}
	public float getY(){
		return this.pos.y;
	}
	public float getZ(){
		return this.pos.z;
	}
	public Matrix4f getViewMatrix()
	{
		if(this.viewChanged) updateViewMatrix();
		return this.viewMat;
	}
	public Matrix4f getProjectionMatrix()
	{
		return this.projMat;
	}
	public Matrix4f getProjectionViewMatrix()
	{
		if(this.viewChanged) updateViewMatrix();
		return this.projViewMat;
	}
}
