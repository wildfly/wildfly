/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.manualmode.ee.globaldirectory.libraries;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author Tomas Terem (tterem@redhat.com)
 **/
public class GlobalDirectoryLibraryImpl2 implements GlobalDirectoryLibrary {

    public String get() {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        String res = null;
        try (InputStream is = classloader.getResourceAsStream("properties.txt")) {
            try(BufferedReader in = new BufferedReader(new InputStreamReader(is))) {
                res = in.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }
}
