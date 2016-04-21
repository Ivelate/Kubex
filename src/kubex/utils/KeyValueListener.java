package kubex.utils;

/**
 * This work is licensed under the Creative Commons Attribution 4.0 International License. To view a copy of this license, visit http://creativecommons.org/licenses/by/4.0/.
 * 
 * @author Víctor Arellano Vicente (Ivelate)
 * 
 * Interface implemented by objects which want to be notified if the value of some input changes
 */
public interface KeyValueListener {
	public void notifyKeyIncrement(int code,int value);
}
