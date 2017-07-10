package org.chevalier.reflect;

/**
 * @author Chevalier (chevalier_zero@hotmail.com)
 */
public abstract class AccessMethod {

	public abstract Object get(Object obj, String fieldName);

	public <T> T get(Object obj, String fieldName, Class<T> clz){
		
		Object value = get(obj, fieldName);
		
		return (value != null) ? clz.cast(value) : null;
	}

	public abstract void set(Object obj, String fieldName, Object value);
}
