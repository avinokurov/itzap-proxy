package org.integration.proxy.utils.jar;

import com.google.common.base.Predicate;

import java.io.File;
import java.net.URL;
import java.util.List;

public interface ArtifactInterface {
    public enum SourceType {
        JAR, DIR, S3
    }

    String getRoot();

    String getName();

    String getExtension();

    File getDestination(String from);

    File toParentPath();

    File toPath();

    ArtifactInterface getFallback();

    boolean isTemp();

    SourceType getSourceType();

    Predicate<String> getPredicate();

    List<String> forClasses();

    boolean isUseSystemLoader();

    List<URL> loadUrls();
}
