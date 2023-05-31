/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
