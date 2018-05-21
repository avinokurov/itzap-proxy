package org.integration.proxy.utils.jar;

import com.google.common.base.MoreObjects;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.net.URL;
import java.util.List;

import static org.integration.proxy.utils.LibLoader.jarPredicate;
import static org.integration.proxy.utils.jar.JarUtils.isRunningJar;

public abstract class AbstractArtifact implements ArtifactInterface {
    private final String root;
    private final String name;
    private final String extension;
    private final Class<?> clazz;
    private final File destination;
    private final Predicate<String> predicate;

    protected AbstractArtifact(String fname, Class<?> clazz, File destination) {
        this(FilenameUtils.getPath(fname),
                FilenameUtils.getBaseName(fname), FilenameUtils.getExtension(fname),
                clazz, destination, jarPredicate(FilenameUtils.getPath(fname),
                        FilenameUtils.getBaseName(fname)));
    }

    protected AbstractArtifact(String root, String name, Class<?> clazz, File destination) {
        this(root, name, "", clazz, destination, jarPredicate(root, name));
    }

    protected AbstractArtifact(String root,
                            String name,
                            String extension,
                            Class<?> clazz,
                            File destination,
                            Predicate<String> predicate) {
        this.root = root;
        this.name = name;
        this.extension = extension;
        this.clazz = clazz;
        this.destination = destination;
        this.predicate = predicate;
    }

    @Override
    public File toPath() {
        URL resource = clazz.getResource("/" + getPath());
        if (resource == null) {
            return new File(getPath());
        }

        return new File(resource.getFile());
    }

    @Override
    public File toParentPath() {
        URL resource = clazz.getResource("/");
        if (resource == null) {
            return new File("/" + getPath());
        }

        return new File(
                new File(resource.getFile()).getParentFile(), getPath()
        );
    }

    @Override
    public String getRoot() {
        return root;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getExtension() {
        return extension;
    }

    @Override
    public File getDestination(String from) {
        return destination;
    }

    @Override
    public ArtifactInterface getFallback() {
        return null;
    }

    @Override
    public boolean isTemp() {
        return true;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("root", root)
                .add("name", name)
                .add("extension", extension)
                .add("destination", destination.getAbsolutePath())
                .add("sourceType", this.getSourceType())
                .toString();
    }

    @Override
    public SourceType getSourceType() {

        if (isRunningJar(this.predicate.getClass())) {
            return SourceType.JAR;
        }

        return SourceType.DIR;
    }

    protected String getPath() {
        return this.root + "/" + this.name;
    }

    @Override
    public Predicate<String> getPredicate() {
        return this.predicate;
    }

    @Override
    public List<String> forClasses() {
        return ImmutableList.of();
    }

    @Override
    public boolean isUseSystemLoader() {
        return true;
    }
}
