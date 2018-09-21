package net.proxy.lib.utils;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.proxy.lib.ProxyException;
import net.proxy.lib.ProxyUtils;
import net.proxy.lib.model.ArtifactInterface;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.proxy.lib.utils.JarUtils.getJarFile;


public class LibLoader {
    private static final Map<String, URLClassLoader> LIB_CLASS_LOADER = Maps.newConcurrentMap();
    private static final Logger LOGGER = LoggerFactory.getLogger(LibLoader.class);

    public static URLClassLoader getLibClassLoader(ArtifactInterface lib) {
        return getLibClassLoader(lib, jarPredicate(lib.getRoot(),
                lib.getName(), lib.getExtensions(), lib.getPredicate()));
    }

    public static boolean containsLoader(String lib) {
        if (StringUtils.isBlank(lib)) {
            return false;
        }

        return LIB_CLASS_LOADER.containsKey(lib);
    }

    public static URLClassLoader getLibClassLoader(ArtifactInterface lib, Predicate<String> filter) {
        String libName = lib.getName();
        if (!LIB_CLASS_LOADER.containsKey(libName)) {
            synchronized (LibLoader.class) {
                if (!LIB_CLASS_LOADER.containsKey(libName)) {
                    LIB_CLASS_LOADER.put(libName, loadLibs(lib,
                            ObjectUtils.defaultIfNull(filter, lib.getPredicate())));
                }
            }
        }

        return LIB_CLASS_LOADER.get(libName);
    }

    public static void unloadAll() {
        LIB_CLASS_LOADER.clear();
    }

    private static URLClassLoader loadLibs(ArtifactInterface artifact, Predicate<String> filter) {
        List<URL> urls = null;
        try {
            ArtifactInterface currentArtifact = artifact;
            while(currentArtifact != null) {
                urls = currentArtifact.load();

                if ((urls == null || urls.isEmpty()) &&
                        currentArtifact.hasFallback()) {
                    currentArtifact = currentArtifact.getFallback();
                    LOGGER.info("Fallback to {}", currentArtifact);
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
                        return ProxyUtils.getUrlFromClass(input);
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
                                                 final String lib,
                                                 Set<String> extentions) {
        return jarPredicate(root, lib, extentions, null);
    }

    private static Predicate<String> jarPredicate(final String root,
                                                  final String lib,
                                                  final Set<String> extentions,
                                                  Predicate<String> defaultPredicate) {
        if (defaultPredicate != null) {
            return defaultPredicate;
        }

        return new Predicate<String>() {
            @Override
            public boolean apply(String input) {
                return isKeepArtifact(root, lib, input, extentions);
            }
        };
    }

    public static boolean isKeepArtifact(String root,
                                         String lib,
                                         String input,
                                         Set<String> extentions) {
        String testInput = FilenameUtils.separatorsToUnix(input);
        String testPath = FilenameUtils.separatorsToUnix(Joiner.on('/')
                .skipNulls().join(root, lib));
        String testRuntime = StringUtils.isBlank(root) ? FilenameUtils.separatorsToUnix("runtime/") :
                FilenameUtils.separatorsToUnix(root + "/runtime/");


        LOGGER.debug("Testing input {} for {} or runtime {}",
                    testInput, testPath, testRuntime);

        String ext = FilenameUtils.getExtension(input);
        return (StringUtils.containsIgnoreCase(testInput, testPath) &&
                extentions.contains(ext)) ||
                StringUtils.containsIgnoreCase(testInput, testRuntime);
    }

    public static List<URL> loadArtifactFromDir(final ArtifactInterface artifact, final Predicate<String> filter) {
        LOGGER.debug("Loading DIR artifact {}", artifact);

        File libDir = artifact.toPath();
        File targetLibDir = artifact.toParentPath();

        if ((!libDir.exists() || !libDir.isDirectory()) && !targetLibDir.exists()) {
            LOGGER.warn("Artifact='{}' is not found. Application may not function properly", artifact);
            return ImmutableList.of();
        }

        File[] libJarFiles = libDir.listFiles();
        File[] targetLibJars = targetLibDir.listFiles();

        if (ArrayUtils.isEmpty(libJarFiles) && ArrayUtils.isEmpty(targetLibJars)) {
            LOGGER.warn("No files found in {}. Application may not function properly",
                    libDir.getAbsolutePath());
            return ImmutableList.of();
        }

        File[] allFiles = ArrayUtils.addAll(libJarFiles, targetLibJars);
        try {
            return FluentIterable.of(allFiles)
                    .filter(new Predicate<File>() {
                        @Override
                        public boolean apply(File input) {
                            return input != null &&
                                    input.exists() &&
                                    "jar".equalsIgnoreCase(FilenameUtils.getExtension(input.getName())) &&
                                    filter.apply(input.getAbsolutePath());
                        }
                    })
                    .transform(new Function<File, URL>() {
                        @Override
                        public URL apply(File jarFile) {
                            try {
                                LOGGER.info("Adding file {}", jarFile.getAbsolutePath());
                                return jarFile.toURI().toURL();
                            } catch (MalformedURLException e) {
                                throw new ProxyException(artifact, String.format("Failed to get URL from a jar file=%s",
                                        jarFile.getAbsolutePath()));
                            }
                        }
                    }).toList();
        } catch (Exception e) {
            LOGGER.warn("Failed to load artifacts from dir {}. Application may not function properly",
                    libDir.getAbsolutePath(), e);
            return ImmutableList.of();
        }
    }
}
