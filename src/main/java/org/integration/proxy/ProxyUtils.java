package org.integration.proxy;

import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.collect.FluentIterable;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;

import static org.integration.proxy.utils.LibUtils.getVersion;
import static org.integration.proxy.utils.jar.JarUtils.getUrlFromClass;


public final class ProxyUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyUtils.class);
    private static final String UNKNOWN_VALUE = "unknown";

    static final ProxyVersionedInterface UNKNOWN_VERSION;
    public static ProxyObject NULL_OBJECT = new ProxyObject(null, Object.class);

    static {
        UNKNOWN_VERSION = newVersionInfo(UNKNOWN_VALUE, UNKNOWN_VALUE);
    }

    private ProxyUtils() {}

    public static class ProxyObject {
        private final Object object;
        private final Class clazz;

        private ProxyObject(Object object) {
            this(object, object.getClass());
        }

        private ProxyObject(ProxyObject object) {
            this(object.object, object.clazz);
        }

        private ProxyObject(Object object, Class clazz) {
            this.object = object;
            this.clazz = clazz;
        }

        public Object getObject() {
            return object;
        }

        public Class getClazz() {
            return clazz;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("object", object)
                    .add("class", clazz)
                    .toString();
        }
    }

    private static Function<Object, ProxyObject> MAP_FUNCTION = new Function<Object, ProxyObject>() {
        @Override
        public ProxyObject apply(Object input) {
            return newProxy(input);
        }
    };

    private static Function<ProxyObject, Object> MAP_TO_OBJECT_FUNCTION = new Function<ProxyObject, Object>() {
        @Override
        public Object apply(ProxyObject input) {
            return input == null ? null : input.getObject();
        }
    };

    public static ProxyVersionedInterface newVersionInfo(final String label,
                                                                                 final String version) {
        return newVersionInfo(label, version, UNKNOWN_VALUE);
    }

    public static ProxyVersionedInterface newVersionInfo(final String label,
                                                                                 final String version,
                                                                                 final String name) {
        return new ProxyVersionedInterface() {
            @Override
            public String getVersion() {
                return version;
            }

            @Override
            public String getPath() {
                return ProxyUtils.getPath(this);
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getLabel() {
                return label;
            }
        };
    }

    static ProxyObject unwrapObject(Object obj) {
        return newProxy(obj);
    }

    static ProxyObject[] unwrapObjects(Object ... params) {
        if (ArrayUtils.isEmpty(params)) {
            return new ProxyObject[0];
        }
        if (params.length == 1) {
            return new ProxyObject[]{MAP_FUNCTION.apply(params[0])};
        }

        return FluentIterable.of(params)
                .transform(MAP_FUNCTION).toArray(ProxyObject.class);
    }

    static Object[] unwrapProxies(MethodDesriptor desriptor) {
        if (desriptor == null || ArrayUtils.isEmpty(desriptor.getParams())) {
            return new Object[0];
        }

        return unwrapProxies(desriptor.getParams());
    }

    static Object[] unwrapProxies(ProxyObject ... params) {
        if (ArrayUtils.isEmpty(params)) {
            return new Object[0];
        }
        if (params.length == 1) {
            return new Object[]{params[0].getObject()};
        }

        return FluentIterable.of(params)
                .transform(MAP_TO_OBJECT_FUNCTION).toArray(Object.class);
    }

    static Class[] getClasses(Object[] params) {
        if (ArrayUtils.isEmpty(params)) {
            return new Class[0];
        } else {
            return FluentIterable.of(unwrapObjects(params))
                    .transform(new Function<ProxyObject, Class>() {
                        @Override
                        public Class apply(ProxyObject input) {
                            return input.getClazz();
                        }
                    })
                    .toArray(Class.class);
        }
    }

    static ProxyObject newProxy(Object target) {
        if (target instanceof ProxyCallerInterface) {
            ProxyCallerInterface iProxy = (ProxyCallerInterface)target;
            return new ProxyObject(iProxy.see(), iProxy.myClass());
        } else if (target instanceof MethodDesriptor.Result) {
            ProxyCallerInterface proxy = ((MethodDesriptor.Result)target).asProxy();
            return new ProxyObject(proxy.see(), proxy.myClass());
        } else if (target instanceof ProxyObject) {
            return new ProxyObject((ProxyObject)target);
        }

        return new ProxyObject(target, target.getClass());
    }

    public static ProxyObject newProxy(Object target, Class clazz) {
        return new ProxyObject(target, clazz);
    }

    static ProxyObject newProxy(ProxyObject target) {
        return new ProxyObject(target);
    }

    @SuppressWarnings("unchecked")
    static ProxyObject newProxy(Class<?> clazz) throws IllegalAccessException, InstantiationException {
        return new ProxyObject(clazz.newInstance(), clazz);
    }

    @SuppressWarnings("unchecked")
    static ProxyObject newProxy(Class<?> clazz, ProxyObject ... params) throws IllegalAccessException,
            InvocationTargetException, InstantiationException, NoSuchMethodException {

        Constructor constructor;
        Class[] signature = getClasses(params);
        try {
            constructor = clazz.getConstructor(signature);
        } catch (NoSuchMethodException e) {
            constructor = clazz.getConstructor(ClassUtils.wrappersToPrimitives(signature));
        }

        if (ArrayUtils.isEmpty(params)) {
            return new ProxyObject(constructor.newInstance(), clazz);
        }

        return new ProxyObject(constructor.newInstance(unwrapProxies(params)), clazz);
    }

    @SuppressWarnings("unchecked")
    static ProxyObject newProxy(Class<?> clazz, String factoryMethod, ProxyObject ... params) throws NoSuchMethodException,
            InvocationTargetException, IllegalAccessException {
        if (ArrayUtils.isEmpty(params)) {
            return new ProxyObject(clazz.getMethod(factoryMethod).invoke(null), clazz);
        } else {
            Class[] signature = getClasses(params);
            Object[] uParams = unwrapProxies(params);
            try {
                return new ProxyObject(clazz.getMethod(factoryMethod, signature)
                        .invoke(null, uParams));
            } catch (NoSuchMethodException e) {
                LOGGER.info("Failed to find factory method {}. Try to unwrap params", factoryMethod);
                return new ProxyObject(clazz.getMethod(factoryMethod, ClassUtils.wrappersToPrimitives(signature))
                        .invoke(null, uParams));
            }
        }
    }

    public static ProxyCallerInterface takeFirst(Object[] args, ProxyVersionedInterface versionInfo) {
        if (ArrayUtils.isEmpty(args)) {
            return new ProxyStaticCaller(null, versionInfo);
        } else {
            return new ProxyCaller(newProxy(args[0]), versionInfo);
        }
    }

    public static boolean isMethod(Method method, String name) {
        return method != null &&
                method.getName().equalsIgnoreCase(name);
    }

    public static ProxyVersionedInterface fromObject(final Object object) {
        if (object instanceof ProxyCallerInterface) {
            return (ProxyCallerInterface)object;
        } else if (object instanceof MethodDesriptor.Result) {
            return ((MethodDesriptor.Result) object).asProxy();
        } else if (object instanceof ProxyInterface) {
                return versionFromObject(object);
        } else {
            return UNKNOWN_VERSION;
        }

    }

    public static ProxyVersionedInterface versionFromObject(final Object obj) {
        if (obj == null) {
            return UNKNOWN_VERSION;
        }

        return versionFromClass(obj.getClass());
    }

    public static ProxyVersionedInterface versionFromClass(final Class clazz) {
        final URL location = getUrlFromClass(clazz);
        try {
            if (location != null) {
                final File jarFile = new File(location.toURI());
                String ext = FilenameUtils.getExtension(jarFile.getAbsolutePath());
                if ("jar".equalsIgnoreCase(ext)) {
                    return newVersionInfo(getVersion(jarFile.getParentFile().getParentFile()),
                            getVersion(jarFile));
                } else {
                    return newVersionInfo(getVersion(jarFile.getParentFile()),
                            getVersion(jarFile));
                }
            } else {
                return UNKNOWN_VERSION;
            }
        } catch (Exception e) {
            LOGGER.error("Could not get version info from class {}", clazz.getName(), e);
            return UNKNOWN_VERSION;
        }
    }

    public static String getPath(ProxyVersionedInterface versionInfo) {
        if (versionInfo == UNKNOWN_VERSION || versionInfo == null) {
            return UNKNOWN_VALUE;
        }

        return getPath(versionInfo.getLabel(), versionInfo.getVersion());
    }

    public static String getPath(String label, String version) {
        if (StringUtils.isBlank(label)) {
            return UNKNOWN_VALUE;
        }

        if (StringUtils.isBlank(version)) {
            return label;
        }

        return label + File.separator + version;
    }

    public static String getJarPath(ProxyVersionedInterface versionInfo) {
        if (versionInfo == UNKNOWN_VERSION || versionInfo == null) {
            return UNKNOWN_VALUE;
        }

        if (StringUtils.isBlank(versionInfo.getVersion())) {
            return versionInfo.getLabel();
        }

        return versionInfo.getLabel() + "/" + versionInfo.getVersion();
    }

    public static String getS3Path(ProxyVersionedInterface versionInfo) {
        if (versionInfo == UNKNOWN_VERSION || versionInfo == null) {
            return UNKNOWN_VALUE;
        }

        if (StringUtils.isBlank(versionInfo.getVersion())) {
            return normalizeToUnix("engine/" + versionInfo.getLabel());
        }

        if (StringUtils.isBlank(versionInfo.getName()) ||
                UNKNOWN_VALUE.equalsIgnoreCase(versionInfo.getName())) {
            return normalizeToUnix("engine/" + versionInfo.getLabel() + "/" +
                    versionInfo.getVersion() + "/");
        }

        return normalizeToUnix("engine/" + versionInfo.getLabel() + "/" +
                        versionInfo.getVersion() + "/" +
                        versionInfo.getName() + "/");
    }

    public static String getVersionWithDefault(String version, String defaultVersion) {
        if ("null".equalsIgnoreCase(version) || UNKNOWN_VALUE.equalsIgnoreCase(version)) {
            version = defaultVersion;
        }

        return StringUtils.defaultIfBlank(version, defaultVersion);
    }

    private static String normalizeToUnix(String path) {
        return FilenameUtils.separatorsToUnix(FilenameUtils.normalize(path));
    }
}
