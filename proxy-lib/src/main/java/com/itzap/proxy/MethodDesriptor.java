package com.itzap.proxy;

import com.google.common.base.*;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.itzap.proxy.model.ArtifactInterface;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MethodDesriptor implements ProxyInterface {
    private final String name;
    private final ProxyUtils.ProxyObject[] params;
    private final boolean is_static;
    private final Class[] signature;
    private final boolean pushClassLoader;

    private MethodDesriptor(String name, Object ... params) {
        this.name = name;
        this.params = ProxyUtils.unwrapObjects(params);
        this.is_static = false;
        this.signature = ProxyUtils.getClasses(params);
        this.pushClassLoader = false;
    }

    private MethodDesriptor(Builder builder) {
        this.name = builder.name;
        this.params = builder.params;
        this.is_static = builder.is_static;
        this.signature = builder.signature;
        this.pushClassLoader = builder.pushClassLoader;
    }

    public ProxyUtils.ProxyObject[] getParams() {
        return params;
    }

    public boolean isStatic() {
        return is_static;
    }

    public boolean isPushClassLoader() {
        return pushClassLoader;
    }

    public Class[] getSignature() {
        return signature;
    }

    private static String fullName(String name, Class[] params) {
        return Joiner.on('/')
                .join(name, Joiner.on('/')
                        .skipNulls()
                        .join(Stream.of(params).map(new java.util.function.Function<Class, String>() {
                                    @Override
                                    public String apply(Class input) {
                                        return input == null ? null : input.getName();
                                    }
                                }).collect(Collectors.toList())
                        )
                );
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getLabel() {
        return this.name;
    }

    public String getFullName() {
        return fullName(this.name, this.signature);
    }

    public static MethodDesriptor method(String name, Object ... params) {
        return new MethodDesriptor(name, params);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private ProxyUtils.ProxyObject[] params;
        private boolean is_static;
        private Class[] signature;
        private boolean pushClassLoader;

        private Builder() {}

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setParams(Object ... params) {
            this.params = ProxyUtils.unwrapObjects(params);
            return this;
        }

        public Builder setParams(ProxyUtils.ProxyObject[] params) {
            this.params = params;
            return this;
        }

        public Builder setStatic(boolean is_static) {
            this.is_static = is_static;
            return this;
        }

        public Builder setPushClassLoader(boolean pushClassLoader) {
            this.pushClassLoader = pushClassLoader;
            return this;
        }

        public Builder setSignature(Class ... signature) {
            this.signature = signature;
            return this;
        }

        public MethodDesriptor build() {
            if (ArrayUtils.isEmpty(this.signature)) {
                this.signature = ProxyUtils.getClasses(this.params);
            }
            Preconditions.checkNotNull(this.name, "Method name cannot be null");

            return new MethodDesriptor(this);
        }
    }

    public static class Result implements ProxyVersionedInterface {
        private final CachedMethod method;
        private final Object result;
        private final ArtifactInterface artifact;

        Result(CachedMethod method, Object result) {
            this.method = method;
            this.result = result;
            this.artifact = method == null || method.getArtifact() == null ?
                    ProxyUtils.versionFromObject(result) : method.getArtifact();
        }

        @Override
        public String getName() {
            return artifact.getVersion().getName();
        }

        @Override
        public String getLabel() {
            return this.artifact.getVersion().getLabel();
        }

        @Override
        public String getPath() {
            return this.artifact.getVersion().getPath();
        }

        @Override
        public String getVersion() {
            return this.artifact.getVersion().getVersion();
        }

        @Override
        public File getDir() {
            return this.artifact.getVersion().getDir();
        }

        public Object getResult() {
            return result;
        }

        public Result rerun(ProxyUtils.ProxyObject ... params) {
            if (this.method == null) {
                return new Result(method, null);
            } else {
                return this.method.makeCall(params);
            }
        }

        public CachedMethod getMethod() {
            return this.method;
        }

        public Long asLong() {
            if (!(this.result instanceof Number)) {
                return 0L;
            } else {
                return Number.class.cast(this.result).longValue();
            }
        }

        public boolean isNull() {
            return this.result == null;
        }

        @SuppressWarnings("unchecked")
        public List<ProxyCallerInterface> asList() {
            if (this.result == null) {
                return ImmutableList.of();
            } else if (this.result instanceof Iterable) {
                return FluentIterable.<ProxyCallerInterface>from((Iterable) this.result)
                        .transform((Function<Object, ProxyCallerInterface>) input -> new ProxyCaller(ProxyUtils.unwrapObject(input), artifact))
                        .toList();
            }

            return ImmutableList.of();

        }

        public String asString() {
            if (this.result == null) {
                return StringUtils.EMPTY;
            } else {
                return String.valueOf(this.result);
            }
        }

        public static String toString(Object runtimeObject) {
            return new ProxyCaller(ProxyUtils.unwrapObject(runtimeObject),
                    ProxyUtils.fromObject(runtimeObject)).call("toString").asString();
        }

        public ProxyEnum asEnum() {
            if (this.result == null) {
                return new ProxyEnum(null, this.artifact);
            }

            return new ProxyEnum(ProxyUtils.unwrapObject(this.result), this.artifact);
        }

        public ProxyCallerInterface asProxy() {
            if (this.result == null) {
                return new ProxyCaller(null, this.artifact);
            }

            return new ProxyCaller(ProxyUtils.unwrapObject(this.result), this.artifact);
        }

        public boolean sameAs(ProxyCallerInterface other) {
            if (this.result == null && (other == null || other.see() == null)) {
                return true;
            } else if (this.result == null) {
                return false;
            }

            return this.result.equals(other.see());
        }

        public boolean asBool() {
            if (this.result == null) {
                return false;
            }

            if (boolean.class.isInstance(this.result)) {
                return boolean.class.cast(this.result);
            }

            if (Boolean.class.isInstance(this.result)) {
                return Boolean.class.cast(this.result);
            }

            return false;
        }

        public File asFile() {
            if (!(this.result instanceof File)) {
                return new File(".");
            } else {
                return File.class.cast(this.result);
            }
        }

        public Map<String, String> asMap() {
            if (!(this.result instanceof Map)) {
                return ImmutableMap.of();
            }
            ImmutableMap.Builder<String, String> resultMap = ImmutableMap.builder();
            for(Object e: Map.class.cast(this.result).entrySet()) {
                Map.Entry entry = (Map.Entry)e;
                resultMap.put(toString(entry.getKey()), toString(entry.getValue()));
            }
            return resultMap.build();
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", fullName(name, this.signature))
                .add("static", isStatic())
                .add("label", getLabel())
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MethodDesriptor)) return false;
        MethodDesriptor that = (MethodDesriptor) o;
        return is_static == that.is_static &&
                Objects.equal(name, that.name) &&
                compareSignature(signature, that.signature);
    }

    @Override
    public int hashCode() {
        int sig = Objects.hashCode((Object[]) FluentIterable.of(signature)
                .transform(input -> input.getName()).toArray(String.class));

        return Objects.hashCode(name, is_static, sig);
    }

    private static boolean compareSignature(Class[] s1, Class[] s2) {
        if (s1 == s2) {
            return true;
        } else if (s1 == null || s2 == null) {
            return false;
        } else if (s1.length != s2.length) {
            return false;
        }

        Map<String, Class> lookup = new HashMap<>();
        for (Class s: s1) {
            lookup.put(s.getName(), s);
        }
        for (Class s: s2) {
            if (!lookup.containsKey(s.getName())) {
                return false;
            }
        }

        return true;
    }
}
