package org.integration.proxy.utils;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.integration.proxy.utils.jar.ArtifactInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.integration.proxy.utils.LibUtils.APPEND_LIB_LOG;
import static org.integration.proxy.utils.jar.JarUtils.getUrlFromClass;


public class LibLoader {
    private static final Map<String, URLClassLoader> LIB_CLASS_LOADER = Maps.newConcurrentMap();
    private static final Logger LOGGER = LoggerFactory.getLogger(LibLoader.class);

    public static URLClassLoader getLibClassLoader(ArtifactInterface lib) {
        return getLibClassLoader(lib, jarPredicate(lib.getRoot(), lib.getName(), lib.getPredicate()));
    }

    public static boolean containsLoader(String lib) {
        return LIB_CLASS_LOADER.containsKey(lib);
    }

    public static URLClassLoader getLibClassLoader(ArtifactInterface lib, Predicate<String> filter) {
        if (!LIB_CLASS_LOADER.containsKey(lib.getName())) {
            synchronized (LibLoader.class) {
                if (!LIB_CLASS_LOADER.containsKey(lib.getName())) {
                    LIB_CLASS_LOADER.put(lib.getName(), loadLibs(lib,
                            ObjectUtils.defaultIfNull(filter, lib.getPredicate())));
                }
            }
        }

        return LIB_CLASS_LOADER.get(lib.getName());
    }

    public static void unloadAll() {
        LIB_CLASS_LOADER.clear();
    }

    private static URLClassLoader loadLibs(ArtifactInterface artifact, Predicate<String> filter) {
        System.setProperty(APPEND_LIB_LOG, "true");

        List<URL> urls = null;
        try {
            ArtifactInterface currentArtifact = artifact;
            Predicate<String> curFilter = currentArtifact == null ? filter :
                    ObjectUtils.defaultIfNull(filter, currentArtifact.getPredicate());
            while(currentArtifact != null) {
                urls = currentArtifact.loadUrls();
                if ((urls == null || urls.isEmpty()) &&
                        currentArtifact.getFallback() != null) {
                    LOGGER.info("Fallback to {}", currentArtifact.getFallback());
                    currentArtifact = currentArtifact.getFallback();
                    curFilter = currentArtifact.getPredicate();
                } else {
                    break;
                }
            }

            if (!artifact.forClasses().isEmpty()) {
                List<URL> tempUrls = urls != null ?
                        Lists.newArrayList(urls) : new ArrayList<URL>();
                tempUrls.addAll(loadForClasses(artifact.getClass(), artifact.forClasses()));
                urls = tempUrls;
            }

            if (urls == null || urls.isEmpty()) {
                LOGGER.warn("Did not find any lib URLs for lib={}. Application may not function properly", artifact.getName());
                return new URLClassLoader(new URL[]{});
            }

            if (artifact.isUseSystemLoader()) {
                return URLClassLoader.newInstance(urls.toArray(new URL[0]));
            } else {
                return URLClassLoader.newInstance(urls.toArray(new URL[0]), ClassLoader.getSystemClassLoader().getParent());
            }
        } catch (Exception ex) {
            LOGGER.warn("Failed to load libraries. Application may not function properly", ex);
            return new URLClassLoader(new URL[]{});
        }
    }

    private static List<URL> loadForClasses(final Class clazz, List<String> classes) {
        return FluentIterable.from(classes)
                .transform(new Function<String, Class>() {
                    @Override
                    public Class apply(String input) {
                        try {
                            return clazz.getClassLoader().loadClass(input);
                        } catch (ClassNotFoundException e) {
                            LOGGER.error("Failed to load class {}", input);
                            return null;
                        }
                    }
                })
                .transform(new Function<Class, URL>() {
                    @Override
                    public URL apply(Class input) {
                        return getUrlFromClass(input);
                    }
                })
                .filter(new Predicate<URL>() {
                    @Override
                    public boolean apply(URL input) {
                        return input != null;
                    }
                })
                .toList();
    }

    public static Predicate<String> jarPredicate(final String root,
                                                 final String lib) {
        return jarPredicate(root, lib, null);
    }

    private static Predicate<String> jarPredicate(final String root,
                                                 final String lib,
                                                 Predicate<String> defaultPredicate) {
        if (defaultPredicate != null) {
            return defaultPredicate;
        }

        return new Predicate<String>() {
            @Override
            public boolean apply(String input) {
                return isKeepArtifact(root, lib, input);
            }
        };
    }

    public static boolean isKeepArtifact(String root, String lib, String input) {
        String testInput = FilenameUtils.separatorsToUnix(input);
        String testPath = FilenameUtils.separatorsToUnix(Joiner.on('/')
                .skipNulls().join(root, lib));
        String testRuntime = FilenameUtils.separatorsToUnix(root + "/runtime/");


        LOGGER.debug("Testing input {} for {} or runtime {}",
                    testInput, testPath, testRuntime);

        return (StringUtils.containsIgnoreCase(testInput, testPath) &&
                StringUtils.endsWithIgnoreCase(input, ".jar")) ||
                StringUtils.containsIgnoreCase(testInput, testRuntime);
    }
}
