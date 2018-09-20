package net.proxy.lib.model;

import com.google.common.base.Predicate;
import net.proxy.lib.ProxyVersionedInterface;
import net.proxy.lib.utils.LibLoader;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Set;

public class DirArtifact extends AbstractArtifact {
    private DirArtifact(Builder builder) {
        super(builder.root, builder.name, builder.extensions, builder.clazz,
                builder.destination, builder.predicate, builder.versionInfo);
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

    public static class Builder extends AbstractArtifact.Builder<DirArtifact, Builder> {

        @Override
        protected Builder getThis() {
            return this;
        }

        @Override
        public DirArtifact build() {
            return new DirArtifact(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
