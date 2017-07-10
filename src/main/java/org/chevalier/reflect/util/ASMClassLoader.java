package org.chevalier.reflect.util;

import org.chevalier.reflect.AccessMethod;
import org.chevalier.reflect.asm.ClassWriter;
import org.chevalier.reflect.asm.Label;
import org.chevalier.reflect.asm.MethodWriter;
import org.chevalier.reflect.asm.Opcodes;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Chevalier (chevalier_zero@hotmail.com)
 */
public class ASMClassLoader extends ClassLoader implements Opcodes {

	private final static int VERSION_OPCODE = ASMUtils.getVersionOpcode();
	
	private final SimpleMap<String, String> classNames = new SimpleMap<String, String>();

	private ASMClassLoader() {
	}

	public static ASMClassLoader getInstance() {

		return SingletonHolder.INSTANCE;
	}

	private final static class SingletonHolder {

		private final static ASMClassLoader INSTANCE = new ASMClassLoader();
	}

	public AccessMethod create(Class<?> clzEntity) throws Exception {

		String className = getClassName(clzEntity);

		ClassWriter cw = new ClassWriter();
		cw.visit(VERSION_OPCODE, ACC_PUBLIC + ACC_SUPER, className, null, "java/lang/Object",
				new String[] { ASMUtils.getInternalName(AccessMethod.class) });

		MethodWriter mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
		mv.visitInsn(RETURN);
		mv.visitMaxs(1, 1);
		mv.visitEnd();

		makeMethodGet(cw, "get", className, clzEntity);
		makeMethodSet(cw, "set", className, clzEntity);

		cw.visitEnd();

		byte[] code = cw.toByteArray();

		Class<?> exampleClass = this.defineClass(className, code, 0, code.length);

		Object obj = exampleClass.newInstance();
		AccessMethod asm = (AccessMethod) obj;

		return asm;
	}

	/**
	 * 构造类名称，如果类名重复则在结尾加上一个递增的数字
	 * 
	 * @param key
	 * @return
	 */
	private String getClassName(Class<?> clzEntity) {

		String key = clzEntity.getSimpleName() + AccessMethod.class.getSimpleName() + "Impl";
		String className = classNames.get(key);

		if (className != null) {

			if (className.equals(key)) {

				className += "2";

			} else {

				// 获取类名后面的数字，继续累加
				int index = Integer.parseInt(className.substring(key.length()));

				if (index >= Integer.MAX_VALUE) {

					throw new Error("too many classes with the same name");
				}

				className = key + (++index);
			}
		} else {

			className = key;
		}

		classNames.put(key, className);

		return className;
	}

	private void makeMethodGet(ClassWriter cw, String methodName, String className, Class<?> clzEntity) {

		Field[] fields = clzEntity.getDeclaredFields();

		List<Map<String, Object>> getFields = new ArrayList<Map<String, Object>>(fields.length);

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

				getFields.add(setField);

			} catch (Exception e) {
			}
		}

		if (getFields.isEmpty()) {

			return;
		}

		MethodWriter mv = cw.visitMethod(Opcodes.ACC_PUBLIC, methodName,
				"(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;", null, null);

		mv.visitCode();
		Label lblIf = new Label();
		String entityPath = ASMUtils.getInternalName(clzEntity);

		for (int i = 0, size = getFields.size(); i < size; i++) {

			try {

				Map<String, Object> setField = getFields.get(i);

				String fieldName = (String) setField.get("fieldName");
				Class<?> fieldClass = (Class<?>) setField.get("fieldType");
				Class<?> fieldRefClass = ASMUtils.getReferenceType(fieldClass);
				String fieldType = ASMUtils.getDescriptor(fieldClass);
				String fieldClassPath = ASMUtils.getInternalName(fieldClass);
				String fieldRefType = ASMUtils.getDescriptor(fieldRefClass);
				String fieldRefClassPath = ASMUtils.getInternalName(fieldRefClass);
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

				if (fieldRefClassPath.equals(fieldClassPath) == false) {

					mv.visitMethodInsn(INVOKESTATIC, fieldRefClassPath, "valueOf", "(" + fieldType + ")" + fieldRefType,
							false);
				}

				mv.visitInsn(ARETURN);

				if (i + 1 < size) {

					lblElse = new Label();
				}

				mv.visitLabel(lblIf);
				mv.visitFrame(F_SAME, 0, null, 0, null);

				lblIf = lblElse;

			} catch (Exception e) {
			}

		}

		mv.visitInsn(ACONST_NULL);
		mv.visitInsn(ARETURN);
		mv.visitMaxs(2, 2);
		mv.visitEnd();
	}

	private void makeMethodSet(ClassWriter cw, String methodName, String className, Class<?> clzEntity) {

		Field[] fields = clzEntity.getDeclaredFields();

		List<Map<String, Object>> setFields = new ArrayList<Map<String, Object>>(fields.length);

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

		MethodWriter mv = cw.visitMethod(Opcodes.ACC_PUBLIC, methodName,
				"(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V", null, null);

		mv.visitCode();
		Label lblIf = new Label();
		String entityPath = ASMUtils.getInternalName(clzEntity);

		for (int i = 0, size = setFields.size(); i < size; i++) {

			try {

				Map<String, Object> setField = setFields.get(i);

				String fieldName = (String) setField.get("fieldName");
				Class<?> fieldClass = (Class<?>) setField.get("fieldType");
				Class<?> fieldRefClass = ASMUtils.getReferenceType(fieldClass);
				String fieldType = ASMUtils.getDescriptor(fieldClass);
				String fieldClassPath = ASMUtils.getInternalName(fieldClass);
				String fieldRefClassPath = ASMUtils.getInternalName(fieldRefClass);
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

				if (fieldRefClassPath.equals(fieldClassPath) == false) {

					mv.visitMethodInsn(INVOKEVIRTUAL, fieldRefClassPath, fieldClassPath + "Value", "()" + fieldType,
							false);
				}

				mv.visitMethodInsn(INVOKEVIRTUAL, entityPath, setName, "(" + fieldType + ")V", false);

				if (i + 1 < size) {

					lblElse = new Label();
					mv.visitJumpInsn(GOTO, lblElse);
				}

				mv.visitLabel(lblIf);
				mv.visitFrame(F_SAME, 0, null, 0, null);

				lblIf = lblElse;

			} catch (Exception e) {
			}

		}

		mv.visitInsn(RETURN);
		mv.visitMaxs(2, 2);
		mv.visitEnd();
	}
}
