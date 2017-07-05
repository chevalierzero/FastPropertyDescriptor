package org.chevalier.reflect;

import javax.lang.model.element.Modifier;

public class Main {

	public static void main(String[] args) throws Exception {
		
		AccessMethod accessMethod = AccessMethodFactory.getAccessMethod(TestEntity.class);
		
		TestEntity testEntity = TestEntity.class.newInstance();
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

		System.out.println(accessMethod.get(testEntity, "byteValue"));
		System.out.println(accessMethod.get(testEntity, "charValue"));
		System.out.println(accessMethod.get(testEntity, "shortValue"));
		System.out.println(accessMethod.get(testEntity, "intValue"));
		System.out.println(accessMethod.get(testEntity, "longValue"));
		System.out.println(accessMethod.get(testEntity, "floatValue"));
		System.out.println(accessMethod.get(testEntity, "doubleValue"));
		System.out.println(accessMethod.get(testEntity, "booleanValue"));
		System.out.println(accessMethod.get(testEntity, "stringValue"));
		System.out.println(accessMethod.get(testEntity, "enumValue"));
		System.out.println(accessMethod.get(testEntity, "objectValue"));
	}

}
