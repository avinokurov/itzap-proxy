package net.proxy;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import net.proxy.model.ArtifactInterface;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Callable;

import static net.proxy.ProxyUtils.unwrapObjects;

public abstract class AbstractCaller implements ProxyCallerInterface {
    private final Class clazz;
    private final Map<String, MethodDesriptor.Result> data = Maps.newHashMap();
    private final Cache<MethodDesriptor, Method> methodCache = CacheBuilder.newBuilder()
            .build();
    private final ArtifactInterface artifact;

    public AbstractCaller(Class clazz,
                          ArtifactInterface artifact,
                          Map<String, MethodDesriptor.Result> data) {
        this.clazz = clazz;
        this.artifact = artifact;
        this.data.putAll(data);
    }

    @Override
    public MethodDesriptor.Result call(String name) {
        return makeCall(MethodDesriptor.method(name));
    }

    @Override
    public MethodDesriptor.Result call(final String name, final Object... params) {
        if (ArrayUtils.isEmpty(params)) {
            return call(name);
        }

        return call(MethodDesriptor.method(name, params));
    }

    @Override
    public MethodDesriptor.Result call(MethodDesriptor desriptor) {
        return makeCall(desriptor, desriptor.getParams());
    }

    @Override
    public MethodDesriptor.Result call(MethodDesriptor desriptor, Object ... params) {
        return makeCall(desriptor, unwrapObjects(params));
    }

    @Override
    public MethodDesriptor.Result data(String name) {
        return this.data.get(name);
    }

    @Override
    public String getVersion() {
        return this.artifact.getVersion().getVersion();
    }

    @Override
    public File getDir() {
        return this.artifact.getVersion().getDir();
    }

    @Override
    public String getPath() {
        return this.artifact.getVersion().getPath();
    }

    @Override
    public ArtifactInterface getArtifact() {
        return artifact;
    }

    @SuppressWarnings("unchecked")
    private MethodDesriptor.Result makeCall(final MethodDesriptor desriptor, ProxyUtils.ProxyObject... params) {
        if (desriptor == null || StringUtils.isBlank(desriptor.getName())) {
            return null;
        }

        try {
            Method method;
            try {
                method = clazz.getMethod(desriptor.getName(), desriptor.getSignature());
            } catch (NoSuchMethodException ex) {
                LOG.info("Proxy {} Method {} for signature not found. Trying with primitives.",
                        getName(), desriptor.getName());
                method = clazz.getMethod(desriptor.getName(),
                        ClassUtils.wrappersToPrimitives(desriptor.getSignature()));
            }

            CachedMethod cachedMethod = new CachedMethod(see(), clazz, method, desriptor, this.artifact);
            return cachedMethod.makeCall(params);

        } catch (Exception e) {
            throw new ProxyException(this,
                    String.format("Failed to call method %s", desriptor.getName()), e);
        }
    }

    private Class[] remap(Class[] signature) {
        if (ArrayUtils.isEmpty(signature)) {
            return signature;
        }

        // remap classes from the same class loader
        final ClassLoader loader = this.clazz.getClassLoader();
        return Arrays.stream(signature).map(input -> {
            if (input == null) {
                return null;
            }
            try {
                return loader.loadClass(input.getName());
            } catch (ClassNotFoundException e) {
                LOG.warn("Class {} in method signature is not found in the target class loader",
                        input.getName());
                return input;
            }
        }).toArray(Class[]::new);

    }

    @Override
    public boolean isInstanceOf(String name) {
        if (this.clazz == null) {
            return false;
        }
        if (name.indexOf('.') == -1) {
            return this.clazz.getSimpleName().equalsIgnoreCase(name);
        }

        try {
            Class<?> namedClazz = clazz.getClassLoader().loadClass(name);
            return namedClazz.isInstance(see());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Class myClass() {
        return this.clazz;
    }

    @Override
    public boolean isNull() {
        return see() == null;
    }

    @Override
    public String getName() {
        return this.clazz == null ? "unknown" :
                this.clazz.getName();
    }

    @Override
    public String getLabel() {
        return this.artifact.getLabel();
    }
}
