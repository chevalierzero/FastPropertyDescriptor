package org.chevalier.reflect;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.chevalier.reflect.asm.ClassWriter;
import org.chevalier.reflect.asm.Label;
import org.chevalier.reflect.asm.MethodWriter;
import org.chevalier.reflect.asm.Opcodes;
import org.chevalier.reflect.util.ASMClassLoader;
import org.chevalier.reflect.util.ASMUtils;
import org.chevalier.reflect.util.SimpleMap;

/**
 * @author Chevalier (chevalier_zero@hotmail.com)
 */
final class AccessMethodLoader implements Opcodes {

	private final static int VERSION_OPCODE = ASMUtils.getVersionOpcode();

	private final static SimpleMap<String, String> CLASS_NAMES = new SimpleMap<String, String>();

	public static AccessMethod loadClass(Class<?> clzEntity) throws Exception {

		String className = getClassName(clzEntity);
		String classInternalName = ASMUtils.getInternalName(className);
		ClassWriter cw = new ClassWriter();
		cw.visit(VERSION_OPCODE, ACC_PUBLIC + ACC_SUPER, classInternalName,
				ASMUtils.getInternalName(AbstractAccessMethod.class), null);

		MethodWriter mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, ASMUtils.getInternalName(AbstractAccessMethod.class), "<init>", "()V", false);
		mv.visitInsn(RETURN);
		mv.visitMaxs(1, 1);
		mv.visitEnd();

		makeMethodGet(cw, "get", classInternalName, clzEntity);
		makeMethodSet(cw, "set", classInternalName, clzEntity);

		cw.visitEnd();

		Object obj = ASMClassLoader.getInstance().defineClass(className, cw.toByteArray()).newInstance();
		AccessMethod asm = (AccessMethod) obj;

		return asm;
	}

	/**
	 * 构造类名称，如果类名重复则在结尾加上一个递增的数字
	 * 
	 * @param clzEntity
	 * @return
	 */
	private static String getClassName(Class<?> clzEntity) {

		String key = clzEntity.getSimpleName();
		String className = CLASS_NAMES.get(key);
		String baseName = AccessMethod.class.getName() + "$" + key + "Impl";

		if (className == null) {

			className = baseName;
			
		} else if (className.length() == baseName.length()) {

			className = baseName + "2";

		} else {

			// 获取类名后面的数字，继续累加
			int index = Integer.parseInt(className.substring(baseName.length()));

			if (index >= Integer.MAX_VALUE) {

				throw new Error("too many classes with the same name");
			}

			className = baseName + (++index);
		}

		CLASS_NAMES.put(key, className);

		return className;
	}

	private static void makeMethodGet(ClassWriter cw, String methodName, String className, Class<?> clzEntity) {

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

	private static void makeMethodSet(ClassWriter cw, String methodName, String className, Class<?> clzEntity) {

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

	public abstract static class AbstractAccessMethod implements AccessMethod {

		public <T> T get(Object obj, String fieldName, Class<T> clz) {

			Object value = get(obj, fieldName);

			return (value != null) ? clz.cast(value) : null;
		}
	}
}
