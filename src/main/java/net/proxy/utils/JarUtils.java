package net.proxy.utils;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import net.proxy.model.ArtifactInterface;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class JarUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(JarUtils.class);

    private JarUtils() {}

    public static String getFileLocation(Class<?> clazz) {
        try {
            return URLDecoder.decode(clazz.getProtectionDomain().getCodeSource().getLocation().getFile(), "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }

    public static String getJarFile(Class<?> clazz) {
        String fileName = getFileLocation(clazz);
        if (isRunningJar(clazz)) {
            return new File(fileName).getAbsolutePath();
        } else {
            return null;
        }
    }

    public static boolean isRunningJar(Class<?> clazz) {
        String fileName = clazz.getProtectionDomain().getCodeSource().getLocation().getFile();
        return fileName != null && FilenameUtils.getExtension(fileName).equalsIgnoreCase("jar");
    }

    public static boolean isRunningJar() {
        return isRunningJar(JarUtils.class);
    }

    public static File getAppPath(Class<?> clazz) {
        String fileName = getJarFile(clazz);
        if (fileName != null && isRunningJar(clazz)) {
            return new File(fileName).getParentFile();
        } else {
            return new File(getFileLocation(clazz));
        }
    }

    public static List<URL> loadArtifactFromJar(ArtifactInterface artifact,
                                                Predicate<String> filter) {
        LOGGER.debug("Loading JAR artifact {}", artifact);

        File tempLibs = null;
        List<URL> urls = Lists.newArrayList();
        FileOutputStream tempStream = null;
        String jarFileName = getJarFile(filter.getClass());
        if (StringUtils.isBlank(jarFileName)) {
            LOGGER.info("Cannot load from jar file. Jar file is null");
            return urls;
        }

        try {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Loading from jar file {}", jarFileName);
            }
            JarFile thisJar = new JarFile(jarFileName);
            List<JarEntry> entries = Collections.list(thisJar.entries());
            for (JarEntry jarItem: entries) {
                if (filter.apply(jarItem.getName())) {
                    // extract all libs into a temp folder
                    if (tempLibs == null) {
                        tempLibs = artifact.getDestination(jarItem.getName());
                        FileUtils.forceMkdir(tempLibs);
                        if (artifact.isTemp()) {
                            FileUtils.forceDeleteOnExit(tempLibs);
                        }
                    }
                    File tempFile = new File(tempLibs, FilenameUtils.getName(jarItem.getName()));
                    if (!tempFile.exists()) {
                        InputStream libFile = thisJar.getInputStream(jarItem);
                        if (artifact.isTemp()) {
                            FileUtils.forceDeleteOnExit(tempFile);
                        }
                        LOGGER.info("Extracting lib {}", tempFile.getAbsolutePath());
                        tempStream = new FileOutputStream(tempFile);
                        IOUtils.copy(libFile, tempStream);
                        tempStream.close();
                    } else {
                        LOGGER.info("Lib {} is loaded", tempFile.getAbsolutePath());
                    }
                    urls.add(tempFile.toURI().toURL());
                }
            }

            return urls;
        } catch (Exception e) {
            if (tempLibs != null) {
                FileUtils.deleteQuietly(tempLibs);
            }
            LOGGER.warn("Failed to load artifact {} from jar {}. Application may not function properly",
                    artifact, getJarFile(filter.getClass()), e);
            return urls;
        } finally {
            IOUtils.closeQuietly(tempStream);
        }
    }
}
