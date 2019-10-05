package com.itzap.proxy;

import org.apache.commons.lang3.StringUtils;

public class ProxyException extends RuntimeException {
    private final ProxyInterface proxy;
    private static final String MESSAGE = "Failed to invoke method %s with label %s";
    private static final String UNKNOWN = "unknown";

    public ProxyException(ProxyInterface proxy, Throwable ex) {
        super(String.format(MESSAGE,
                StringUtils.defaultIfBlank(proxy.getName(), UNKNOWN),
                StringUtils.defaultIfBlank(proxy.getLabel(), UNKNOWN)), ex);
        this.proxy = proxy;
    }

    public ProxyException(ProxyInterface proxy, String message, Throwable ex) {
        super(String.format(MESSAGE + ". Message: %s",
                StringUtils.defaultIfBlank(proxy.getName(), UNKNOWN),
                StringUtils.defaultIfBlank(proxy.getLabel(), UNKNOWN), message), ex);
        this.proxy = proxy;
    }

    public ProxyException(ProxyInterface proxy, String message) {
        super(String.format(MESSAGE + ". Message: %s",
                StringUtils.defaultIfBlank(proxy.getName(), UNKNOWN),
                StringUtils.defaultIfBlank(proxy.getLabel(), UNKNOWN), message));
        this.proxy = proxy;
    }

    public ProxyInterface getProxy() {
        return proxy;
    }
}
