package com.redislabs.sa.ot.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertyFileFetcher {

    public static Properties loadProps(String propertyFileName){
        Properties p=null;
        InputStream inputStream = null;

        System.out.println("LOADING PROPERTIES FILE: "+propertyFileName+" USING CLASSLOADER...");
        inputStream = PropertyFileFetcher.class.getClassLoader().getResourceAsStream(propertyFileName);
        System.out.println("inputStream is now: "+inputStream);
        if(null != inputStream) {
            p = new Properties();
            try {
                p.load(inputStream);
                System.out.println("! --> CLASSLOADER LOADED PROPERTIES FILE...");
            } catch (IOException e) {
                System.out.println("! --> CLASSLOADER LOAD OF PROPERTIES FILES...FAILED");
                e.printStackTrace();
            }
        }
        return p;
    }

}
