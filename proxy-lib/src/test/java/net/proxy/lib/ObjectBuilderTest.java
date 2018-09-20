package net.proxy.lib;

import net.proxy.lib.model.DirArtifact;
import org.junit.Test;

import java.io.File;

import static net.proxy.lib.ProxyUtils.newVersionInfo;


public class ObjectBuilderTest {
    @Test
    public void buildTest() {
        // load test lib
        String libPath = ObjectBuilderTest.class.getResource("/").getPath();
        ProxyCallerInterface caller = ObjectBuilder.builder()
                .setClassName("net.proxy.lib.test.LibClass")
                .setArtifact(DirArtifact.builder()
                        .withClazz(ObjectBuilderTest.class)
                        .withVersionInfo(newVersionInfo(new File(libPath)))
                        .build())
                .build();
    }
}
