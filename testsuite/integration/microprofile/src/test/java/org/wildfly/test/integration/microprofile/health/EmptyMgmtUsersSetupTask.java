/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.health;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;

import java.io.File;
import java.nio.file.Files;

public class EmptyMgmtUsersSetupTask implements ServerSetupTask {

    static File mgmtUsersFile;
    byte[] bytes;

    static {
        String jbossHome = System.getProperty("jboss.home", null);
        if (jbossHome == null) {
            throw new IllegalStateException("jboss.home not set");
        }
        mgmtUsersFile = new File(jbossHome + File.separatorChar + "standalone" + File.separatorChar
                + "configuration" + File.separatorChar + "mgmt-users.properties");

        if (!mgmtUsersFile.exists()) {
            throw new IllegalStateException("Determined mgmt-users.properties path " + mgmtUsersFile + " does not exist");
        }
        if (!mgmtUsersFile.isFile()) {
            throw new IllegalStateException("Determined mgmt-users.properties path " + mgmtUsersFile + " is not a file");
        }
    }

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        bytes = Files.readAllBytes(mgmtUsersFile.toPath());
        Files.write(mgmtUsersFile.toPath(), "".getBytes());
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        Files.write(mgmtUsersFile.toPath(), bytes);
    }
}
