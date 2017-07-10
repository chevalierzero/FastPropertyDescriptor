package org.chevalier.reflect;

public interface AccessMethod {

	public Object get(Object obj, String fieldName);

	public <T> T get(Object obj, String fieldName, Class<T> clz);

	public void set(Object obj, String fieldName, Object value);
}
