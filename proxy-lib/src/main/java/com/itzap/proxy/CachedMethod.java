package com.itzap.proxy;

import com.itzap.proxy.model.ArtifactInterface;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;

import static com.itzap.proxy.ProxyUtils.unwrapProxies;

public class CachedMethod implements ProxyInterface {
    private final Object source;
    private final Class sourceClass;
    private final Method method;
    private final MethodDesriptor descriptor;
    private final ArtifactInterface artifact;

    CachedMethod(Object source, Class sourceClass, Method method,
                 MethodDesriptor descriptor, ArtifactInterface artifact) {
        this.source = source;
        this.sourceClass = sourceClass;
        this.method = method;
        this.descriptor = descriptor;
        this.artifact = artifact;
    }

    MethodDesriptor.Result makeCall(ProxyUtils.ProxyObject... params) {
        return makeCall(this.source, params);
    }

    public MethodDesriptor.Result makeCall(Object source, ProxyUtils.ProxyObject... params) {
        if (descriptor == null || StringUtils.isBlank(descriptor.getName()) || method == null) {
            return new MethodDesriptor.Result(this, null);
        }

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            if (this.descriptor.isPushClassLoader()) {
                Thread.currentThread().setContextClassLoader(this.sourceClass.getClassLoader());
            }

            if (this.descriptor.isStatic()) {
                return new MethodDesriptor.Result(this, method.invoke(null, ProxyUtils.unwrapProxies(params)));
            } else {
                return new MethodDesriptor.Result(this, method.invoke(source, ProxyUtils.unwrapProxies(params)));
            }

        } catch (Exception e) {
            throw new ProxyException(this,
                    String.format("Failed to call method %s", this.getName()), e);
        } finally {
            if (this.descriptor.isPushClassLoader()) {
                Thread.currentThread().setContextClassLoader(contextClassLoader);
            }
        }
    }

    public ArtifactInterface getArtifact() {
        return artifact;
    }

    @Override
    public String getName() {
        return this.descriptor == null ? "unknown" : this.descriptor.getName();
    }

    @Override
    public String getLabel() {
        return this.descriptor == null ? "unknwon" : this.descriptor.getLabel();
    }
}
