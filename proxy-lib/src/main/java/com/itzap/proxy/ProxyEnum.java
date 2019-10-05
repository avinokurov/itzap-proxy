package com.itzap.proxy;

import com.google.common.collect.ImmutableMap;
import com.itzap.proxy.model.ArtifactInterface;

import static com.itzap.proxy.ProxyUtils.NULL_OBJECT;

public class ProxyEnum extends ProxyCaller {
    public static ProxyEnum NULL_ENUM_PROXY = new ProxyEnum(NULL_OBJECT, null) {
        @Override
        public boolean isNull() {
            return true;
        }
    };

    public ProxyEnum(ProxyUtils.ProxyObject target, ArtifactInterface artifact) {
        super(target, artifact, ImmutableMap.<String, MethodDesriptor.Result>of());
    }

    public String name() {
        return call("name").asString();
    }

    public long ordinal() {
        return call("ordinal").asLong();
    }

    public boolean isSameName(String name) {
        return name().equalsIgnoreCase(name);
    }
}
