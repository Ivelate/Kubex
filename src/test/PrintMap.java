package test;

import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import monecruft.gui.MapGenerator;

public class PrintMap 
{
	private static final int ZOOM=2;
	private static final int INITIAL_X=0;
	private static final int INITIAL_Y=0;
	private static final int WATER_LEVEL=64;
	
	public static void main(String[] args) throws IOException
	{
		BufferedImage bi=new BufferedImage(1024, 1024,
			    BufferedImage.TYPE_INT_RGB);
		MapGenerator mg=new MapGenerator(0,128,0);
		for(int w=0;w<1024;w++){
			for(int h=0;h<1024;h++){
				int height=mg.getHeight((w*ZOOM)+INITIAL_X, (h*ZOOM)+INITIAL_Y);
				int rgb;
				if(height<WATER_LEVEL){
					rgb=255;
				}else{
					int gray=255-((height-WATER_LEVEL)*255/(128-WATER_LEVEL));
					rgb=(gray << 16) | (gray << 8) | gray;
				}
				bi.setRGB(w, h, rgb);
			}
		}
		File f = new File("MyFile.png");
		ImageIO.write(bi, "PNG", f);
		Desktop.getDesktop().open(f);
	}
	
}
