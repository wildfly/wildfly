/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.smoke.provisioning;

import java.io.File;
import java.nio.file.Path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * Confirm expected files are present in the test installation regardless of how
 * different testsuite execution modes impact how that installation is provisioned.
 */
public class InstallationProvisioningTestCase {

    @Test
    public void testVersiontxt() {
        checkFileExists("version.txt");
    }

    private void checkFileExists(String... relativePath) {
        String jbossHome = System.getProperty("jboss.home");
        Assertions.assertTrue(jbossHome.contains("smoke"), jbossHome + " is not local to the smoke testsuite");
        File file = Path.of(jbossHome, relativePath).toFile();
        Assertions.assertTrue(file.exists(), file + " does not exist");
        Assertions.assertFalse(file.isDirectory(), file + " is a directory");
    }

}
