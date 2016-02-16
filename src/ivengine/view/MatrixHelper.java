package ivengine.view;

import static org.lwjgl.opengl.GL20.glUniformMatrix4;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

public class MatrixHelper 
{
	public static final Vector3f zAxis=new Vector3f(0,0,1);
	public static final Vector3f yAxis=new Vector3f(0,1,0);
	public static final Vector3f xAxis=new Vector3f(1,0,0);
	public static final Matrix4f identity=new Matrix4f();
	
	private static Matrix3f m3buf=new Matrix3f();
	private static Vector3f v3buf=new Vector3f();
	
	private static FloatBuffer matrix44Buffer=BufferUtils.createFloatBuffer(16);
	
	public static Matrix4f createProjectionMatrix(float znear,float zfar,float fov,float arat)
	{
		// Setup projection matrix
		Matrix4f projectionMatrix = new Matrix4f();

		float y_scale = coTangent(degreesToRadians(fov / 2f));
		float x_scale = y_scale / arat;
		float frustum_length = zfar - znear;

		projectionMatrix.m00 = x_scale;
		projectionMatrix.m11 = y_scale;
		projectionMatrix.m22 = -((zfar + znear) / frustum_length);
		projectionMatrix.m23 = -1;
		projectionMatrix.m32 = -((2 * znear * zfar) / frustum_length);
		projectionMatrix.m33 = 0;
		
		return projectionMatrix;
	}
	public static Matrix4f createOrthoMatix(float xp,float xm,float yp,float ym,float zp,float zm)
	{
		Matrix4f res=new Matrix4f();
		res.m00 = 2.0f/(xp-xm);
		res.m01 = 0.0f;
		res.m02 = 0.0f;
		res.m03 = 0.0f;

		res.m10 = 0.0f;
		res.m11 = 2.0f/(yp-ym);
		res.m12 = 0.0f;
		res.m13 = 0.0f;

		res.m20 = 0.0f;
		res.m21 = 0.0f;
		res.m22 = -2.0f/(zp-zm);
		res.m23 = 0.0f;

		res.m30 = -(xp+xm)/(xp-xm);
		res.m31 = -(yp+ym)/(yp-ym);
		res.m32 = (zp+zm)/(zp-zm); //|TODO wtf
		res.m33 = 1.0f;
		
		System.out.println(res);
		
		return res;
	}
	/*static
	{

		/*Matrix4f or=createOrthoMatix(114.68283f, -122.138916f,
129.2872f, -107.17637f,
-223.08084f, -294.14893f);
		System.out.println(Matrix4f.transform(or, new Vector4f(0,-26.04f,-259.6099f,1),null));System.exit(0);*/
		/*Matrix4f or=createOrthoMatix(10,-10,10,-10,10,-10);
		System.out.println(Matrix4f.transform(or, new Vector4f(0,0,-7,1),null));System.exit(0);
		
	}*/
	public static Matrix4f createScaleTranslationMatrix(float scale,float xt,float yt,float zt)
	{
		Matrix4f res=new Matrix4f();
		res.m00 = scale;
		res.m01 = 0.0f;
		res.m02 = 0.0f;
		res.m03 = 0.0f;

		res.m10 = 0.0f;
		res.m11 = scale;
		res.m12 = 0.0f;
		res.m13 = 0.0f;

		res.m20 = 0.0f;
		res.m21 = 0.0f;
		res.m22 = scale;
		res.m23 = 0.0f;

		res.m30 = xt;
		res.m31 = yt;
		res.m32 = zt;
		res.m33 = 1.0f;
		
		return res;
	}
	public static Vector4f multiply(Matrix4f op1,Vector4f op2)
	{
		return new Vector4f((op1.m00*op2.x)+(op1.m10*op2.y)+(op1.m20*op2.z)+(op1.m30*op2.w),
				(op1.m01*op2.x)+(op1.m11*op2.y)+(op1.m21*op2.z)+(op1.m31*op2.w),
				(op1.m02*op2.x)+(op1.m12*op2.y)+(op1.m22*op2.z)+(op1.m32*op2.w),
				(op1.m03*op2.x)+(op1.m13*op2.y)+(op1.m23*op2.z)+(op1.m33*op2.w));
	}
	public static void uploadMatrix(Matrix4f mat,int dest)
	{
		matrix44Buffer.rewind();mat.store(matrix44Buffer); matrix44Buffer.flip();
	    glUniformMatrix4(dest, false, matrix44Buffer);
	}
	public static void uploadTranslatedMatrix(Matrix4f mat,Vector3f trans,int dest)
	{
		mat.m30-=trans.x; mat.m31-=trans.y; mat.m32-=trans.z;
		uploadMatrix(mat,dest);
		mat.m30+=trans.x; mat.m31+=trans.y; mat.m32+=trans.z;
	}
	public static void uploadVector(Vector3f vec,int dest)
	{
	    GL20.glUniform3f(dest, vec.x, vec.y, vec.z);
	}
	public static Matrix4f getRotationAndInvert(Matrix4f mat)
	{
		Matrix4f toRet=new Matrix4f(mat);
		toRet.m30=0;
		toRet.m31=0;
		toRet.m32=0;
		Matrix4f.invert(toRet, toRet);
		return toRet;
	}
	private static float coTangent(float angle) {
		return (float)(1f / Math.tan(angle));
	}
	
	private static float degreesToRadians(float degrees) {
		return degrees * (float)(Math.PI / 180d);
	}
	
	//Well, this uses a static buffer, so synchronized
	public static synchronized Matrix4f getAffineInverse(Matrix4f m,Matrix4f dest)
	{
		m3buf.m00=m.m00; m3buf.m01=m.m01; m3buf.m02=m.m02;
		m3buf.m10=m.m10; m3buf.m11=m.m11; m3buf.m12=m.m12;
		m3buf.m20=m.m20; m3buf.m21=m.m21; m3buf.m22=m.m22;
		
		Matrix3f.invert(m3buf, m3buf);
		
		v3buf.x=m.m30;
		v3buf.y=m.m31;
		v3buf.z=m.m32;
		
		Matrix4f d=dest==null?new Matrix4f():dest;
		
		d.m00=m3buf.m00; d.m01=m3buf.m01; d.m02=m3buf.m02;
		d.m10=m3buf.m10; d.m11=m3buf.m11; d.m12=m3buf.m12;
		d.m20=m3buf.m20; d.m21=m3buf.m21; d.m22=m3buf.m22;
		
		Matrix3f.transform(m3buf, v3buf, v3buf);
		
		d.m30=-v3buf.x; 
		d.m31=-v3buf.y;
		d.m32=-v3buf.z;
		
		d.m03=0; d.m13=0; d.m23=0; d.m33=1;
		
		return d;
	}
}
