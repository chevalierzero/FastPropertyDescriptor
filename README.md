# PropertyDescriptorASM
  PropertyDescriptorASM是一个依赖于“ASM”、通过反射的方式快速调用get与set方法的微型工具包。由于目前代码非常不成熟，所以希望各路大牛们帮忙维护，目前本人也在开发一套轻量级的jdbc框架，希望以后可以与此项目相结合
# 如何使用
  TestEntity testEntity = new TestEntity();
  // it's an example of how to initialise a AccessMethod for TestEntity
  AccessMethod accessMethod = AccessMethodFactory.getAccessSetMethod(TestEntity.class)
  // it's an example of how to use method set
  accessMethod.set(testEntity, "property1", "It's a test value");
  // it's an example of how to use method get
  String testValue = (String)accessMethod.get(testEntity, "property1");
# 注意事项
  由于本项目采用字节码方式生成每个实体类对应的反射类，所以首次对每个实体类的初始化会占用少量时间（自测试为10ms左右），再次使用后的速度接近于直接调用get与set方法
# 第三方依赖
  asm-5.0.4
