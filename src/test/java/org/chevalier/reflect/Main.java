package org.chevalier.reflect;

public class Main {

	public static void main(String[] args) throws Exception {
		
		AccessMethod as = AccessMethodFactory.getAccessMethod(TestEntity.class);
		TestEntity testEntity = TestEntity.class.newInstance();
		
		as.set(testEntity, "intValue", Integer.MAX_VALUE);
		as.set(testEntity, "shortValue", Short.MAX_VALUE);
		as.set(testEntity, "longValue", Long.MAX_VALUE);
		as.set(testEntity, "floatValue", Float.MAX_VALUE);
		as.set(testEntity, "doubleValue", Double.MAX_VALUE);
		as.set(testEntity, "charValue", 'A');
		as.set(testEntity, "byteValue", Byte.MAX_VALUE);
		as.set(testEntity, "booleanValue", true);
		as.set(testEntity, "stringValue", "Hello World!");

		System.out.println(as.get(testEntity, "intValue"));
		System.out.println(as.get(testEntity, "shortValue"));
		System.out.println(as.get(testEntity, "longValue"));
		System.out.println(as.get(testEntity, "floatValue"));
		System.out.println(as.get(testEntity, "doubleValue"));
		System.out.println(as.get(testEntity, "charValue"));
		System.out.println(as.get(testEntity, "byteValue"));
		System.out.println(as.get(testEntity, "booleanValue"));
		System.out.println(as.get(testEntity, "stringValue"));
	}

}
