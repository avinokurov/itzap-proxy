package com.itzap.proxy.test;

public class LibClass {
    public String getLibVersion() {
        return "1.0";
    }

    public static String sayHi(String name) {
        return String.format("Hi %s!", name);
    }
}
