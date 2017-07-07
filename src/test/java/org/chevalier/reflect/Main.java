package org.chevalier.reflect;

import java.util.Arrays;

import javax.lang.model.element.Modifier;

public class Main {

	public static void main(String[] args) throws Exception {

		TestEntity testEntity = TestEntity.class.newInstance();
		
		AccessMethod accessMethod = AccessMethodFactory.getAccessMethod(TestEntity.class);
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

		System.out.println("byteValue : " + accessMethod.get(testEntity, "byteValue"));
		System.out.println("charValue : " + accessMethod.get(testEntity, "charValue"));
		System.out.println("shortValue : " + accessMethod.get(testEntity, "shortValue"));
		System.out.println("intValue : " + accessMethod.get(testEntity, "intValue"));
		System.out.println("longValue : " + accessMethod.get(testEntity, "longValue"));
		System.out.println("floatValue : " + accessMethod.get(testEntity, "floatValue"));
		System.out.println("doubleValue : " + accessMethod.get(testEntity, "doubleValue"));
		System.out.println("booleanValue : " + accessMethod.get(testEntity, "booleanValue"));
		System.out.println("stringValue : " + accessMethod.get(testEntity, "stringValue"));
		System.out.println("enumValue : " + accessMethod.get(testEntity, "enumValue"));
		System.out.println("objectValue : " + accessMethod.get(testEntity, "objectValue"));
		System.out.println("intValues : " + Arrays.toString((int[])accessMethod.get(testEntity, "intValues")));
		System.out.println("objectValues : " + Arrays.toString((Object[])accessMethod.get(testEntity, "objectValues")));
	}
}
