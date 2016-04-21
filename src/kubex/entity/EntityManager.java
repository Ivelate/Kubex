package kubex.entity;

import ivengine.properties.Cleanable;
import kubex.gui.WorldFacade;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Manages all entities in the scene. For now its just the player, but they could be extended more easily managing them this way
 */
public class EntityManager implements Cleanable
{
	private Player player;
	private WorldFacade wf;
	public EntityManager(Player player,WorldFacade wf)
	{
		this.player=player;
		this.wf=wf;
	}

	public void update(float tEl)
	{
		this.player.update(tEl,wf);
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
