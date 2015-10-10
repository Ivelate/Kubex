package monecruft.entity;

import monecruft.gui.WorldFacade;
import ivengine.view.Camera;

public class SuperPlayer extends Player {

	private Camera cam;
	public SuperPlayer(float ix, float iy, float iz, Camera cam) {
		super(ix, iy, iz, cam);
		// TODO Auto-generated constructor stub
		this.cam=cam;
	}
	@Override
	public void moveForward(float amout,WorldFacade wf)
	{
		cam.moveX(1);
	}
	@Override
	public void moveLateral(float amout,WorldFacade wf)
	{
		
	}
	

}
