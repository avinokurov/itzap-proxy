package net.proxy.lib.utils;

import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Maps;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

public final class DirUtils {
    private static final Pattern VERSION_SPLITTER = Pattern.compile("\\.|\\-");
    private static final Logger LOGGER = LoggerFactory.getLogger(DirUtils.class);
    private static final Map<String, Boolean> DIR_CACHE = Maps.newConcurrentMap();

    public static final File[] EMPTY_FILE_LIST = new File[0];

    private DirUtils() {}

    public static void forceMkdir(File file) throws IOException {
        if (isDirectory(file)) {
            FileUtils.forceMkdir(file);
        } else {
            FileUtils.forceMkdir(file.getParentFile());
        }
    }

    public static void forceMkdir(File file, boolean force) throws IOException {
        FileUtils.forceMkdir(file);
    }

    public static boolean isDirectory(File file) {
        if (file == null) {
            return false;
        }

        if (DIR_CACHE.containsKey(file.getAbsolutePath())) {
            return DIR_CACHE.get(file.getAbsolutePath());
        }

        if (file.exists()) {
            DIR_CACHE.put(file.getAbsolutePath(), file.isDirectory());
            return file.isDirectory();
        }

        boolean isVersion = FluentIterable.from(Splitter.on(VERSION_SPLITTER)
                .split(file.getName()))
                .limit(3)
                .allMatch(input -> NumberUtils.isDigits(input));
        if (isVersion) {
            DIR_CACHE.put(file.getAbsolutePath(), true);
            return true;
        }

        if (StringUtils.isBlank(FilenameUtils.getBaseName(file.getName()))) {
            // special folders that starts with period like .ssh
            DIR_CACHE.put(file.getAbsolutePath(), true);
            return true;
        }

        DIR_CACHE.put(file.getAbsolutePath(), FilenameUtils.getExtension(file.getName()).isEmpty());
        return DIR_CACHE.get(file.getAbsolutePath());
    }

    public static boolean isDirectory(String file) {
        if (file == null) {
            return false;
        }

        if (DIR_CACHE.containsKey(file)) {
            return DIR_CACHE.get(file);
        }

        return isDirectory(new File(file));
    }

    public static String getVersion(String path) {
        if (StringUtils.isBlank(path)) {
            return path;
        }

        return getVersion(new File(path));
    }

    public static String getVersion(File path) {
        if (path == null) {
            return StringUtils.EMPTY;
        }

        if (isDirectory(path)) {
            return FilenameUtils.getName(path.getAbsolutePath());
        }

        String ext = FilenameUtils.getExtension(path.getName());
        if (!ext.isEmpty() && NumberUtils.isDigits(ext)) {
            return FilenameUtils.getName(path.getAbsolutePath());
        }

        return FilenameUtils.getName(FilenameUtils.getPathNoEndSeparator(path.getAbsolutePath()));
    }
}
