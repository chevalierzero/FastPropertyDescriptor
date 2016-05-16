package org.chevalier.reflect;

public class Main {

	public static void main(String[] args) throws Exception {
		
		AccessMethod accessMethod = AccessMethodFactory.getAccessMethod(TestEntity.class);
		TestEntity testEntity = TestEntity.class.newInstance();
		
		accessMethod.set(testEntity, "intValue", Integer.MAX_VALUE);
		accessMethod.set(testEntity, "shortValue", Short.MAX_VALUE);
		accessMethod.set(testEntity, "longValue", Long.MAX_VALUE);
		accessMethod.set(testEntity, "floatValue", Float.MAX_VALUE);
		accessMethod.set(testEntity, "doubleValue", Double.MAX_VALUE);
		accessMethod.set(testEntity, "charValue", 'A');
		accessMethod.set(testEntity, "byteValue", Byte.MAX_VALUE);
		accessMethod.set(testEntity, "booleanValue", true);
		accessMethod.set(testEntity, "stringValue", "Hello World!");

		System.out.println(accessMethod.get(testEntity, "intValue"));
		System.out.println(accessMethod.get(testEntity, "shortValue"));
		System.out.println(accessMethod.get(testEntity, "longValue"));
		System.out.println(accessMethod.get(testEntity, "floatValue"));
		System.out.println(accessMethod.get(testEntity, "doubleValue"));
		System.out.println(accessMethod.get(testEntity, "charValue"));
		System.out.println(accessMethod.get(testEntity, "byteValue"));
		System.out.println(accessMethod.get(testEntity, "booleanValue"));
		System.out.println(accessMethod.get(testEntity, "stringValue"));
	}

}
