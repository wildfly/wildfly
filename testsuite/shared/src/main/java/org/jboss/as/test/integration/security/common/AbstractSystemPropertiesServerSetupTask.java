/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.security.common;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

/**
 * {@link ServerSetupTask} instance for system properties setup.
 *
 * @author Josef Cacek
 */
public abstract class AbstractSystemPropertiesServerSetupTask implements ServerSetupTask {

    private static final Logger LOGGER = Logger.getLogger(AbstractSystemPropertiesServerSetupTask.class);

    private SystemProperty[] systemProperties;

    // Public methods --------------------------------------------------------

    public final void setup(final ManagementClient managementClient, String containerId) throws Exception {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.trace("Adding system properties.");
        }
        systemProperties = getSystemProperties();
        if (systemProperties == null || systemProperties.length == 0) {
            LOGGER.warn("No system property configured in the ServerSetupTask");
            return;
        }

        final List<ModelNode> updates = new ArrayList<ModelNode>();

        for (SystemProperty systemProperty : systemProperties) {
            final String propertyName = systemProperty.getName();
            if (propertyName == null || propertyName.trim().length() == 0) {
                LOGGER.warn("Empty property name provided.");
                continue;
            }
            ModelNode op = new ModelNode();
            op.get(OP).set(ADD);
            op.get(OP_ADDR).add(SYSTEM_PROPERTY, propertyName);
            op.get(ModelDescriptionConstants.VALUE).set(systemProperty.getValue());
            updates.add(op);
        }
        CoreUtils.applyUpdates(updates, managementClient.getControllerClient());
    }

    /**
     *
     * @param managementClient
     * @param containerId
     * @see org.jboss.as.test.integration.security.common.AbstractSecurityDomainSetup#tearDown(org.jboss.as.arquillian.container.ManagementClient,
     *      java.lang.String)
     */
    public final void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.trace("Removing system properties.");
        }
        if (systemProperties == null || systemProperties.length == 0) {
            return;
        }

        final List<ModelNode> updates = new ArrayList<ModelNode>();

        for (SystemProperty systemProperty : systemProperties) {
            final String propertyName = systemProperty.getName();
            if (propertyName == null || propertyName.trim().length() == 0) {
                continue;
            }
            ModelNode op = new ModelNode();
            op.get(OP).set(REMOVE);
            op.get(OP_ADDR).add(SYSTEM_PROPERTY, propertyName);
            updates.add(op);
        }
        CoreUtils.applyUpdates(updates, managementClient.getControllerClient());

    }

    public static SystemProperty[] mapToSystemProperties(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        final List<SystemProperty> list = new ArrayList<SystemProperty>();
        for (Map.Entry<String, String> property : map.entrySet()) {
            list.add(new DefaultSystemProperty(property.getKey(), property.getValue()));
        }
        return list.toArray(new SystemProperty[list.size()]);
    }

    // Protected methods -----------------------------------------------------

    /**
     * Returns configuration of the login modules.
     *
     * @return
     */
    protected abstract SystemProperty[] getSystemProperties();

    // Embedded classes ------------------------------------------------------

    public interface SystemProperty {
        String getName();

        String getValue();

    }

    public static class DefaultSystemProperty implements SystemProperty {
        private final String name;
        private final String value;

        /**
         * Create a new DefaultSystemProperty.
         *
         * @param name
         * @param value
         */
        public DefaultSystemProperty(String name, String value) {
            super();
            this.name = name;
            this.value = value;
        }

        /**
         * Get the name.
         *
         * @return the name.
         */
        public String getName() {
            return name;
        }

        /**
         * Get the value.
         *
         * @return the value.
         */
        public String getValue() {
            return value;
        }

    }
}
