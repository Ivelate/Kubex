package monecruft.gui;

import java.awt.Font;

import org.newdawn.slick.Color;
import org.newdawn.slick.TrueTypeFont;

public class GlobalTextManager 
{
	private static GlobalTextManager instance=null;
	
	private static float DEFAULT_FADE_TIME=1f;
	
	private String text=null;
	private float text_time=0;
	private float text_fade_time=0;
	private TrueTypeFont font;
	
	public GlobalTextManager()
	{
		instance=this;
		
		//Load fonts
		Font awtFont = new Font("Arial", Font.PLAIN, 22); //name, style (PLAIN, BOLD, or ITALIC), size
		font = new TrueTypeFont(awtFont, true); //base Font, anti-aliasing true/false
	}
	public void update(float tEl)
	{
		if(text!=null){
			if(text_time<0){
				text_fade_time-=tEl;
				if(text_fade_time<0) text=null;
			}
			else if(text_fade_time<DEFAULT_FADE_TIME){
				text_fade_time+=tEl;
			}
			else{
				text_time-=tEl;
				if(text_time<0) text_fade_time=DEFAULT_FADE_TIME;
			}
		}
	}
	public void draw(int width,int height)
	{
		//Color.white.bind();
		if(text!=null) {
			Color c=text_fade_time<0?new Color(1f,1f,1f,0.8f):new Color(1f,1f,1f,(text_fade_time/DEFAULT_FADE_TIME)*0.8f);
			font.drawString((width-font.getWidth(text))/2, height*3/4, text, c); //x, y, string to draw, color
		}
	}
	
	private void insertTextConcrete(String text,float time,boolean initialFade)
	{
		this.text=text;
		this.text_time=time;
		this.text_fade_time=initialFade?0:DEFAULT_FADE_TIME;
	}
	public static void insertText(String text)
	{
		getInstance().insertTextConcrete(text,2f,true);
	}
	public static void insertTextInsta(String text)
	{
		getInstance().insertTextConcrete(text,2f,false);
	}
	private static GlobalTextManager getInstance()
	{
		if(instance==null) instance=new GlobalTextManager();
		
		return instance;
	}
}
