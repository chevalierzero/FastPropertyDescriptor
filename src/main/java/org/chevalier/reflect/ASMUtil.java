package org.chevalier.reflect;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.objectweb.asm.ClassWriter.COMPUTE_MAXS;

/**
 * 
 * @author Chevalier (chevalier_zero@hotmail.com)
 */
class ASMUtil extends ClassLoader implements Opcodes {
	
	private static SimpleMap<String, String> classNames = new SimpleMap<String, String>();

	public static AccessMethod create(Class<?> clzEntity) throws Exception {

		Class<AccessMethod> clz = AccessMethod.class;
		String className = clzEntity.getSimpleName() + clz.getSimpleName() + "Impl";
		String key = className;
		
		className = classNames.get(className);
		
		if(className != null){
			
			if(className.equals(className)){
				
				className += "2";
				
			}else{
				
				int index = Integer.parseInt(className.substring(className.length() - 1));
				className = className + (++index);
			}
		}else{
			
			className = key;
		}
		
		ClassWriter cw = new ClassWriter(COMPUTE_MAXS);

		cw.visit(Opcodes.V1_7, ACC_PUBLIC + ACC_SUPER, className, null, "java/lang/Object",
				new String[] { clz.getName().replace(".", "/") });

		MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);

		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
		mv.visitInsn(RETURN);
		mv.visitMaxs(1, 1);
		mv.visitEnd();
		
		makeMethodGet(cw, "get", className, clzEntity);
		makeMethodSet(cw, "set", className, clzEntity);

		cw.visitEnd();
		
		byte[] code = cw.toByteArray();
		
		ASMUtil loader = new ASMUtil();
		Class<?> exampleClass = loader.defineClass(className, code, 0, code.length);

		Object obj = exampleClass.newInstance();
		AccessMethod asm = (AccessMethod) obj;

		classNames.put(key, className);
		
		return asm;
	}
	
	private static void makeMethodGet(ClassWriter cw, String methodName, String className, Class<?> clzEntity) {

		Field[] fields = clzEntity.getDeclaredFields();

		List<Map<String, Object>> setFields = new ArrayList<Map<String, Object>>();

		for (int i = 0; i < fields.length; i++) {

			if ((Modifier.FINAL & fields[i].getModifiers()) != 0) {
				continue;
			}

			try {

				PropertyDescriptor propertyDescriptor = new PropertyDescriptor(fields[i].getName(), clzEntity);

				Map<String, Object> setField = new HashMap<String, Object>();
				setField.put("fieldName", fields[i].getName());
				setField.put("fieldType", fields[i].getType());
				setField.put("getName", propertyDescriptor.getReadMethod().getName());

				setFields.add(setField);

			} catch (Exception e) {
			}

		}

		if (setFields.isEmpty()) {

			return;
		}

		MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, methodName,
				"(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;", null, null);
		
		mv.visitCode();
		Label lblIf = new Label();
		String entityPath = clzEntity.getName().replace(".", "/");

		for (int i = 0, size = setFields.size(); i < size; i++) {

			try {

				Map<String, Object> setField = setFields.get(i);

				String fieldName = (String) setField.get("fieldName");
				Class<?> fieldClass = (Class<?>) setField.get("fieldType");
				Class<?> fieldRefClass = castPriToRef(fieldClass);
				String fieldType = Type.getDescriptor(fieldClass);
				String fieldClassPath = Type.getInternalName(fieldClass);
				String fieldRefType = Type.getDescriptor(fieldRefClass);
				String fieldRefClassPath = Type.getInternalName(fieldRefClass);
				String getName = (String) setField.get("getName");
				Label lblElse = null;
				
				mv.visitLdcInsn(fieldName);
				mv.visitVarInsn(ASTORE, 3);
				mv.visitVarInsn(ALOAD, 3);
				mv.visitVarInsn(ALOAD, 2);

				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);

				mv.visitJumpInsn(IFEQ, lblIf);

				mv.visitVarInsn(ALOAD, 1);
				mv.visitTypeInsn(CHECKCAST, entityPath);

				mv.visitMethodInsn(INVOKEVIRTUAL, entityPath, getName, "()" + fieldType, false);

				if(fieldRefClassPath.equals(fieldClassPath) == false){

					mv.visitMethodInsn(INVOKESTATIC, fieldRefClassPath, "valueOf", "(" + fieldType + ")" + fieldRefType, false);
				}
				
				mv.visitInsn(ARETURN);
				
				if (i + 1 < size) {

					lblElse = new Label();
				}

				mv.visitLabel(lblIf);
				mv.visitFrame(F_SAME, 0, null, 0, null);

				lblIf = lblElse;

			} catch (Exception e) {}

		}

		mv.visitInsn(ACONST_NULL);
		mv.visitInsn(ARETURN);
		mv.visitMaxs(2, 2);
		mv.visitEnd();
		
	}

	private static void makeMethodSet(ClassWriter cw, String methodName, String className, Class<?> clzEntity) {

		Field[] fields = clzEntity.getDeclaredFields();

		List<Map<String, Object>> setFields = new ArrayList<Map<String, Object>>();

		for (int i = 0; i < fields.length; i++) {

			if ((Modifier.FINAL & fields[i].getModifiers()) != 0) {
				continue;
			}

			try {

				PropertyDescriptor propertyDescriptor = new PropertyDescriptor(fields[i].getName(), clzEntity);

				Map<String, Object> setField = new HashMap<String, Object>();
				setField.put("fieldName", fields[i].getName());
				setField.put("fieldType", fields[i].getType());
				setField.put("setName", propertyDescriptor.getWriteMethod().getName());

				setFields.add(setField);

			} catch (Exception e) {
			}

		}

		if (setFields.isEmpty()) {

			return;
		}

		MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, methodName,
				"(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V", null, null);
		
		mv.visitCode();
		Label lblIf = new Label();
		String entityPath = clzEntity.getName().replace(".", "/");

		for (int i = 0, size = setFields.size(); i < size; i++) {

			try {

				Map<String, Object> setField = setFields.get(i);

				String fieldName = (String) setField.get("fieldName");
				Class<?> fieldClass = (Class<?>) setField.get("fieldType");
				Class<?> fieldRefClass = castPriToRef(fieldClass);
				String fieldType = Type.getDescriptor(fieldClass);
				String fieldClassPath = Type.getInternalName(fieldClass);
				String fieldRefClassPath = Type.getInternalName(fieldRefClass);
				String setName = (String) setField.get("setName");
				Label lblElse = null;
				
				mv.visitLdcInsn(fieldName);
				mv.visitVarInsn(ASTORE, 4);
				mv.visitVarInsn(ALOAD, 4);
				mv.visitVarInsn(ALOAD, 2);

				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);

				mv.visitJumpInsn(IFEQ, lblIf);

				mv.visitVarInsn(ALOAD, 1); 
				mv.visitTypeInsn(CHECKCAST, entityPath);

				mv.visitVarInsn(ALOAD, 3);
				mv.visitTypeInsn(CHECKCAST, fieldRefClassPath);
				
				if(fieldRefClassPath.equals(fieldClassPath) == false){

					mv.visitMethodInsn(INVOKEVIRTUAL, fieldRefClassPath, fieldClassPath + "Value", "()" + fieldType, false);
				}
				
				mv.visitMethodInsn(INVOKEVIRTUAL, entityPath, setName, "(" + fieldType + ")V", false);

				if (i + 1 < size) {

					lblElse = new Label();
					mv.visitJumpInsn(GOTO, lblElse);
				}

				mv.visitLabel(lblIf);
				mv.visitFrame(F_SAME, 0, null, 0, null);

				lblIf = lblElse;

			} catch (Exception e) {}

		}

		mv.visitInsn(RETURN);
		mv.visitMaxs(2, 2);
		mv.visitEnd();
		
	}
	
	private static Class<?> castPriToRef(Class<?> clz){
		
		Class<?> refClz = clz;
		
		if(clz.isPrimitive()){

			switch (clz.getName()) {
			
				case "byte":
					refClz = Byte.class;
					break;
					
				case "char":
					refClz = Character.class;
					break;
				
				case "short":
					refClz = Short.class;
					break;
				
				case "int":
					refClz = Integer.class;
					break;
					
				case "long":
					refClz = Long.class;
					break;
					
				case "float":
					refClz = Float.class;
					break;
		
				case "double":
					refClz = Double.class;
					break;
				
				case "boolean":
					refClz = Boolean.class;
					break;
			}
		}
		
		return refClz;
	}
}
