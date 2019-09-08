/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.security.common;

import java.util.Arrays;
import java.util.ListIterator;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.logging.Logger;
import org.wildfly.test.security.common.elytron.ConfigurableElement;

/**
 * Abstract parent for server setup tasks configuring Elytron. Implementing classes overrides {@link #getConfigurableElements()}
 * method to provide configured on which this task will call {@link ConfigurableElement#create(CLIWrapper)}.
 *
 * @author Josef Cacek
 */
public abstract class AbstractElytronSetupTask
        implements org.jboss.as.arquillian.api.ServerSetupTask {

    private static final Logger LOGGER = Logger.getLogger(AbstractElytronSetupTask.class);

    private ConfigurableElement[] configurableElements;
    private ManagementClient cachedManagementClient;

    @Override
    public void setup(final ManagementClient managementClient, final String containerId)
            throws Exception {
        // WFLY-12514. The overridable setup(ModelControllerClient) method used ServerReload.reloadIfRequired(ModelControllerClient)
        // but it's more robust to use ServerReload.reloadIfRequired(ManagementClient). To let it do that
        // without breaking compatibility, cache the ManagementClient
        this.cachedManagementClient = managementClient;
        setup(managementClient.getControllerClient());
        this.cachedManagementClient = null;
    }

    @Override
    public void tearDown(final ManagementClient managementClient, final String containerId)
            throws Exception {
        // WFLY-12514. The overridable setup(ModelControllerClient) method used ServerReload.reloadIfRequired(ModelControllerClient)
        // but it's more robust to use ServerReload.reloadIfRequired(ManagementClient). To let it do that
        // without breaking compatibility, cache the ManagementClient
        this.cachedManagementClient = managementClient;
        tearDown(managementClient.getControllerClient());
        this.cachedManagementClient = null;
    }

    /**
     * Creates configuration elements (provided by implementation of {@link #getConfigurableElements()} method) and calls
     * {@link ConfigurableElement#create(CLIWrapper)} for them.
     */
    protected void setup(final ModelControllerClient modelControllerClient) throws Exception {

        configurableElements = getConfigurableElements();

        if (configurableElements == null || configurableElements.length == 0) {
            LOGGER.warn("Empty Elytron configuration.");
            return;
        }

        try (CLIWrapper cli = new CLIWrapper(true)) {
            for (final ConfigurableElement configurableElement : configurableElements) {
                LOGGER.infov("Adding element {0} ({1})", configurableElement.getName(),
                        configurableElement.getClass().getSimpleName());
                configurableElement.create(modelControllerClient, cli);
            }
        }

        reloadIfRequired(modelControllerClient);
    }

    /**
     * Reverts configuration changes done by {@link #setup(ModelControllerClient)} method - i.e. calls {@link ConfigurableElement#remove(CLIWrapper)} method
     * on instances provided by {@link #getConfigurableElements()} (in reverse order).
     */
    protected void tearDown(ModelControllerClient modelControllerClient) throws Exception {
        if (configurableElements == null || configurableElements.length == 0) {
            LOGGER.warn("Empty Elytron configuration.");
            return;
        }

        try (CLIWrapper cli = new CLIWrapper(true)) {
            final ListIterator<ConfigurableElement> reverseConfigIt = Arrays.asList(configurableElements)
                    .listIterator(configurableElements.length);
            while (reverseConfigIt.hasPrevious()) {
                final ConfigurableElement configurableElement = reverseConfigIt.previous();
                LOGGER.infov("Removing element {0} ({1})", configurableElement.getName(),
                        configurableElement.getClass().getSimpleName());
                configurableElement.remove(modelControllerClient, cli);
            }
        }
        this.configurableElements = null;

        reloadIfRequired(modelControllerClient);
    }

    /**
     * Returns not-{@code null} array of configurations to be created by this server setup task.
     *
     * @return not-{@code null} array of instances to be created
     */
    protected abstract ConfigurableElement[] getConfigurableElements();

    private void reloadIfRequired(ModelControllerClient modelControllerClient) throws Exception {

        if (cachedManagementClient != null) {
            ServerReload.reloadIfRequired(cachedManagementClient);
        } else {
            // Some subclass must have overridden setup(ManagementClient, String) or tearDown(ManagementClient, String).
            // Probably ok; might hit WFLY-12514 type problems if the server isn't on the default address/port
            // in which case the fix is to correct the overriding code.
            //noinspection deprecation
            ServerReload.reloadIfRequired(modelControllerClient);
        }
    }

}
