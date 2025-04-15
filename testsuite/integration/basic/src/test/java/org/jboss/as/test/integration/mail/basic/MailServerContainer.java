/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.mail.basic;

import java.util.List;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

public class MailServerContainer extends GenericContainer<MailServerContainer> {

    public MailServerContainer(String confPath) {
        // When updating the image version the startup.sh script forked into this testsuite should
        // be checked in case a sync is needed whilst retaining the lines added for this testsuite.
        super(DockerImageName.parse("apache/james:demo-3.8.2"));
        this.setExposedPorts(List.of(25, 110));
        this.waitStrategy = Wait.forLogMessage(".*AddUser command executed sucessfully.*", 3);
        // WFLY-20553 Copying files to a directory that is also defined as a volume in the image
        // was silently getting skipped on podman, this adds the files to a different location and
        // a replacement startup.sh copies them into place.
        this.withCopyFileToContainer(MountableFile.forHostPath(confPath + "testconf/"), "/root/testconf/");
        this.withCopyFileToContainer(MountableFile.forHostPath(confPath + "script/"), "/root/");
    }

    public String getMailServerHost() {
        return this.isRunning() ? this.getHost() : "localhost";
    }

    public Integer getSMTPMappedPort() {
        return this.isRunning() ? this.getMappedPort(25) : 1025;
    }

    public Integer getPOP3MappedPort() {
        return this.isRunning() ? this.getMappedPort(110) : 1110;
    }
}
