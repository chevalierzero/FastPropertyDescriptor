package org.chevalier.reflect;

/**
 * @author Chevalier (chevalier_zero@hotmail.com)
 */
public interface AccessMethod {

	public Object get(Object obj, String fieldName);
	
	public void set(Object obj, String fieldName, Object value);
}
