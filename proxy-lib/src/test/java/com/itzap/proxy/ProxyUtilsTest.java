package com.itzap.proxy;

import com.itzap.proxy.model.ArtifactInterface;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;


public class ProxyUtilsTest {
    @Test
    public void artifactFromClass() {
        ArtifactInterface artifact = ProxyUtils.artifactFromClass(String.class);

        assertThat(artifact, notNullValue());
    }
}
