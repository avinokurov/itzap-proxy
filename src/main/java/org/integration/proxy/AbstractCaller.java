package org.integration.proxy;

import com.google.common.base.Function;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.Callable;

import static org.integration.proxy.ProxyUtils.unwrapProxies;
import static org.integration.proxy.ProxyUtils.versionFromClass;


public abstract class AbstractCaller implements ProxyCallerInterface {
    private final Class clazz;
    private final ProxyVersionedInterface versionInfo;
    private final Map<String, MethodDesriptor.Result> data = Maps.newHashMap();
    private final Cache<MethodDesriptor, Method> methodCache = CacheBuilder.newBuilder()
            .build();

    public AbstractCaller(Class clazz,
                          ProxyVersionedInterface versionInfo,
                          Map<String, MethodDesriptor.Result> data) {
        this.clazz = clazz;
        this.versionInfo = versionInfo == null ?
            versionFromClass(clazz) : versionInfo;
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

        return makeCall(MethodDesriptor.method(name, params));
    }

    @Override
    public MethodDesriptor.Result data(String name) {
        return this.data.get(name);
    }

    @Override
    public MethodDesriptor.Result call(MethodDesriptor desriptor) {
        return makeCall(desriptor);
    }

    @Override
    public MethodDesriptor.Result call(MethodDesriptor desriptor, Object ... params) {
        return makeCall(MethodDesriptor.builder()
                .setParams(params)
                .setSignature(desriptor.getSignature())
                .setName(desriptor.getName())
                .setPushClassLoader(desriptor.isPushClassLoader())
                .build());
    }

    @Override
    public String getVersion() {
        return this.versionInfo.getVersion();
    }

    @Override
    public String getPath() {
        return this.versionInfo.getPath();
    }

    private MethodDesriptor.Result makeCall(final MethodDesriptor desriptor) {
        if (desriptor == null || StringUtils.isBlank(desriptor.getName())) {
            return null;
        }

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        //final Class[] signature = remap(desriptor.getSignature());
        try {
            Method method = methodCache.get(desriptor, new Callable<Method>() {
                    @Override
                    public Method call() throws Exception {
                        try {
                            return clazz.getMethod(desriptor.getName(), desriptor.getSignature());
                        } catch (NoSuchMethodException ex) {
                            LOG.info("Proxy {} Method {} for signature not found. Trying with primitives.",
                                    getName(), desriptor.getName());
                            return clazz.getMethod(desriptor.getName(),
                                    ClassUtils.wrappersToPrimitives(desriptor.getSignature()));
                        }
                    }
                });

            if (desriptor.isPushClassLoader()) {
                Thread.currentThread().setContextClassLoader(this.clazz.getClassLoader());
            }

            if (desriptor.isStatic()) {
                return new MethodDesriptor.Result(desriptor, method.invoke(null, unwrapProxies(desriptor)));
            } else {
                return new MethodDesriptor.Result(desriptor, method.invoke(see(), unwrapProxies(desriptor)));
            }
        } catch (Exception e) {
            throw new ProxyException(this,
                    String.format("Failed to call method %s", desriptor.getName()), e);
        } finally {
            if (desriptor.isPushClassLoader()) {
                Thread.currentThread().setContextClassLoader(contextClassLoader);
            }
        }
    }

    private Class[] remap(Class[] signature) {
        if (ArrayUtils.isEmpty(signature)) {
            return signature;
        }

        // remap classes from the same class loader
        final ClassLoader loader = this.clazz.getClassLoader();
        return FluentIterable.of(signature)
                .transform(new Function<Class, Class>() {
                    @Override
                    public Class apply(Class input) {
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
                    }
                }).toArray(Class.class);

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
        return this.versionInfo.getLabel();
    }
}
