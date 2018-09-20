package net.proxy;

import com.google.common.collect.ImmutableMap;
import net.proxy.model.ArtifactInterface;

import java.util.Map;

public class ProxyCaller extends AbstractCaller {
    private final ProxyUtils.ProxyObject target;

    public ProxyCaller(ProxyUtils.ProxyObject target,
                       ArtifactInterface artifact) {
        this(target, artifact, ImmutableMap.<String, MethodDesriptor.Result>of());
    }

    public ProxyCaller(ProxyUtils.ProxyObject target,
                       ArtifactInterface artifact,
                       Map<String, MethodDesriptor.Result> data) {
        super(target.getClazz(), artifact, data);
        this.target = target;
    }

    @Override
    public Object see() {
        return target == null ? null : target.getObject();
    }
}
