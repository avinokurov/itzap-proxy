package com.itzap.proxy;

import com.itzap.proxy.model.DirArtifact;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


public class ObjectBuilderTest {
    @Test
    public void buildTest() {
        // load test lib
        String libPath = ObjectBuilderTest.class.getResource("/").getPath();
        File libDir = new File(libPath + "../../../test-lib/target/1.0");

        ProxyCallerInterface caller = ObjectBuilder.builder()
                .setClassName("com.itzap.proxy.test.LibClass")
                .setArtifact(DirArtifact.builder()
                        .withClazz(ObjectBuilderTest.class)
                        .withVersionInfo(ProxyUtils.newVersionInfo(libDir))
                        .build())
                .build();
        String version = caller.call("getLibVersion").asString();
        assertThat(version, is("1.0"));
    }
}
