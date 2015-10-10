package monecruft.utils;

import java.util.LinkedList;

public class InputHandler 
{
	public static final int W_VALUE=0;
	public static final int A_VALUE=1;
	public static final int S_VALUE=2;
	public static final int D_VALUE=3;
	public static final int SPACE_VALUE=4;
	public static final int SHIFT_VALUE=5;
	public static final int ESC_VALUE=6;
	public static final int E_VALUE=7;
	public static final int MOUSE_BUTTON1_VALUE=8;
	public static final int MOUSE_BUTTON2_VALUE=9;
	public static final int MOUSE_WHEEL_VALUE=10;
	
	private static boolean[] keyInfo=new boolean[11];
	private static LinkedList[] listenerList=new LinkedList[11];
	static
	{
		for(int i=0;i<listenerList.length;i++)
		{
			listenerList[i]=new LinkedList<KeyToggleListener>();
		}
	}
	public static boolean isWPressed()
	{
		return keyInfo[W_VALUE];
	}
	public static boolean isAPressed()
	{
		return keyInfo[A_VALUE];
	}
	public static boolean isSPressed()
	{
		return keyInfo[S_VALUE];
	}
	public static boolean isDPressed()
	{
		return keyInfo[D_VALUE];
	}
	public static boolean isSPACEPressed()
	{
		return keyInfo[SPACE_VALUE];
	}
	public static boolean isSHIFTPressed()
	{
		return keyInfo[SHIFT_VALUE];
	}
	public static boolean isESCPressed()
	{
		return keyInfo[ESC_VALUE];
	}
	public static boolean isEPressed()
	{
		return keyInfo[E_VALUE];
	}
	public static boolean isMouseButton1Pressed()
	{
		return keyInfo[MOUSE_BUTTON1_VALUE];
	}
	public static boolean isMouseButton2Pressed()
	{
		return keyInfo[MOUSE_BUTTON2_VALUE];
	}
	public static void setW(boolean v)
	{
		setKey(W_VALUE,v);
	}
	public static void setA(boolean v)
	{
		setKey(A_VALUE,v);
	}
	public static void setS(boolean v)
	{
		setKey(S_VALUE,v);
	}
	public static void setD(boolean v)
	{
		setKey(D_VALUE,v);
	}
	public static void setSPACE(boolean v)
	{
		setKey(SPACE_VALUE,v);
	}
	public static void setSHIFT(boolean v)
	{
		setKey(SHIFT_VALUE,v);
	}
	public static void setESC(boolean v)
	{
		setKey(ESC_VALUE,v);
	}
	public static void setE(boolean v)
	{
		setKey(E_VALUE,v);
	}
	public static void setMouseButton1(boolean v)
	{
		setKey(MOUSE_BUTTON1_VALUE,v);
	}
	public static void setMouseButton2(boolean v)
	{
		setKey(MOUSE_BUTTON2_VALUE,v);
	}
	private static void setKey(int code,boolean v)
	{
		if(v&&!keyInfo[code])
		{
			for(Object l:listenerList[code])
			{
				if(!(l instanceof KeyToggleListener)) break;
				KeyToggleListener tl=(KeyToggleListener) l;
				tl.notifyKeyToggle(code);
			}
		}
		keyInfo[code]=v;
	}
	public static void addWheel(int val)
	{
		System.out.println("WHEEEL   "+val);
		for(Object l:listenerList[MOUSE_WHEEL_VALUE])
		{
			if(!(l instanceof KeyValueListener)) break;
			KeyValueListener tl=(KeyValueListener) l;
			tl.notifyKeyIncrement(MOUSE_WHEEL_VALUE,val);
		}
	}
	public static void addKeyToggleListener(int code,KeyToggleListener tl)
	{
		listenerList[code].add(tl);
	}
	public static void addKeyValueListener(int code,KeyValueListener tl)
	{
		listenerList[code].add(tl);
	}
	public static void removeKeyToggleListener(int code,KeyToggleListener tl)
	{
		listenerList[code].remove(tl);
	}
}
