# PropertyDescriptorASM
  PropertyDescriptorASM可以快速的对实体类的get、set方法进行反射调用，速度接近于直接使用代码调用，可以代替Java自带的反射方法
# 如何使用
  TestEntity testEntity = new TestEntity(); <br/>
  // it's an example of how to initialise a AccessMethod for TestEntity <br/>
  AccessMethod accessMethod = AccessMethodFactory.build(TestEntity.class) <br/>
  // it's an example of how to use method set <br/>
  accessMethod.set(testEntity, "property1", "It's a test value"); <br/>
  // it's an example of how to use method get <br/>
  String testValue = (String)accessMethod.get(testEntity, "property1"); <br/>
  String testValue = accessMethod.get(testEntity, "property1", String.class);
# 注意事项
  由于本项目采用字节码方式生成每个实体类对应的反射类，所以首次调用某个实体类的get或set方法时会占用少量时间对这个类进行初始化（消耗的时间和类中属性的数量有关，代码测试中的类花费时间为40ms），再次使用后的速度接近于直接调用get与set方法
