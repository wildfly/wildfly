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
 * Observers container lifecycle events to prepare the server with an test implementation of an installation manager.
 */
public class ServerLifecycleObserver {

    private static final String MODULE_NAME = "org.jboss.prospero";
    private TestModule testModule;

    public void containerBeforeStart(@Observes BeforeStart event) throws IOException {
        createTestModule();
    }

    public void containerAfterStop(@Observes AfterStop event) throws IOException {
        testModule.remove();
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
