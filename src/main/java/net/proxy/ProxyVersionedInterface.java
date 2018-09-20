package net.proxy;

import java.io.File;

public interface ProxyVersionedInterface extends ProxyInterface {
    String getVersion();
    String getPath();
    File getDir();
}
