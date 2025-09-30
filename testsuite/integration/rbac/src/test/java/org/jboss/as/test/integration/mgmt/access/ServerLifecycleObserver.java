/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.mgmt.access;

import java.io.IOException;

import org.jboss.arquillian.container.spi.event.container.AfterStop;
import org.jboss.arquillian.container.spi.event.container.BeforeStart;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.as.test.module.util.TestModule;
import org.wildfly.test.installationmanager.TestInstallationManager;
import org.wildfly.test.installationmanager.TestInstallationManagerFactory;

/**
 * Installs a test implementation of the Installation Manager into the server during server startup,
 * see {@code org.wildfly.installationmanager.spi.InstallationManagerFactory} service.
 * <p>
 * If the service is found on the {@code org.wildfly.installation-manager.api} JBoss Module classpath,
 * the server registers the {@code [/host=*]/core-service=installer} management resource, making the
 * resource available for testing.
 * <p>
 * To disable this configuration, set the system property {@code installation.manager.services.install.skip}
 * to {@code true}. This is necessary for servers that do not support the Installation Manager,
 * such as RPM-based installations which are not managed by tools like Prospero.
 */
public class ServerLifecycleObserver {

    private static final String MODULE_NAME = "org.jboss.prospero";
    private TestModule testModule;
    private static final boolean INST_MGR_SRV_SKIP = Boolean.getBoolean("installation.manager.services.install.skip");

    public void containerBeforeStart(@Observes BeforeStart event) throws IOException {
        if (!INST_MGR_SRV_SKIP) {
            createTestModule();
        }
    }

    public void containerAfterStop(@Observes AfterStop event) {
        if (!INST_MGR_SRV_SKIP) {
            testModule.remove();
        }
    }

    private void createTestModule() throws IOException {
        testModule = new TestModule(MODULE_NAME, "org.wildfly.installation-manager.api");
        testModule.addResource("test-mock-installation-manager.jar").addClass(TestInstallationManager.class)
                .addClass(TestInstallationManagerFactory.class)
                .addAsManifestResource("META-INF/services/org.wildfly.installationmanager.spi.InstallationManagerFactory",
                        "services/org.wildfly.installationmanager.spi.InstallationManagerFactory");
        testModule.create(true);
    }
}
