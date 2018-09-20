package net.proxy;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import net.proxy.model.ArtifactInterface;
import net.proxy.utils.LibLoader;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static net.proxy.ProxyUtils.newProxy;

public class ObjectBuilder implements ProxyVersionedInterface {
    private static final Logger LOGGER = LoggerFactory.getLogger(ObjectBuilder.class);

    private String packageName;
    private String className;
    private ProxyUtils.ProxyObject[] params;
    private String factoryMethod;
    private List<MethodDesriptor> descriptors;
    private List<MethodDesriptor.Result> descriptorResults;
    private boolean staticObject;
    private String interfaceName;
    private InvocationHandler handler;
    private Map<String, MethodDesriptor.Result> data = ImmutableMap.of();
    private boolean pushClassloader;
    private ArtifactInterface artifact;

    private ObjectBuilder() {
    }

    public static ObjectBuilder builder() {
        return new ObjectBuilder();
    }

    public static ObjectBuilder from(ObjectBuilder builder) {
        return new ObjectBuilder()
                .setArtifact(builder.artifact)
                .setPackageName(builder.packageName)
                .setClassName(builder.className)
                .setInterfaceName(builder.interfaceName)
                .setFactoryMethod(builder.factoryMethod)
                .setHandler(builder.handler);
    }

    public ObjectBuilder setClassName(String className) {
        if (StringUtils.isNotBlank(className)) {
            this.className = className;
            this.interfaceName = null;
        } else {
            this.className = null;
        }

        return this;
    }

    public ObjectBuilder setPushClassloader(boolean pushClassloader) {
        this.pushClassloader = pushClassloader;
        return this;
    }

    public ObjectBuilder setParams(Object ... params) {
        this.params = ProxyUtils.unwrapObjects(params);
        return this;
    }

    public ObjectBuilder setArtifact(ArtifactInterface artifact) {
        this.artifact = artifact;
        return this;
    }

    public ObjectBuilder setPackageName(String packageName) {
        this.packageName = packageName;
        return this;
    }

    public ObjectBuilder setStaticObject(boolean staticObject) {
        this.staticObject = staticObject;
        return this;
    }

    public ObjectBuilder setDescriptors(List<MethodDesriptor> descriptors) {
        this.descriptors = descriptors;
        return this;
    }

    public ObjectBuilder setFactoryMethod(String factoryMethod) {
        this.factoryMethod = factoryMethod;
        return this;
    }

    public List<MethodDesriptor.Result> getDescriptorResults() {
        return descriptorResults;
    }

    public ObjectBuilder setInterfaceName(String interfaceName) {
        if (StringUtils.isNotBlank(interfaceName)) {
            this.interfaceName = interfaceName;
            this.className = null;
        } else {
            this.interfaceName = null;
        }
        return this;
    }

    public ObjectBuilder setHandler(InvocationHandler handler) {
        this.handler = handler;
        return this;
    }

    public ObjectBuilder setData(Map<String, Object> data) {
        this.data = Maps.transformEntries(data, new Maps.EntryTransformer<String, Object, MethodDesriptor.Result>() {
            @Override
            public MethodDesriptor.Result transformEntry(String key, Object value) {
                if (value instanceof MethodDesriptor.Result) {
                    return (MethodDesriptor.Result) value;
                }

                return new MethodDesriptor.Result(new CachedMethod(value, value.getClass(),
                        null, MethodDesriptor.method(key), artifact), value);
            }
        });
        return this;
    }

    public ArtifactInterface getArtifact() {
        return artifact;
    }

    public Class loadClass() {
        String clazzName = resolveClassName();

        LOGGER.debug("Loading enum {}", clazzName);

        try {
            return getClassLoader().loadClass(clazzName);
        } catch (Exception e) {
            throw new ProxyException(this, e);
        }
    }

    public ProxyCallerInterface build() {
        ProxyCallerInterface target;
        Class<?> targetClass;
        ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            targetClass = loadClass();
            if (this.pushClassloader) {
                Thread.currentThread().setContextClassLoader(targetClass.getClassLoader());
            }

            if (StringUtils.isNotBlank(this.interfaceName)) {
                // building proxy interface
                target = buildProxyInterface(targetClass);
            } else if (!this.staticObject) {
                // crete new instance of the object using new class loader
                if (StringUtils.isNoneBlank(this.factoryMethod)) {
                    target = buildFromFactoryMethod(targetClass);
                } else if (ArrayUtils.isEmpty(params)) {
                    LOGGER.debug("Calling empty constructor for class  {}", targetClass.getName());
                    target = new ProxyCaller(ProxyUtils.newProxy(targetClass), this.artifact, this.data);
                } else {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Calling constructor for class  {} with params {}",
                                targetClass.getName(), Joiner.on(',').join(params));
                    }
                    target = new ProxyCaller(newProxy(targetClass, params), this.artifact, this.data);
                }
            } else {
                target = new ProxyStaticCaller(targetClass, this.artifact, this.data);
            }
        } catch (ClassNotFoundException e) {
            throw new ProxyException(this, e);
        } catch (Exception e) {
            throw new ProxyException(this, String.format("Failed to create new instance of the class: %s",
                    resolveClassName()), e);
        } finally {
            if (this.pushClassloader) {
                Thread.currentThread().setContextClassLoader(currentClassLoader);
            }
        }

        return setup(target, targetClass);
    }

    private ProxyCallerInterface buildProxyInterface(Class targetClass) throws ClassNotFoundException {
        if (this.handler == null) {
            throw new ProxyException(this, String.format("Cannot create proxy class %s. Handler is null",
                    targetClass.getName()));
        }
        LOGGER.debug("Build proxy interface  {}", targetClass.getName());

        return new ProxyCaller(ProxyUtils.newProxy(Proxy.newProxyInstance(getClassLoader(),
                new Class[] {targetClass}, this.handler), targetClass), this.artifact, this.data);
    }

    private ProxyCallerInterface setup(final ProxyCallerInterface target, final Class<?> targetClass) {
        if (this.descriptors == null || this.descriptors.isEmpty()) {
            return target;
        }


        LOGGER.debug("Call setters on target {}", targetClass.getName());

        descriptorResults = FluentIterable.from(this.descriptors)
                .transform(new Function<MethodDesriptor, MethodDesriptor.Result>() {
                    @Override
                    public MethodDesriptor.Result apply(MethodDesriptor input) {
                        try {
                            return target.call(input);
                        } catch (Exception ex) {
                            throw new ProxyException(input,
                                    "Failed to call method on object " + targetClass.getName(), ex);
                        }
                    }
                }).toList();

        return target;
    }

    private ProxyCallerInterface buildFromFactoryMethod(Class<?> targetClass) throws Exception {
        if (StringUtils.isBlank(this.factoryMethod)) {
            throw new ProxyException(this, "Failed to build from factory method. Method name is empty");
        }

        LOGGER.debug("Building class {} from factory method {}",
                targetClass.getName(), this.factoryMethod);

        return new ProxyCaller(newProxy(targetClass, this.factoryMethod, this.params),
                this.artifact);
    }

    @SuppressWarnings("unchecked")
    public Map<String, ProxyEnum> buildEnum() {
        Class<?> typeClass = loadClass();
        Map<String, ProxyEnum> srcType = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Object type : typeClass.getEnumConstants()) {
            ProxyEnum proxyEnum = new ProxyEnum(ProxyUtils.newProxy(type, typeClass),
                    this.artifact);
            srcType.put(proxyEnum.name(), proxyEnum);
        }
        return srcType;
    }

    @Override
    public String getName() {
        return className;
    }

    @Override
    public String getLabel() {
        return this.artifact.getVersion().getLabel();
    }

    @Override
    public String getVersion() {
        return this.artifact.getVersion().getVersion();
    }

    @Override
    public String getPath() {
        return this.artifact.getVersion().getPath();
    }

    @Override
    public File getDir() {
        return this.artifact.getVersion().getDir();
    }

    private URLClassLoader getClassLoader() {
        String clazzName = resolveClassName();

        if (this.artifact == null ||
                this.artifact.getVersion() == null ||
                this.artifact.getVersion() == ProxyUtils.UNKNOWN_VERSION) {
            throw new ProxyException(this, "Lib version info cannot be null or unknown");
        }
        if (StringUtils.isBlank(clazzName)) {
            throw new ProxyException(this, "Class name cannot be blank");
        }
        boolean contains = LibLoader.containsLoader(artifact.getName());

        URLClassLoader classLoader = LibLoader.getLibClassLoader(artifact);

        if (classLoader == null) {
            throw new ProxyException(this, String.format("Failed to find class loader for artifact %s",
                    artifact.toString()));
        }

        if (!contains) {
            LOGGER.info("Loading from {}",
                    Joiner.on('\n').join(classLoader.getURLs()));
        }

        return classLoader;
    }

    private String resolveClassName() {
        if (StringUtils.isNotBlank(this.className) &&
                StringUtils.isNotBlank(this.interfaceName)) {
            throw new ProxyException(this, String.format("Cannot resolve class name. " +
                            "Cannot have class name %s and interface %s defined at the same time",
                    this.className, this.interfaceName));
        }

        String clazzName = StringUtils.defaultIfBlank(this.className, this.interfaceName);
        return Joiner.on('.').skipNulls().join(this.packageName, clazzName);
    }

    public ProxyVersionedInterface getVersionInfo() {
        return this.artifact.getVersion();
    }
}
