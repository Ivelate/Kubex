package monecruft.gui;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL20.glUniform1f;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glDepthFunc;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glBufferSubData;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glGetAttribLocation;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;

import java.nio.FloatBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import ivengine.view.Camera;
import ivengine.view.MatrixHelper;
import monecruft.shaders.BasicColorShaderProgram;
import monecruft.shaders.DeferredShaderProgram;
import monecruft.shaders.SkyShaderProgram;
import monecruft.storage.FloatBufferPool;
import monecruft.utils.SquareCorners;

public class Sky 
{
	private static final double DEFAULT_TIMEZONE=1;
	private static final double DEFAULT_LATITUDE=0;
	private static final double DEFAULT_LONGITUDE=-1.630753;
	private static final int DEFAULT_YEAR = 2015;
	private static final int DEFAULT_MONTH = 8;
	private static final int DEFAULT_DAY = 25;
	private static final double DEFAULT_TURBIDITY = 2.7f;
	
	private double julianDay=getJulianDayNumber(DEFAULT_YEAR,DEFAULT_MONTH,DEFAULT_DAY,0,0,0,DEFAULT_TIMEZONE);
	private double solarDeclination=getSolarDeclination(julianDay);
	private PerezYxyCoefficients coeffs = getPerezCoefficientsForTurbidity(DEFAULT_TURBIDITY);
	private double currentTime=0;
	private double solarAltitude=getSolarAltitude(solarDeclination, getSolarTime(julianDay, currentTime, DEFAULT_TIMEZONE, DEFAULT_LONGITUDE), DEFAULT_LATITUDE);
	private double solarAzimuth=getSolarAzimuth(solarDeclination, getSolarTime(julianDay, currentTime, DEFAULT_TIMEZONE, DEFAULT_LONGITUDE), DEFAULT_LATITUDE);
	
	private Camera cam;
	private Camera sunCamera;

	
	public Sky(Camera cam,Camera sunCamera)
	{
		this.cam=cam;
		this.sunCamera=sunCamera;
	}
		public void uploadToShader(DeferredShaderProgram DSP)
		{
			double zenithS = (Math.PI / 2.) - solarAltitude;
			YyxColor sunYyx = getYyxColorForZenithAndTurbidity(zenithS, DEFAULT_TURBIDITY);

			sunYyx.uploadToShader(DSP);
			this.coeffs.uploadToShader(DSP);
			glUniform1f(glGetUniformLocation(DSP.getID(),"solar_zenith"),(float)zenithS);
			glUniform1f(glGetUniformLocation(DSP.getID(),"solar_azimuth"),(float)solarAzimuth);

		}
		/*public void drawBackgroundColor(float r,float g,float b)
		{
			this.BCSP.enable();
			glDisable( GL11.GL_BLEND );
			glEnable(GL11.GL_CULL_FACE);
			glEnable(GL11.GL_DEPTH_TEST);
			glDepthFunc(GL11.GL_EQUAL);
			glBindBuffer(GL15.GL_ARRAY_BUFFER,this.vbo);
			this.BCSP.setupAttributes();
			int loc = glGetUniformLocation(BCSP.getID(), "color.r");
			glUniform1f(loc, r);
			loc = glGetUniformLocation(BCSP.getID(), "color.g");
			glUniform1f(loc, g);
			loc = glGetUniformLocation(BCSP.getID(), "color.b");
			glUniform1f(loc, b);

			glDrawArrays(GL_TRIANGLES, 0, NUM_COMPONENTS);
			glBindBuffer(GL15.GL_ARRAY_BUFFER,0);
			glDepthFunc(GL11.GL_LEQUAL);
			this.BCSP.disable();
		}*/
		public void setCurrentTime(float currentTime)
		{
			this.currentTime=currentTime;
		}
		public void update(float tEl)
		{
			this.currentTime+=(tEl);
			if(this.currentTime>21) this.currentTime=4;
			if(this.currentTime>24) this.currentTime=0;
			
			this.sunCamera.setPitch((float)this.solarAltitude);
			this.sunCamera.setYaw(-(float)(this.solarAzimuth));
			
			double solarTime = getSolarTime(julianDay, currentTime, DEFAULT_TIMEZONE, DEFAULT_LONGITUDE);
			this.solarAltitude = getSolarAltitude(solarDeclination, solarTime, DEFAULT_LATITUDE);
			this.solarAzimuth = getSolarAzimuth(solarDeclination, solarTime, DEFAULT_LATITUDE);
		}
		public double getSolarAltitude()
		{
			return this.solarAltitude;
		}
		public Vector3f getSunNormal()
		{
			Vector3f res=new Vector3f();
			res.y=(float)Math.sin(this.solarAltitude);
			double dim=Math.cos(this.solarAltitude);
			res.x=(float)(dim*Math.sin(this.solarAzimuth));
			res.z=(float)(dim*Math.cos(this.solarAzimuth));
			
			return res;
		}

	/** SUN THINGS **/
		
		private static double getJulianDayNumber(int year, int month, int day, int hour, int minute, int second, double timezone) {
		    double dayDecimal, julianDay, a, b;
		   
		    double timeDecimal = ((hour - timezone) / 24.) + (minute / (60. * 24.)) + (second / (60. * 60. * 24.));
		    dayDecimal = day + timeDecimal;
		   
		    if (month < 3) {
		        month += 12;
		        year--;
		    }
		   
		    a = (int)Math.floor(year / 100.);
		    b = 2 - a + Math.floor(a / 4.);
		   
		    julianDay = (int)Math.floor(365.25 * (year + 4716.)) + (int)Math.floor(30.6001 * (month + 1)) + dayDecimal + b - 1524.5;
		 
		    return julianDay;
		}
		 
		 
		private static double getDecimalTime(int hour, int minute, int second) {
		    return hour + ((double)minute / 60.) + ((double)second / 3600.);
		}
		 
		 
		private static double getSolarTime(double julianDayNumber, double decimalTime, double timezone, double longitude) {
		    return decimalTime + .17f*Math.sin(4.*Math.PI*((julianDayNumber - 80) / 373)) - .129f*Math.sin(2.*Math.PI*((julianDayNumber - 8) / 355.)) - ((15*timezone - longitude) / 15.);
		}
		 
		 
		private static double getSolarDeclination(double julianDayNumber) {
		    return .4093f * Math.sin((2*Math.PI * (julianDayNumber - 81)) / 368.);
		}
		 
		 
		private static double getSolarAltitude(double solarDeclination, double solarTime, double latitude) {
		    return Math.asin(Math.sin(Math.toRadians(latitude)) * Math.sin(solarDeclination) - Math.cos (Math.toRadians(latitude)) * Math.cos (solarDeclination) * Math.cos (Math.PI * solarTime / 12.));
		}
		 
		 
		private static double getSolarAzimuth(double solarDeclination, double solarTime, double latitude) {
		    return Math.atan2((-Math.cos(solarDeclination) * Math.sin(Math.PI * solarTime / 12.)), (Math.cos(Math.toRadians(latitude)) * Math.sin(solarDeclination) - Math.sin(Math.toRadians(latitude)) * Math.cos(solarDeclination) * Math.cos(Math.PI * solarTime / 12.)));
		}
		 
		 
		private static PerezYxyCoefficients getPerezCoefficientsForTurbidity(double turbidity) {
			PerezYxyCoefficients coeff=new PerezYxyCoefficients();
		   
		    coeff.YA = .17872f * turbidity - 1.46303f;
		    coeff.YB = -.3554f * turbidity + .42749f;
		    coeff.YC = -.02266f * turbidity + 5.32505f;
		    coeff.YD = .12064f * turbidity - 2.57705f;
		    coeff.YE = -.06696f * turbidity + .37027f;
		   
		    coeff.xA = -.01925f * turbidity - .25922f;
		    coeff.xB = -.06651f * turbidity + .00081f;
		    coeff.xC = -.00041f * turbidity + .21247f;
		    coeff.xD = -.06409f * turbidity - .89887f;
		    coeff.xE = -.00325f * turbidity + .04517f;
		   
		    coeff.yA = -.01669f * turbidity - .26078f;
		    coeff.yB = -.09495f * turbidity + .00921f;
		    coeff.yC = -.00792f * turbidity + .21023f;
		    coeff.yD = -.04405f * turbidity - 1.65369f;
		    coeff.yE = -.01092f * turbidity + .05291f;
		   
		    return coeff;
		}
		 
		 
		private static YyxColor getYyxColorForZenithAndTurbidity(double zenith, double turbidity) {
		    YyxColor color=new YyxColor();
		    double zenith2 = Math.pow(zenith, 2);
		    double zenith3 = Math.pow(zenith, 3);
		    double turbidity2 = Math.pow(turbidity, 2);
		 
		    color.Y = ((4.0453f * turbidity - 4.971f) * Math.tan((4.f/9.f - turbidity / 120.f) * (Math.PI - 2*zenith)) - 0.2155f * turbidity + 2.4192f);
		    double Y0 = (4.0453 * turbidity - 4.971f) * Math.tan((4.0 / 9 - turbidity / 120f) * (Math.PI)) - 0.2155 * turbidity + 2.4192; 
		    color.Y=color.Y/Y0;
		    color.x = (.00166f * zenith3 - .00375f * zenith2 + .00209f * zenith + 0.f) * turbidity2 + (-0.02903f * zenith3 + .06377f * zenith2 - .03202f * zenith + .00394f) * turbidity + (.11693f * zenith3 - .21196f * zenith2 + .06052f * zenith + .25886f);
		    color.y = (.00275f * zenith3 - .0061f * zenith2 + .00317f * zenith + 0.f) * turbidity2 + (-0.04214f * zenith3 + .0897f * zenith2 - .04153f * zenith + .00516f) * turbidity + (.15346f * zenith3 - .26756f * zenith2 + .0667f * zenith + .26688f);
		   
		    return color;
		}
		 
		private static class YyxColor
		{
			public double y;
			public double x;
			public double Y;
			public void uploadToShader(DeferredShaderProgram DSP)
			{
				int loc = glGetUniformLocation(DSP.getID(), "zenitalAbs.x");
				glUniform1f(loc, (float)x);
				loc = glGetUniformLocation(DSP.getID(), "zenitalAbs.y");
				glUniform1f(loc, (float)y);
				loc = glGetUniformLocation(DSP.getID(), "zenitalAbs.Y");
				glUniform1f(loc, (float)Y);
			}
		}
		private static class PerezYxyCoefficients
		{
			public double YA;
			public double YB;
			public double YC;
			public double YD;
			public double YE;
			
			public double xA;
			public double xB;
			public double xC;
			public double xD;
			public double xE;
			
			public double yA;
			public double yB;
			public double yC;
			public double yD;
			public double yE;
			
			public void uploadToShader(DeferredShaderProgram DSP)
			{
				int loc = glGetUniformLocation(DSP.getID(), "coeff.x.A");
				glUniform1f(loc, (float)xA);
				loc = glGetUniformLocation(DSP.getID(), "coeff.x.B");
				glUniform1f(loc, (float)xB);
				loc = glGetUniformLocation(DSP.getID(), "coeff.x.C");
				glUniform1f(loc, (float)xC);
				loc = glGetUniformLocation(DSP.getID(), "coeff.x.D");
				glUniform1f(loc, (float)xD);
				loc = glGetUniformLocation(DSP.getID(), "coeff.x.E");
				glUniform1f(loc, (float)xE);

				loc = glGetUniformLocation(DSP.getID(), "coeff.y.A");
				glUniform1f(loc, (float)yA);
				loc = glGetUniformLocation(DSP.getID(), "coeff.y.B");
				glUniform1f(loc, (float)yB);
				loc = glGetUniformLocation(DSP.getID(), "coeff.y.C");
				glUniform1f(loc, (float)yC);
				loc = glGetUniformLocation(DSP.getID(), "coeff.y.D");
				glUniform1f(loc, (float)yD);
				loc = glGetUniformLocation(DSP.getID(), "coeff.y.E");
				glUniform1f(loc, (float)yE);
				
				loc = glGetUniformLocation(DSP.getID(), "coeff.Y.A");
				glUniform1f(loc, (float)YA);
				loc = glGetUniformLocation(DSP.getID(), "coeff.Y.B");
				glUniform1f(loc, (float)YB);
				loc = glGetUniformLocation(DSP.getID(), "coeff.Y.C");
				glUniform1f(loc, (float)YC);
				loc = glGetUniformLocation(DSP.getID(), "coeff.Y.D");
				glUniform1f(loc, (float)YD);
				loc = glGetUniformLocation(DSP.getID(), "coeff.Y.E");
				glUniform1f(loc, (float)YE);
			}
		}
}
