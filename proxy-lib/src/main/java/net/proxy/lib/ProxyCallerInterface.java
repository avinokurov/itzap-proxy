package net.proxy.lib;

import net.proxy.lib.model.ArtifactInterface;

public interface ProxyCallerInterface extends ProxyVersionedInterface {
    MethodDesriptor.Result call(String name);
    MethodDesriptor.Result data(String name);
    MethodDesriptor.Result call(String name, Object ... params);
    MethodDesriptor.Result call(MethodDesriptor desriptor);
    MethodDesriptor.Result call(MethodDesriptor desriptor, Object ... params);

    Object see();
    Class myClass();
    boolean isInstanceOf(String name);
    boolean isNull();
    ArtifactInterface getArtifact();
}
