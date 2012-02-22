/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.host.controller.descriptions;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST_ENVIRONMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;

import java.io.File;
import java.net.InetAddress;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.persistence.ConfigurationFile;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.host.controller.HostControllerEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * A resource description that describes the host environment.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author Kabir Khan
 */
public class HostControllerResourceDescription extends SimpleResourceDefinition {
    public static final PathElement RESOURCE_PATH = PathElement.pathElement(CORE_SERVICE, HOST_ENVIRONMENT);

    private static final AttributeDefinition PROCESS_CONTROLLER_ADDRESS = createAttributeDefinition("process-controller-address");
    private static final AttributeDefinition PROCESS_CONTROLLER_PORT = createAttributeDefinition("process-controller-port", ModelType.INT);
    private static final AttributeDefinition HOST_CONTROLLER_ADDRESS = createAttributeDefinition("host-controller-address");
    private static final AttributeDefinition HOST_CONTROLLER_PORT = createAttributeDefinition("host-controller-port", ModelType.INT);
    private static final AttributeDefinition HOME_DIR = createAttributeDefinition("home-dir");
    private static final AttributeDefinition MODULES_DIR = createAttributeDefinition("modules-dir");
    private static final AttributeDefinition DOMAIN_BASE_DIR = createAttributeDefinition("domain-base-dir");
    private static final AttributeDefinition DOMAIN_CONFIG_DIR = createAttributeDefinition("domain-config-dir");
    private static final AttributeDefinition HOST_CONFIG_FILE = createAttributeDefinition("host-config-file");
    private static final AttributeDefinition DOMAIN_CONFIG_FILE = createAttributeDefinition("domain-config-file");
    private static final AttributeDefinition DOMAIN_CONTENT_DIR = createAttributeDefinition("domain-content-dir");
    private static final AttributeDefinition DOMAIN_DATA_DIR = createAttributeDefinition("domain-data-dir");
    private static final AttributeDefinition DOMAIN_LOG_DIR = createAttributeDefinition("domain-log-dir");
    private static final AttributeDefinition DOMAIN_SERVERS_DIR = createAttributeDefinition("domain-servers-dir");
    private static final AttributeDefinition DOMAIN_TEMP_DIR = createAttributeDefinition("domain-temp-dir");
    private static final AttributeDefinition DEFAULT_JVM = createAttributeDefinition("default-jvm");
    private static final AttributeDefinition IS_RESTART = createAttributeDefinition("is-restart", ModelType.BOOLEAN);
    private static final AttributeDefinition BACKUP_DOMAIN_FILES = createAttributeDefinition("backup-domain-files", ModelType.BOOLEAN);
    private static final AttributeDefinition USE_CACHED_DC = createAttributeDefinition("use-cached-dc", ModelType.BOOLEAN);
    private static final AttributeDefinition INITIAL_RUNNING_MODE = createAttributeDefinition("initial-running-mode");
    private static final AttributeDefinition QUALIFIED_HOST_NAME = createAttributeDefinition("qualified-host-name");
    private static final AttributeDefinition HOST_NAME = createAttributeDefinition("host-name");

    public static final AttributeDefinition[] HOST_ENV_ATTRIBUTES = {
        PROCESS_CONTROLLER_ADDRESS,
        PROCESS_CONTROLLER_PORT,
        HOST_CONTROLLER_ADDRESS,
        HOST_CONTROLLER_PORT,
        HOME_DIR,
        MODULES_DIR,
        DOMAIN_BASE_DIR,
        DOMAIN_CONFIG_DIR,
        HOST_CONFIG_FILE,
        DOMAIN_CONFIG_FILE,
        DOMAIN_CONTENT_DIR,
        DOMAIN_DATA_DIR,
        DOMAIN_LOG_DIR,
        DOMAIN_SERVERS_DIR,
        DOMAIN_TEMP_DIR,
        DEFAULT_JVM,
        IS_RESTART,
        BACKUP_DOMAIN_FILES,
        USE_CACHED_DC,
        INITIAL_RUNNING_MODE,
        QUALIFIED_HOST_NAME,
        HOST_NAME
    };

    private final HostEnvironmentReadHandler osh;

    /**
     * Creates a new description provider to describe the server environment.
     *
     * @param environment the environment the resource is based on.
     */
    private HostControllerResourceDescription(final HostControllerEnvironment environment) {
        super(RESOURCE_PATH, HostRootDescription.getResourceDescriptionResolver("host.env"));
        osh = new HostEnvironmentReadHandler(environment);
    }

    /**
     * A factory method for creating a new server environment resource description.
     *
     * @param environment the environment the resource is based on.
     *
     * @return a new server environment resource description.
     */
    public static HostControllerResourceDescription of(final HostControllerEnvironment environment) {
        return new HostControllerResourceDescription(environment);
    }

    private static AttributeDefinition createAttributeDefinition(String name) {
        return createAttributeDefinition(name, ModelType.STRING);
    }

    private static AttributeDefinition createAttributeDefinition(String name, ModelType type) {
        return SimpleAttributeDefinitionBuilder.create(name, type).setFlags(AttributeAccess.Flag.STORAGE_RUNTIME).build();
    }

    @Override
    public void registerAttributes(final ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attribute : HOST_ENV_ATTRIBUTES) {
            resourceRegistration.registerReadOnlyAttribute(attribute, osh);
        }
    }



    private static class HostEnvironmentReadHandler implements OperationStepHandler {
        private final HostControllerEnvironment environment;

        public HostEnvironmentReadHandler(final HostControllerEnvironment environment) {
            this.environment = environment;
        }

        @Override
        public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
            final ModelNode result = context.getResult();
            final String name = operation.require(NAME).asString();
            if (equals(name, PROCESS_CONTROLLER_ADDRESS)) {
                set(result, environment.getProcessControllerAddress());
            } else if (equals(name, PROCESS_CONTROLLER_PORT)) {
                set(result, environment.getProcessControllerPort());
            } else if (equals(name, HOST_CONTROLLER_ADDRESS)) {
                set(result, environment.getHostControllerAddress());
            } else if (equals(name, HOST_CONTROLLER_PORT)) {
                set(result, environment.getHostControllerPort());
            } else if (equals(name, HOME_DIR)) {
                set(result, environment.getHomeDir());
            } else if (equals(name, MODULES_DIR)) {
                set (result, environment.getModulesDir());
            } else if (equals(name, DOMAIN_BASE_DIR)) {
                set(result, environment.getDomainBaseDir());
            } else if (equals(name, DOMAIN_CONFIG_DIR)) {
                set(result, environment.getDomainConfigurationDir());
            } else if (equals(name, HOST_CONFIG_FILE)) {
                set(result, environment.getHostConfigurationFile());
            } else if (equals(name, DOMAIN_CONFIG_FILE)) {
                set(result, environment.getDomainConfigurationFile());
            } else if (equals(name, DOMAIN_CONTENT_DIR)) {
                set(result, environment.getDomainContentDir());
            } else if (equals(name, DOMAIN_DATA_DIR)) {
                set(result, environment.getDomainDataDir());
            } else if (equals(name, DOMAIN_LOG_DIR)) {
                set(result, environment.getDomainLogDir());
            } else if (equals(name, DOMAIN_SERVERS_DIR)) {
                set(result, environment.getDomainServersDir());
            } else if (equals(name, DOMAIN_TEMP_DIR)) {
                set(result, environment.getDomainTempDir());
            } else if (equals(name, DEFAULT_JVM)) {
                set(result, environment.getDefaultJVM());
            } else if (equals(name, IS_RESTART)) {
                set(result, environment.isRestart());
            } else if (equals(name, BACKUP_DOMAIN_FILES)) {
                set(result, environment.isBackupDomainFiles());
            } else if (equals(name, USE_CACHED_DC)) {
                set(result, environment.isUseCachedDc());
            } else if (equals(name, INITIAL_RUNNING_MODE)) {
                set(result, environment.getInitialRunningMode().name());
            } else if (equals(name, QUALIFIED_HOST_NAME)) {
                set(result, environment.getQualifiedHostName());
            } else if (equals(name, HOST_NAME)) {
                set(result, environment.getHostName());
            }
            context.completeStep();
        }

        private void set(final ModelNode node, final int value) {
            node.set(value);
        }

        private void set(final ModelNode node, final boolean value) {
            node.set(value);
        }


        private void set(final ModelNode node, final String value) {
            if (value != null) {
                node.set(value);
            }
        }

        private void set(final ModelNode node, final InetAddress value) {
            if (value != null) {
                node.set(value.toString());
            }
        }

        private void set(final ModelNode node, final File value) {
            if (value != null) {
                node.set(value.getAbsolutePath());
            }
        }

        private void set(final ModelNode node, final ConfigurationFile value) {
            if (value != null) {
                set(node, value.getBootFile());
            }
        }

        private boolean equals(final String name, final AttributeDefinition attribute) {
            return name.equals(attribute.getName());
        }
    }
}
