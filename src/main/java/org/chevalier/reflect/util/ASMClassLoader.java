package org.chevalier.reflect.util;

/**
 * @author Chevalier (chevalier_zero@hotmail.com)
 */
public class ASMClassLoader extends ClassLoader {

	private ASMClassLoader() { }

	public static ASMClassLoader getInstance() {

		return SingletonHolder.INSTANCE;
	}

	private final static class SingletonHolder {

		private final static ASMClassLoader INSTANCE = new ASMClassLoader();
	}
	
	public Class<?> defineClass(String className, byte[] code) throws Exception {

		return this.defineClass(className, code, 0, code.length);
	}
}
