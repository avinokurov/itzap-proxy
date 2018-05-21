package org.integration.proxy.utils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.File;
import java.io.IOException;

public final class LibUtils {
    public static final String APPEND_LIB_LOG = "lib.append.log";

    private LibUtils() {}

    public static void forceMkdir(File file) throws IOException {
        if (FilenameUtils.getExtension(file.getName()).isEmpty()) {
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

        if (file.exists()) {
            return file.isDirectory();
        }

        return FilenameUtils.getExtension(file.getName()).isEmpty();
    }

    public static boolean isDirectory(String file) {
        if (file == null) {
            return false;
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
