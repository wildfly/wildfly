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
package org.jboss.as.host.controller;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.JVM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.domain.controller.resources.ServerGroupResourceDefinition;
import org.jboss.as.host.controller.ManagedServer.ManagedServerBootConfiguration;
import org.jboss.as.host.controller.model.host.HostResourceDefinition;
import org.jboss.as.host.controller.model.jvm.JvmElement;
import org.jboss.as.host.controller.model.jvm.JvmOptionsBuilderFactory;
import org.jboss.as.process.DefaultJvmUtils;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.controller.resources.SystemPropertyResourceDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * Combines the relevant parts of the domain-level and host-level models to
 * determine the jvm launch command needed to start an application server instance.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class ManagedServerBootCmdFactory implements ManagedServerBootConfiguration {

    private static final ModelNode EMPTY = new ModelNode();
    static {
        EMPTY.setEmptyList();
        EMPTY.protect();
    }

    private final String serverName;
    private final ModelNode domainModel;
    private final ModelNode hostModel;
    private final ModelNode serverModel;
    private final ModelNode serverGroup;
    private final JvmElement jvmElement;
    private final HostControllerEnvironment environment;
    private final boolean managementSubsystemEndpoint;
    private final ModelNode endpointConfig = new ModelNode();
    private final ExpressionResolver expressionResolver;
    private final DirectoryGrouping directoryGrouping;

    ManagedServerBootCmdFactory(final String serverName, final ModelNode domainModel, final ModelNode hostModel, final HostControllerEnvironment environment, final ExpressionResolver expressionResolver) {
        this.serverName = serverName;
        this.domainModel = domainModel;
        this.hostModel = hostModel;
        this.environment = environment;
        this.expressionResolver = expressionResolver;
        this.serverModel = resolveExpressions(hostModel.require(SERVER_CONFIG).require(serverName));
        this.directoryGrouping = resolveDirectoryGrouping(hostModel, expressionResolver);
        final String serverGroupName = serverModel.require(GROUP).asString();
        this.serverGroup = resolveExpressions(domainModel.require(SERVER_GROUP).require(serverGroupName));

        String serverVMName = null;
        ModelNode serverVM = null;
        if(serverModel.hasDefined(JVM)) {
            for (final String jvm : serverModel.get(JVM).keys()) {
                serverVMName = jvm;
                serverVM = serverModel.get(JVM, jvm);
                break;
            }
        }
        String groupVMName = null;
        ModelNode groupVM = null;
        if(serverGroup.hasDefined(JVM)) {
            for(final String jvm : serverGroup.get(JVM).keys()) {
                groupVMName = jvm;
                groupVM = serverGroup.get(JVM, jvm);
                break;
            }
        }
        // Use the subsystem endpoint
        // TODO by default use the subsystem endpoint
        this.managementSubsystemEndpoint = serverGroup.get(ServerGroupResourceDefinition.MANAGEMENT_SUBSYSTEM_ENDPOINT.getName()).asBoolean(false);
        // Get the endpoint configuration
        if(managementSubsystemEndpoint) {
            final String profileName = serverGroup.get(PROFILE).asString();
            final ModelNode profile = domainModel.get(PROFILE, profileName);
            if(profile.hasDefined(SUBSYSTEM) && profile.hasDefined("remoting")) {
                endpointConfig.set(profile.get(SUBSYSTEM, "remoting"));
            }
        }

        final String jvmName = serverVMName != null ? serverVMName : groupVMName;
        final ModelNode hostVM = jvmName != null ? hostModel.get(JVM, jvmName) : null;

        this.jvmElement = new JvmElement(jvmName,
                resolveExpressions(hostVM),
                resolveExpressions(groupVM),
                resolveExpressions(serverVM));
    }

    /**
     * Resolve expressions in the given model (if there are any)
     */
    private ModelNode resolveExpressions(final ModelNode unresolved) {
        if (unresolved == null) {
            return null;
        }
        try {
            return expressionResolver.resolveExpressions(unresolved.clone());
        } catch (OperationFailedException e) {
            // Fail
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    /**
     * Returns the value of found in the model.
     *
     * @param model the model that contains the key and value.
     * @param expressionResolver the expression resolver to use to resolve expressions
     *
     * @return the directory grouping found in the model.
     *
     * @throws IllegalArgumentException if the {@link ModelDescriptionConstants#DIRECTORY_GROUPING directory grouping}
     *                                  was not found in the model.
     */
    private static DirectoryGrouping resolveDirectoryGrouping(final ModelNode model, final ExpressionResolver expressionResolver) {
        try {
            return DirectoryGrouping.forName(HostResourceDefinition.DIRECTORY_GROUPING.resolveModelAttribute(expressionResolver, model).asString());
        } catch (OperationFailedException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Create and verify the configuration before trying to start the process.
     *
     * @return the process boot configuration
     */
    public ManagedServerBootConfiguration createConfiguration() {
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public HostControllerEnvironment getHostControllerEnvironment() {
        return environment;
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getServerLaunchCommand() {
        final List<String> command = new ArrayList<String>();

        command.add(getJavaCommand());

        command.add("-D[" + ManagedServer.getServerProcessName(serverName) + "]");

        JvmOptionsBuilderFactory.getInstance().addOptions(jvmElement, command);

        Map<String, String> bootTimeProperties = getAllSystemProperties(true);
        // Add in properties passed in to the ProcessController command line
        for (Map.Entry<String, String> hostProp : environment.getHostSystemProperties().entrySet()) {
            if (!bootTimeProperties.containsKey(hostProp.getKey())) {
                bootTimeProperties.put(hostProp.getKey(), hostProp.getValue());
            }
        }
        for (Entry<String, String> entry : bootTimeProperties.entrySet()) {
            String property = entry.getKey();
            if (!"org.jboss.boot.log.file".equals(property) && !"logging.configuration".equals(property)) {
                final StringBuilder sb = new StringBuilder("-D");
                sb.append(property);
                sb.append('=');
                sb.append(entry.getValue() == null ? "true" : entry.getValue());
                command.add(sb.toString());
            }
        }
        // Use the directory grouping type to set props controlling the server data/log/tmp dirs
        String serverDirProp = bootTimeProperties.get(ServerEnvironment.SERVER_BASE_DIR);
        File serverDir = serverDirProp == null ? new File(environment.getDomainServersDir(), serverName) : new File(serverDirProp);
        final String logDir = addPathProperty(command, "log", ServerEnvironment.SERVER_LOG_DIR, bootTimeProperties,
                directoryGrouping, environment.getDomainLogDir(), serverDir);
        addPathProperty(command, "tmp", ServerEnvironment.SERVER_TEMP_DIR, bootTimeProperties,
                directoryGrouping, environment.getDomainTempDir(), serverDir);
        addPathProperty(command, "data", ServerEnvironment.SERVER_DATA_DIR, bootTimeProperties,
                directoryGrouping, environment.getDomainDataDir(), serverDir);

        final File loggingConfig = new File(getAbsolutePath(environment.getDomainServersDir(), serverName, "data", "logging.properties"));
        final String path;
        if (loggingConfig.exists()) {
            path = "file:" + loggingConfig.getAbsolutePath();
        } else {
            // Sets the initial log file to use
            command.add("-Dorg.jboss.boot.log.file=" + getAbsolutePath(new File(logDir), "server.log"));
            final String fileName = SecurityActions.getSystemProperty("logging.configuration");
            if (fileName == null) {
                // If nothing else is defined use a default logging configuration that matches the default from the
                // standalone server. This allows a single log file to be used.
                path = "file:" + getAbsolutePath(environment.getDomainConfigurationDir(), "default-server-logging.properties");
            } else {
                path = fileName;
            }
        }
        command.add(String.format("-Dlogging.configuration=%s", path));

        command.add("-jar");
        command.add(getAbsolutePath(environment.getHomeDir(), "jboss-modules.jar"));
        command.add("-mp");
        command.add(environment.getModulePath());
        command.add("-jaxpmodule");
        command.add("javax.xml.jaxp-provider");
        command.add("org.jboss.as.server");

        return command;
    }

    @Override
    public boolean isManagementSubsystemEndpoint() {
        return managementSubsystemEndpoint;
    }

    @Override
    public ModelNode getSubsystemEndpointConfiguration() {
        return endpointConfig;
    }

    private String getJavaCommand() {
        String javaHome = jvmElement.getJavaHome();
        if (javaHome == null) {
            if(environment.getDefaultJVM() != null) {
                String defaultJvm = environment.getDefaultJVM().getAbsolutePath();
                if (!defaultJvm.equals("java") || (defaultJvm.equals("java") && System.getenv("JAVA_HOME") != null)) {
                    return defaultJvm;
                }
            }
            javaHome = DefaultJvmUtils.getCurrentJvmHome();
        }

        return DefaultJvmUtils.findJavaExecutable(javaHome);
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, String> getServerLaunchEnvironment() {
        final Map<String, String> env = new HashMap<String, String>();
        for(final Entry<String, String> property : jvmElement.getEnvironmentVariables().entrySet()) {
            env.put(property.getKey(), property.getValue());
        }
        return env;
    }

    private Map<String, String> getAllSystemProperties(boolean boottimeOnly){
        Map<String, String> props = new HashMap<String, String>();

        addSystemProperties(domainModel, props, boottimeOnly);
        addSystemProperties(serverGroup, props, boottimeOnly);
        addSystemProperties(hostModel, props, boottimeOnly);
        addSystemProperties(serverModel, props, boottimeOnly);

        return props;
    }

    private void addSystemProperties(final ModelNode source, final Map<String, String> props, boolean boottimeOnly) {
        if (source.hasDefined(SYSTEM_PROPERTY)) {
            for (Property prop : source.get(SYSTEM_PROPERTY).asPropertyList()) {
                ModelNode propResource = prop.getValue();
                try {
                    if (boottimeOnly && !SystemPropertyResourceDefinition.BOOT_TIME.resolveModelAttribute(expressionResolver, propResource).asBoolean()) {
                        continue;
                    }
                } catch (OperationFailedException e) {
                    throw new IllegalStateException(e);
                }
                String val = propResource.hasDefined(VALUE) ? propResource.get(VALUE).asString() : null;
                props.put(prop.getName(), val);
            }
        }
    }

    /**
     * Adds the absolute path to command.
     *
     * @param command           the command to add the arguments to.
     * @param typeName          the type of directory.
     * @param propertyName      the name of the property.
     * @param properties        the properties where the path may already be defined.
     * @param directoryGrouping the directory group type.
     * @param typeDir           the domain level directory for the given directory type; to be used for by-type grouping
     * @param serverDir         the root directory for the server, to be used for 'by-server' grouping
     * @return the absolute path that was added.
     */
    private String addPathProperty(final List<String> command, final String typeName, final String propertyName, final Map<String, String> properties, final DirectoryGrouping directoryGrouping,
                                   final File typeDir, File serverDir) {
        final String result;
        final String value = properties.get(propertyName);
        if (value == null) {
            switch (directoryGrouping) {
                case BY_TYPE:
                    result = getAbsolutePath(typeDir, "servers", serverName);
                    break;
                case BY_SERVER:
                default:
                    result = getAbsolutePath(serverDir, typeName);
                    break;
            }
            properties.put(propertyName, result);
        } else {
            final File dir = new File(value);
            switch (directoryGrouping) {
                case BY_TYPE:
                    result = getAbsolutePath(dir, "servers", serverName);
                    break;
                case BY_SERVER:
                default:
                    result = dir.getAbsolutePath();
                    break;
            }
        }
        command.add(String.format("-D%s=%s", propertyName, result));
        return result;
    }

    static String getAbsolutePath(final File root, final String... paths) {
        File path = root;
        for(String segment : paths) {
            path = new File(path, segment);
        }
        return path.getAbsolutePath();
    }

}
