package monecruft.entity;

import ivengine.properties.Cleanable;
import monecruft.gui.WorldFacade;
import monecruftProperties.DrawableUpdatable;

public class EntityManager implements DrawableUpdatable,Cleanable
{
	private Player player;
	private WorldFacade wf;
	public EntityManager(Player player,WorldFacade wf)
	{
		this.player=player;
		this.wf=wf;
	}
	@Override
	public void update(float tEl)
	{
		this.player.update(tEl,wf);
	}
	@Override
	public void draw()
	{
		
	}
	public Player getPlayer()
	{
		return this.player;
	}
	@Override
	public void fullClean()
	{
		this.player=null;
		this.wf=null;
	}
}
