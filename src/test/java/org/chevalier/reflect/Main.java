package org.chevalier.reflect;

import java.util.Arrays;

import javax.lang.model.element.Modifier;

public class Main {

	public static void main(String[] args) throws Exception {

		TestEntity testEntity = TestEntity.class.newInstance();
		
		AccessMethod accessMethod = AccessMethodFactory.build(TestEntity.class);
		accessMethod.set(testEntity, "byteValue", Byte.MAX_VALUE);
		accessMethod.set(testEntity, "charValue", 'A');
		accessMethod.set(testEntity, "shortValue", Short.MAX_VALUE);
		accessMethod.set(testEntity, "intValue", Integer.MAX_VALUE);
		accessMethod.set(testEntity, "longValue", Long.MAX_VALUE);
		accessMethod.set(testEntity, "floatValue", Float.MAX_VALUE);
		accessMethod.set(testEntity, "doubleValue", Double.MAX_VALUE);
		accessMethod.set(testEntity, "booleanValue", true);
		accessMethod.set(testEntity, "stringValue", "Hello World!");
		accessMethod.set(testEntity, "enumValue", Modifier.ABSTRACT);
		accessMethod.set(testEntity, "objectValue", new Object());
		accessMethod.set(testEntity, "intValues", new int[]{1, 2, 3, 4, 5});
		accessMethod.set(testEntity, "objectValues", new Object[]{new Object(), new Object()});

		System.out.println("byteValue : " + accessMethod.get(testEntity, "byteValue", Byte.class));
		System.out.println("charValue : " + accessMethod.get(testEntity, "charValue", Character.class));
		System.out.println("shortValue : " + accessMethod.get(testEntity, "shortValue", Short.class));
		System.out.println("intValue : " + accessMethod.get(testEntity, "intValue", Integer.class));
		System.out.println("longValue : " + accessMethod.get(testEntity, "longValue", Long.class));
		System.out.println("floatValue : " + accessMethod.get(testEntity, "floatValue", Float.class));
		System.out.println("doubleValue : " + accessMethod.get(testEntity, "doubleValue", Double.class));
		System.out.println("booleanValue : " + accessMethod.get(testEntity, "booleanValue", Boolean.class));
		System.out.println("stringValue : " + accessMethod.get(testEntity, "stringValue", String.class));
		System.out.println("enumValue : " + accessMethod.get(testEntity, "enumValue", Enum.class));
		System.out.println("objectValue : " + accessMethod.get(testEntity, "objectValue", Object.class));
		System.out.println("intValues : " + Arrays.toString(accessMethod.get(testEntity, "intValues", int[].class)));
		System.out.println("objectValues : " + Arrays.toString((Object[])accessMethod.get(testEntity, "objectValues", Object[].class)));
	}
}
