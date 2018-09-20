package net.proxy.utils;

import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.File;
import java.util.regex.Pattern;

public final class DirUtils {
    private static final Pattern VERSION_SPLITTER = Pattern.compile("\\.|\\-");

    private DirUtils() {}

    public static boolean isDirectory(File file) {
        if (file == null) {
            return false;
        }

        if (file.exists()) {
            return file.isDirectory();
        }

        boolean isVersion = FluentIterable.from(Splitter.on(VERSION_SPLITTER)
                .split(file.getName()))
                .limit(3)
                .allMatch(new Predicate<String>() {
                    @Override
                    public boolean apply(String input) {
                        return NumberUtils.isDigits(input);
                    }
                });
        if (isVersion) {
            return true;
        }

        if (StringUtils.isBlank(FilenameUtils.getBaseName(file.getName()))) {
            // special folders that starts with period like .ssh
            return true;
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
