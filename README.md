# itzap-proxy
Load java jar files dynamically in isolated class loader to avoid dependency conflicts and enable modular updates. All jar files in the [main jar folder]/lib/myLib/2.0/*.jar will be loaded.
## Code Examples
1. Create Object from a JAR file located in the myLib/2.0 folder:
```java
ProxyCallerInterface object = ObjectBuilder.builder()
       .setPackageName("org.my.package")
       .setClassName("MyClass")
       .setVersionInfo(newVersionInfo("myLib", "2.0"))
       .build();
object.call("objectMethod");
```
2. Create object from factory method and pass "string param" and 1 to it:
```java
ProxyCallerInterface object = ObjectBuilder.builder()
       .setPackageName("org.my.package")
       .setClassName("MyClass")
       .setVersionInfo(newVersionInfo("myLib", "2.0"))
       .setFactoryMethod("initMyClass")
       .setParams("string param", 1)
       .build();
```
3. Object builder can implement proxy interface:
```java
ProxyCallerInterface object = ObjectBuilder.builder()
   .setClassName("org.mypackage.myclass")
   .setVersionInfo(newVersionInfo("myLib", "2.0"))
   .build();

// Example with call back
object.call("addListener", ObjectBuilder.from(builder)
                            .setInterfaceName("org.mypackage.MyListener")
                            .setHandler(new InvocationHandler() {
                                @Override
                                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                                   // handle call back
                                   return null;
                                })
                            .build()
```   
4. Instantiating object with constructor parameters
```java
ProxyCallerInterface object = ObjectBuilder.builder()
       .setPackageName("org.my.package")
       .setClassName("MyClass")
       .setVersionInfo(newVersionInfo("myLib", "2.0"))
       .setParams("String param", 1)
       .build();
       
```
5. Loading Enums
```java
Map<String, ProxyEnum> enum = ObjectBuilder.from(builder)
                                    .setClassName("MyClass$USStates")
                                    .buildEnum();
```
6. Calling setters on a created object
```java
List<MethodDesriptor> descriptors = Lists.newArrayList();
ObjectBuilder builder = ObjectBuilder.builder()
                .setClassName("org.mypackage.MyClass")
                .setParams("String param")
                .setDescriptors(descriptors);
// call setter
descriptors.add(MethodDesriptor.method("setProperty", "property name", "value"));
ProxyCallerInterface object = builder.build();
```
7. Calling static methods
```java
List<MethodDesriptor> settings = Lists.newArrayList();
ObjectBuilder builder = ObjectBuilder.from(builder)
                .setClassName("org.MyUtils")
                .setStaticObject(true)
                .setDescriptors(settings);
                
settings.add(MethodDesriptor.method("convert", "from this string"));
builder.build();
 
