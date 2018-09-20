package net.proxy.model;

import java.io.File;

public interface LibCallback {
    String getLibVersion();

    File getLibFolder();

    void setLength(long length);

    void start();
    void stop();

    long getLength() ;

    void tell(String origin, String destination, long pos);
}
