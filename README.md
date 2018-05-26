# proxy-object
Load java jar files dynamically in isolated class loader to avoid dependency conflicts and enable modular updates

## Code Examples
1. Create Object from a JAR file located in the myLib/2.0 folder:
```java
ObjectBuilder builder = ObjectBuilder.builder()
       .setPackageName("org.my.package")
       .setClassName("MyClass")
       .setVersionInfo(newVersionInfo("myLib", "2.0"))
       .build();
builder.call("objectMethod");
```
2. Create object from factory method and pass "string param" and 1 to it:
```java
ObjectBuilder builder = ObjectBuilder.builder()
       .setPackageName("org.my.package")
       .setClassName("MyClass")
       .setVersionInfo(newVersionInfo("myLib", "2.0"))
       .setFactoryMethod("initMyClass")
       .setParams("string param", 1)
       .build();
```
3. Object builder can implement proxy interface:
```java
InvocationHandler handler = new InvocationHandler() {
};

ObjectBuilder builder = ObjectBuilder.builder()
   .setInterfaceName("org.mypackage.myclass")
   .setHandler(handler)
   .build();
```   
