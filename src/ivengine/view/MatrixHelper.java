package ivengine.view;

import static org.lwjgl.opengl.GL20.glUniformMatrix4;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;
import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import monecruft.gui.Chunk;
import monecruft.utils.Vector3d;

/**
 *  @author Víctor Arellano Vicente (Ivelate)
 *  
 *  Contains multiple helper methods to work with matrixes
 */
public class MatrixHelper 
{
	public static final Vector3f zAxis=new Vector3f(0,0,1);
	public static final Vector3f yAxis=new Vector3f(0,1,0);
	public static final Vector3f xAxis=new Vector3f(1,0,0);
	public static final Matrix4f identity=new Matrix4f();
	
	private static Matrix3f m3buf=new Matrix3f();
	private static Vector3f v3buf=new Vector3f();
	
	private static FloatBuffer matrix44Buffer=BufferUtils.createFloatBuffer(16);
	
	/**
	 * Creates a perspective projection matrix with a z near point <znear> , z far point <zfar> , field of view <fov> and resolution ratio <arat>
	 */
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
	
	/**
	 * Creates a orthographic projection matrix going from x <xm> to <xp> , from y <ym> to <yp> and from z <zm> to <zp>
	 */
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
		res.m32 = (zp+zm)/(zp-zm); // With the - at the beginning, it doesn't work properly. Its confusing, because all written scientific literature makes clear that this - is 
								   // neccesary, but my tests give wrong results if it's there. Anyways : It works.
		res.m33 = 1.0f;
		
		return res;
	}
	
	/**
	 * Creates both a scale and a translation matrix on the same call. Scale will be equal to <scale> (On all axis), and the translation will be <xt> , <yt> , <zt>
	 */
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
	
	/**
	 * Multiplies a matrix by a vector
	 */
	public static Vector4f multiply(Matrix4f op1,Vector4f op2)
	{
		return new Vector4f((op1.m00*op2.x)+(op1.m10*op2.y)+(op1.m20*op2.z)+(op1.m30*op2.w),
				(op1.m01*op2.x)+(op1.m11*op2.y)+(op1.m21*op2.z)+(op1.m31*op2.w),
				(op1.m02*op2.x)+(op1.m12*op2.y)+(op1.m22*op2.z)+(op1.m32*op2.w),
				(op1.m03*op2.x)+(op1.m13*op2.y)+(op1.m23*op2.z)+(op1.m33*op2.w));
	}
	
	/**
	 * Uploads a matrix <mat> to the graphics card, to the uniform with location <dest>
	 */
	public static void uploadMatrix(Matrix4f mat,int dest)
	{
		matrix44Buffer.rewind();mat.store(matrix44Buffer); matrix44Buffer.flip();
	    glUniformMatrix4(dest, false, matrix44Buffer);
	}
	
	/**
	 * Perfomance method. Reuses a matrix <mat>, so memory cost is 0: Translates that matrix <mat> using the vector - <trans>, uploads it to the graphics card
	 * and retranslates it back to its initial position.
	 * Due to some floating point operation errors, this operation can return a matrix not exactly equal to the original one. This method is fast but pottentially inaccurate, so
	 * its marked as deprecate
	 */
	@Deprecated
	public static void uploadTranslatedMatrix(Matrix4f mat,Vector3f trans,int dest)
	{
		mat.m30-=trans.x; mat.m31-=trans.y; mat.m32-=trans.z;
		uploadMatrix(mat,dest);
		mat.m30+=trans.x; mat.m31+=trans.y; mat.m32+=trans.z;
	}

	/**
	 * Perfomance method. Reuses a matrix <mat>, so memory cost is 0: Translates that matrix <mat> using the vector - <trans>, uploads it to the graphics card
	 * and retranslates it back to its initial position.
	 */
	public static void uploadTranslatedMatrix(Matrix4f mat,Vector3d trans,int dest)
	{
		float m30=mat.m30; float m31=mat.m31; float m32=mat.m32; 
		mat.m30-=trans.x; mat.m31-=trans.y; mat.m32-=trans.z;
		uploadMatrix(mat,dest);
		mat.m30=m30; mat.m31=m31; mat.m32=m32;
	}
	
	/**
	 * Creates a translation matrix on-the-fly over the matrix <mat> and uploads it.
	 * This method when centering the perspective in the player, so the translation will be equal to the
	 * distance to the center point, being that <ix> , <iy> , <iz> , and the objective point being
	 * described by the vector <trans>
	 */
	public static void uploadTranslationMatrix(Matrix4f mat,double ix,double iy,double iz,Vector3d trans,int dest)
	{
		mat.m30=(float)(ix-trans.x);
		mat.m31=(float)(iy-trans.y);
		mat.m32=(float)(iz-trans.z);
		uploadMatrix(mat,dest);
	}

	/**
	 * Uploads a vector <vec> to the graphics card (Into the uniform location <dest> ) 
	 */
	public static void uploadVector(Vector3f vec,int dest)
	{
	    GL20.glUniform3f(dest, vec.x, vec.y, vec.z);
	}
	
	/**
	 * Returns the inverted matrix of the rotation matrix contained in <mat>
	 */
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
	
	/**
	 * Fast affine inverse. Uses a static buffer, so this method is synchronized
	 */
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
