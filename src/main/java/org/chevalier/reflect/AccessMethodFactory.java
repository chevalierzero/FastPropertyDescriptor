package org.chevalier.reflect;

import org.chevalier.reflect.util.ASMClassLoader;
import org.chevalier.reflect.util.SimpleMap;

/**
 * @author Chevalier (chevalier_zero@hotmail.com)
 */
public final class AccessMethodFactory {
	
	private final static SimpleMap<String, AccessMethod> ACCESS_METHODS = new SimpleMap<String, AccessMethod>();
	
	public static AccessMethod build(Class<?> clz) {
		
		if(clz == null){
			
			throw new NullPointerException();
		}
		
		AccessMethod accessSetMethod = ACCESS_METHODS.get(clz.getName());
		
		if(accessSetMethod == null){
			
			synchronized (clz.getSimpleName().intern()) {
				
				accessSetMethod = ACCESS_METHODS.get(clz.getName());
				
				if(accessSetMethod == null){
				
					try {
						
						accessSetMethod = ASMClassLoader.getInstance().create(clz);
						ACCESS_METHODS.put(clz.getName(), accessSetMethod);
						
					} catch (Exception e) {}
				}
			}
		}
		
		return accessSetMethod;
	}
}
