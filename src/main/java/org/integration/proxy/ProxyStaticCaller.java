package org.integration.proxy;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

public class ProxyStaticCaller extends AbstractCaller {
    public ProxyStaticCaller(Class clazz,
                             ProxyVersionedInterface versionInfo) {
        this(clazz, versionInfo, ImmutableMap.<String, MethodDesriptor.Result>of());
    }

    public ProxyStaticCaller(Class clazz,
                             ProxyVersionedInterface versionInfo,
                             Map<String, MethodDesriptor.Result> data) {
        super(clazz, versionInfo, data);
    }

    @Override
    public Object see() {
        return null;
    }
}
