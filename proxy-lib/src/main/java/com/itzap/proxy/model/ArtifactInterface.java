package com.itzap.proxy.model;

import com.google.common.base.Predicate;
import com.itzap.proxy.ProxyInterface;
import com.itzap.proxy.ProxyVersionedInterface;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Set;

public interface ArtifactInterface extends ProxyInterface {
    public enum SourceType {
        JAR, DIR, S3, UNKNOWN
    }

    String getRoot();

    Set<String> getExtensions();

    File getDestination(String from);

    File toParentPath();

    File toPath();

    boolean hasFallback();

    ArtifactInterface getFallback();

    boolean isTemp();

    SourceType getSourceType();

    Predicate<String> getPredicate();

    List<String> forClasses();

    boolean isUseSystemLoader();

    LibCallback getCallback();

    List<URL> load();

    ProxyVersionedInterface getVersion();

    ArtifactInterface fromVersion(ProxyVersionedInterface version);

    interface Builder<B extends Builder, T extends ArtifactInterface> {
        B getThis();
        B setVersionInfo(ProxyVersionedInterface versionInfo);
        T build();
    }
}
