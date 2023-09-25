/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.manualmode.ee.globaldirectory.libraries;

import java.io.File;

/**
 * @author Tomas Terem (tterem@redhat.com)
 **/
public class GlobalDirectoryLibraryImpl3 implements GlobalDirectoryLibrary {

    public String get() {
        return new File(GlobalDirectoryLibraryImpl3.class.getProtectionDomain().getCodeSource().getLocation().getFile()).getAbsolutePath();
    }
}
