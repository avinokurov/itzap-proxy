package net.proxy;

import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import net.proxy.model.AbstractArtifact;
import net.proxy.model.ArtifactInterface;
import net.proxy.model.DirArtifact;
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
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Set;

import static net.proxy.utils.DirUtils.getVersion;
import static net.proxy.utils.LibLoader.jarPredicate;

public final class ProxyUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyUtils.class);
    public static final String UNKNOWN_VALUE = "unknown";
    public static final ProxyVersionedInterface UNKNOWN_VERSION;

    public static ProxyObject NULL_OBJECT = new ProxyObject(null, Object.class);

    static {
        UNKNOWN_VERSION = newVersionInfo(UNKNOWN_VALUE, UNKNOWN_VALUE, null);
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
                                                         final String version,
                                                         final File libDir) {
        return newVersionInfo(label, version, libDir, UNKNOWN_VALUE);
    }

    public static ProxyVersionedInterface newVersionInfo(final String label,
                                                         final String version,
                                                         final File libDir,
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
            public File getDir() {
                return libDir;
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

    public static ProxyCallerInterface takeFirst(Object[] args, ArtifactInterface artifact) {
        if (ArrayUtils.isEmpty(args)) {
            return new ProxyStaticCaller(null, artifact);
        } else {
            return new ProxyCaller(newProxy(args[0]), artifact);
        }
    }

    public static boolean isMethod(Method method, String name) {
        return method != null &&
                method.getName().equalsIgnoreCase(name);
    }

    public static ArtifactInterface fromObject(final Object object) {
        if (object instanceof ProxyCallerInterface) {
            return ((ProxyCallerInterface)object).getArtifact();
        } else if (object instanceof MethodDesriptor.Result) {
            return ((MethodDesriptor.Result) object).asProxy().getArtifact();
        } else if (object instanceof ProxyInterface) {
                return versionFromObject(object);
        } else {
            return AbstractArtifact.UKNOWN_ARTIFACT;
        }

    }

    public static ArtifactInterface versionFromObject(final Object obj) {
        if (obj == null) {
            return AbstractArtifact.UKNOWN_ARTIFACT;
        }

        return artifactFromClass(obj.getClass());
    }

    public static ArtifactInterface artifactFromClass(final Class clazz) {
        final URL location = getUrlFromClass(clazz);

        try {
            if (location != null) {
                final File jarFile = new File(location.toURI());
                String ext = FilenameUtils.getExtension(jarFile.getAbsolutePath());
                Set<String> extentions = ImmutableSet.of(ext);
                if ("jar".equalsIgnoreCase(ext)) {
                    File libFolder = libFolderFromPath(jarFile).getParentFile().getParentFile();

                    ProxyVersionedInterface version = newVersionInfo(getVersion(jarFile.getParentFile().getParentFile()),
                            getVersion(jarFile), libFolder);

                    return new DirArtifact(extentions,
                            clazz, jarPredicate(location.toURI().getPath(),
                            libFolder.getAbsoluteFile().getAbsolutePath(), extentions),
                            version);
                } else {
                    File libFolder = libFolderFromPath(jarFile).getParentFile();

                    ProxyVersionedInterface version = newVersionInfo(getVersion(jarFile.getParentFile()),
                            getVersion(jarFile), libFolder);
                    return new DirArtifact(extentions,
                            clazz, jarPredicate(location.toURI().getPath(),
                            libFolder.getAbsoluteFile().getAbsolutePath(), extentions),
                            version);
                }
            } else {
                return AbstractArtifact.UKNOWN_ARTIFACT;
            }
        } catch (Exception e) {
            LOGGER.error("Could not get version info from class {}", clazz.getName(), e);
            return AbstractArtifact.UKNOWN_ARTIFACT;
        }
    }

    private static File libFolderFromPath(File path) {
        String folder = getVersion(path);
        File fromPath = path;

        while(fromPath != null && !"lib".equalsIgnoreCase(folder)) {
            fromPath = fromPath.getParentFile();
            folder = getVersion(fromPath);
        }
        return fromPath == null ? path : fromPath;
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

        String path = "engine";

        if (StringUtils.isNotBlank(versionInfo.getLabel())) {
            path += ("/" + versionInfo.getLabel());
        }

        if (StringUtils.isBlank(versionInfo.getVersion())) {
            return normalizeToUnix(path);
        }

        if (StringUtils.isBlank(versionInfo.getName()) ||
                UNKNOWN_VALUE.equalsIgnoreCase(versionInfo.getName())) {
            return normalizeToUnix(path + "/" + versionInfo.getVersion() + "/");
        }

        return normalizeToUnix(path + "/" +
                        versionInfo.getVersion() + "/" +
                        versionInfo.getName() + "/");
    }

    public static String getS3Path(String path, String version) {
        if (StringUtils.isBlank(path) ||
                UNKNOWN_VALUE.equalsIgnoreCase(path)) {
            return normalizeToUnix(version);
        }

        return normalizeToUnix(path + "/" + version);
    }

    private static String normalizeToUnix(String path) {
        return FilenameUtils.separatorsToUnix(FilenameUtils.normalize(path));
    }

    public static URL getUrlFromClass(final Class clazz) {
        if (clazz == null) {
            return null;
        }

        final URL location = clazz.getResource('/' + clazz.getName().replace('.', '/') + ".class");
        try {
            if (location != null && "jar".equalsIgnoreCase(location.toURI().getScheme())) {
                JarURLConnection connection = (JarURLConnection) location.openConnection();

                return connection.getJarFileURL();
            } else {
                return null;
            }
        } catch (Exception e) {
            LOGGER.error("Could not get jar URL info from class {}", clazz.getName(), e);
            return null;
        }
    }
}
