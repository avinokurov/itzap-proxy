package net.proxy.lib.model;

import com.google.common.base.Predicate;
import net.proxy.lib.ProxyVersionedInterface;

import java.io.File;
import java.util.List;
import java.util.Set;

public abstract class AbstractArtifactWrapper implements ArtifactInterface {

    public abstract ArtifactInterface start();

    protected abstract ArtifactInterface getArtifact();

    @Override
    public String getRoot() {
        return getArtifact().getRoot();
    }

    @Override
    public String getName() {
        return getArtifact().getName();
    }

    @Override
    public Set<String> getExtensions() {
        return getArtifact().getExtensions();
    }

    @Override
    public File getDestination(String from) {
        return getArtifact().getDestination(from);
    }

    @Override
    public File toParentPath() {
        return getArtifact().toParentPath();
    }

    @Override
    public File toPath() {
        return getArtifact().toPath();
    }

    @Override
    public boolean isTemp() {
        return getArtifact().isTemp();
    }

    @Override
    public SourceType getSourceType() {
        return getArtifact().getSourceType();
    }

    @Override
    public Predicate<String> getPredicate() {
        return getArtifact().getPredicate();
    }

    @Override
    public List<String> forClasses() {
        return getArtifact().forClasses();
    }

    @Override
    public boolean isUseSystemLoader() {
        return getArtifact().isUseSystemLoader();
    }

    @Override
    public LibCallback getCallback() {
        return getArtifact().getCallback();
    }

    @Override
    public String toString() {
        return getArtifact().toString();
    }

    @Override
    public ProxyVersionedInterface getVersion() {
        return getArtifact().getVersion();
    }
}
