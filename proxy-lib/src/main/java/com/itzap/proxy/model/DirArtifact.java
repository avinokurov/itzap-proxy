package com.itzap.proxy.model;

import com.google.common.collect.ImmutableSet;
import com.itzap.proxy.ProxyVersionedInterface;
import com.itzap.proxy.utils.LibLoader;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.net.URL;
import java.util.List;

import static com.itzap.proxy.utils.LibLoader.jarPredicate;

public class DirArtifact extends AbstractArtifact {
    private DirArtifact(Builder builder) {
        super(builder.root,
                builder.name, builder.extensions, builder.clazz,
                builder.destination,
                builder.predicate, builder.versionInfo);
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
            this.root = StringUtils.defaultIfBlank(this.root, this.versionInfo.getDir().getAbsolutePath());
            this.name = StringUtils.defaultIfBlank(this.name, this.versionInfo.getPath());
            this.extensions = ObjectUtils.defaultIfNull(this.extensions, ImmutableSet.of("jar"));

            this.predicate = ObjectUtils.defaultIfNull(this.predicate, jarPredicate(this.root, this.name, this.extensions));

            return new DirArtifact(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
