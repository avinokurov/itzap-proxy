package net.proxy.model;

import com.google.common.base.Predicate;
import net.proxy.ProxyVersionedInterface;
import net.proxy.utils.LibLoader;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Set;

public class DirArtifact extends AbstractArtifact {
    public DirArtifact(Set<String> extensions,
                       Class<?> clazz,
                       Predicate<String> predicate,
                       ProxyVersionedInterface versionInfo) {
        this(versionInfo.getPath(),
                versionInfo.getName(), extensions, clazz, versionInfo.getDir(),
                predicate, versionInfo);
    }

    public DirArtifact(String root,
                       String name,
                       Set<String> extensions,
                       Class<?> clazz,
                       File destination,
                       Predicate<String> predicate,
                       ProxyVersionedInterface versionInfo) {
        super(root, name, extensions, clazz, destination, predicate, versionInfo);
    }

    @Override
    public SourceType getSourceType() {
        return SourceType.DIR;
    }

    @Override
    public LibCallback getCallback() {
        return null;
    }

    @Override
    public List<URL> load() {
        return LibLoader.loadArtifactFromDir(this, this.predicate);
    }

    @Override
    public ArtifactInterface fromVersion(ProxyVersionedInterface version) {
        return null;
    }

    @Override
    public String getLabel() {
        return getVersion().getLabel();
    }
}
