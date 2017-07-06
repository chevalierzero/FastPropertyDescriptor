package org.chevalier.reflect.util;

import org.chevalier.reflect.asm.Type;

/**
 * @author Chevalier (chevalier_zero@hotmail.com)
 */
public class ASMUtils {
	
	public static String getInternalName(Class<?> clz){
		
		return Type.getInternalName(clz);
	}
	
	public static String getDescriptor(Class<?> clz){
		
		return Type.getDescriptor(clz);
	}
	
	public static Class<?> getReferenceType(Class<?> clz){
		
		if(clz.isPrimitive()){
			
			if(Integer.TYPE == clz){
				
				return Integer.class;
				
			}else if(Boolean.TYPE == clz){
				
				return Boolean.class;
				
			}else if(Long.TYPE == clz){
				
				return Long.class;
				
			}else if(Double.TYPE == clz){
				
				return Double.class;
				
			}else if(Byte.TYPE == clz){
				
				return Byte.class;
				
			}else if(Character.TYPE == clz){
				
				return Character.class;
				
			}else if(Short.TYPE == clz){
				
				return Short.class;
				
			}else if(Float.TYPE == clz){
				
				return Float.class;
			}
		}

		return clz;
	}
}
