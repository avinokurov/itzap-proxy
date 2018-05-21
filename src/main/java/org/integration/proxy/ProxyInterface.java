package org.integration.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface ProxyInterface {

    Logger LOG = LoggerFactory.getLogger(ProxyInterface.class);

    abstract class Getter<T> {
        private final String name;

        protected Getter(String name) {
            this.name = name;
        }

        abstract protected T get(Class<?> src) throws Exception;
        T wrapedGet(Class<?> src) {
            try {
                return get(src);
            } catch (Exception ex) {
                LOG.error("Failed to get", ex);
                return null;
            }
        }

        String getName() {
            return this.name;
        }
    }

    String getName();

    String getLabel();
}
