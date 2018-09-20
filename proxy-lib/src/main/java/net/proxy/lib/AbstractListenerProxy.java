package net.proxy.lib;

import com.google.common.base.Objects;
import net.proxy.lib.model.LibCallback;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public abstract class AbstractListenerProxy implements InvocationHandler {
    protected final LibCallback callback;
    protected final ProxyVersionedInterface versionInfo;
    protected final String id;

    protected AbstractListenerProxy(LibCallback callback,
                                    ProxyVersionedInterface versionInfo,
                                    String id) {
        this.callback = callback;
        this.versionInfo = versionInfo;
        this.id = id;
    }

    protected abstract Object processMethod(Object proxy, Method method, Object[] args) throws Throwable;

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (ProxyUtils.isMethod(method, "hashCode")) {
            return Objects.hashCode(callback,
                    id, versionInfo);
        }

        if (ProxyUtils.isMethod(method, "equal")) {
            return Objects.equal(args[0], args[1]);
        }

        return processMethod(proxy, method, args);
    }
}
