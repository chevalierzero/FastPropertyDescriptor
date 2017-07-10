package org.chevalier.reflect.util;

import org.chevalier.reflect.asm.Opcodes;
import org.chevalier.reflect.asm.Type;

/**
 * @author Chevalier (chevalier_zero@hotmail.com)
 */
public class ASMUtils {
	
	public static String getInternalName(Class<?> clz){
		
		return getInternalName(clz.getName());
	}
	
	public static String getInternalName(String className){
		
		return className.replace('.', '/');
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
	
	public static int getVersionOpcode(){
		
		String version = System.getProperty("java.version");
		
		if (version.startsWith("1.7")) {

			return Opcodes.V1_7;

		} else if (version.startsWith("1.8")) {

			return Opcodes.V1_8;
			
		} else if (version.startsWith("1.6")) {

			return Opcodes.V1_6;

		} else if (version.startsWith("1.5")) {

			return Opcodes.V1_5;

		} else if (version.startsWith("1.4")) {

			return Opcodes.V1_4;
		
		} else if (version.startsWith("1.3")) {

			return Opcodes.V1_3;
			
		} else if (version.startsWith("1.2")) {

			return Opcodes.V1_2;
			
		} else if (version.startsWith("1.1")) {

			return Opcodes.V1_1;
		}
		
		return Opcodes.V1_7;
	}
}