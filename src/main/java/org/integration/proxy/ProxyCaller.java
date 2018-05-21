package org.integration.proxy;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

public class ProxyCaller extends AbstractCaller {
    private final ProxyUtils.ProxyObject target;

    public ProxyCaller(ProxyUtils.ProxyObject target,
                       ProxyVersionedInterface versionInfo) {
        this(target, versionInfo, ImmutableMap.<String, MethodDesriptor.Result>of());
    }

    public ProxyCaller(ProxyUtils.ProxyObject target,
                       ProxyVersionedInterface versionInfo,
                       Map<String, MethodDesriptor.Result> data) {
        super(target.getClazz(), versionInfo, data);
        this.target = target;
    }

    @Override
    public Object see() {
        return target == null ? null : target.getObject();
    }
}
