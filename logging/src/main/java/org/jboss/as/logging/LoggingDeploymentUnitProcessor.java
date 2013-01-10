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

package org.jboss.as.logging;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Manifest;

import org.apache.log4j.xml.DOMConfigurator;
import org.jboss.as.logging.logmanager.ConfigurationPersistence;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.SubDeploymentMarker;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.logmanager.LogContext;
import org.jboss.logmanager.PropertyConfigurator;
import org.jboss.logmanager.ThreadLocalLogContextSelector;
import org.jboss.modules.Module;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualFileFilter;

/**
 * A processor to search for logging configuration files for the deployment.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class LoggingDeploymentUnitProcessor implements DeploymentUnitProcessor {
    static final LoggingDeploymentUnitProcessor INSTANCE = new LoggingDeploymentUnitProcessor();

    public static final String PER_DEPLOYMENT_LOGGING = "org.jboss.as.logging.per-deployment";

    public static final AttachmentKey<LogContext> LOG_CONTEXT_KEY = AttachmentKey.create(LogContext.class);

    private static final String ENCODING = "utf-8";
    private static final String LOGGING_PROFILE = "Logging-Profile";
    private static final String LOG4J_PROPERTIES = "log4j.properties";
    private static final String LOG4J_XML = "log4j.xml";
    private static final String JBOSS_LOG4J_XML = "jboss-log4j.xml";
    private static final String DEFAULT_PROPERTIES = "logging.properties";
    private static final String JBOSS_PROPERTIES = "jboss-logging.properties";
    private static final Object CONTEXT_LOCK = new Object();

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (deploymentUnit.hasAttachment(Attachments.MODULE) && deploymentUnit.hasAttachment(Attachments.DEPLOYMENT_ROOT)) {
            // don't process sub-deployments
            final ResourceRoot root = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
            if (SubDeploymentMarker.isSubDeployment(root)) return;
            if (!processDeploymentLogging(deploymentUnit, root)) {
                processLoggingProfiles(deploymentUnit, root);
            }
        }
    }

    @Override
    public void undeploy(final DeploymentUnit context) {
        // OSGi bundles deployments may not have a module attached
        if (context.hasAttachment(Attachments.MODULE)) {
            // Remove any log context selector references
            final Module module = context.getAttachment(Attachments.MODULE);
            final ClassLoader current = SecurityActions.getThreadContextClassLoader();
            try {
                // Unregister the log context
                SecurityActions.setThreadContextClassLoader(module.getClassLoader());
                final LogContext logContext = LogContext.getLogContext();
                LoggingExtension.CONTEXT_SELECTOR.unregisterLogContext(module.getClassLoader(), logContext);
                LoggingLogger.ROOT_LOGGER.tracef("Removing LogContext '%s' from '%s'", logContext, module);
                context.removeAttachment(LOG_CONTEXT_KEY);
            } finally {
                SecurityActions.setThreadContextClassLoader(current);
            }
        }
    }

    private boolean processLoggingProfiles(final DeploymentUnit deploymentUnit, final ResourceRoot root) throws DeploymentUnitProcessingException {
        boolean result = false;
        final List<DeploymentUnit> subDeployments = getSubDeployments(deploymentUnit);
        final String loggingProfile = findLoggingProfile(root);
        if (loggingProfile != null) {
            result = true;
            // Get the profile logging context
            final LoggingProfileContextSelector loggingProfileContext = LoggingProfileContextSelector.getInstance();
            if (loggingProfileContext.exists(loggingProfile)) {
                // Get the module
                final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
                final LogContext logContext = loggingProfileContext.get(loggingProfile);
                LoggingExtension.CONTEXT_SELECTOR.registerLogContext(module.getClassLoader(), logContext);
                LoggingLogger.ROOT_LOGGER.tracef("Registering log context '%s' on '%s' for profile '%s'", logContext, root, loggingProfile);
                // Process sub-deployments
                for (DeploymentUnit subDeployment : subDeployments) {
                    // A sub-deployment must have a module to process
                    if (subDeployment.hasAttachment(Attachments.MODULE)) {
                        // Set the result to true if a logging profile was found
                        if (subDeployment.hasAttachment(Attachments.DEPLOYMENT_ROOT) && processLoggingProfiles(subDeployment, subDeployment.getAttachment(Attachments.DEPLOYMENT_ROOT))) {
                            result = true;
                        } else {
                            final Module subDeploymentModule = subDeployment.getAttachment(Attachments.MODULE);
                            LoggingExtension.CONTEXT_SELECTOR.registerLogContext(subDeploymentModule.getClassLoader(), logContext);
                            LoggingLogger.ROOT_LOGGER.tracef("Registering log context '%s' on '%s' for profile '%s'", logContext, subDeployment.getAttachment(Attachments.DEPLOYMENT_ROOT), loggingProfile);
                        }
                    }
                }
            } else {
                LoggingLogger.ROOT_LOGGER.loggingProfileNotFound(loggingProfile, root);
            }
        } else {
            // Process sub-deployments
            for (DeploymentUnit subDeployment : subDeployments) {
                // A sub-deployment must have a root resource and a module to process
                if (subDeployment.hasAttachment(Attachments.MODULE) && subDeployment.hasAttachment(Attachments.DEPLOYMENT_ROOT)) {
                    // Set the result to true if a logging profile was found
                    if (processLoggingProfiles(subDeployment, subDeployment.getAttachment(Attachments.DEPLOYMENT_ROOT)))
                        result = true;
                }
            }
        }
        return result;
    }

    private boolean processDeploymentLogging(final DeploymentUnit deploymentUnit, final ResourceRoot root) throws DeploymentUnitProcessingException {
        boolean result = false;
        if (Boolean.valueOf(SecurityActions.getSystemProperty(PER_DEPLOYMENT_LOGGING, Boolean.toString(true)))) {
            // check if we already setup LogContext
            if (deploymentUnit.hasAttachment(LOG_CONTEXT_KEY)) {
                return true;
            }

            LoggingLogger.ROOT_LOGGER.trace("Scanning for logging configuration files.");
            final List<DeploymentUnit> subDeployments = getSubDeployments(deploymentUnit);
            // Check for a config file
            final VirtualFile configFile = findConfigFile(root);
            if (configFile != null) {
                result = true;
                // Get the module
                final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
                // Create the log context and load into the selector for the module.
                final LogContext logContext = LogContext.create();
                // Configure the deployments logging based on the top-level configuration file
                configure(configFile, module.getClassLoader(), logContext);

                // Process the sub-deployments
                for (DeploymentUnit subDeployment : subDeployments) {
                    if (subDeployment.hasAttachment(Attachments.DEPLOYMENT_ROOT) && processDeploymentLogging(subDeployment, subDeployment.getAttachment(Attachments.DEPLOYMENT_ROOT))) {
                        result = true;
                    } else {
                        // No configuration file found, use the top-level configuration
                        final Module subDeploymentModule = subDeployment.getAttachment(Attachments.MODULE);
                        if (subDeploymentModule != null) {
                            LoggingExtension.CONTEXT_SELECTOR.registerLogContext(subDeploymentModule.getClassLoader(), logContext);
                        }
                    }
                }
            } else {
                // No configuration was found, process sub-deployments
                for (DeploymentUnit subDeployment : subDeployments) {
                    final ResourceRoot subDeploymentRoot = subDeployment.getAttachment(Attachments.DEPLOYMENT_ROOT);
                    // Process the sub-deployment
                    if (processDeploymentLogging(subDeployment, subDeploymentRoot)) result = true;
                }
            }
        }
        return result;
    }

    /**
     * Find the logging profile attached to any resource.
     *
     * @param resourceRoot the root resource
     *
     * @return the logging profile name or {@code null} if one was not found
     */
    private String findLoggingProfile(final ResourceRoot resourceRoot) {
        final Manifest manifest = resourceRoot.getAttachment(Attachments.MANIFEST);
        if (manifest != null) {
            final String loggingProfile = manifest.getMainAttributes().getValue(LOGGING_PROFILE);
            if (loggingProfile != null) {
                LoggingLogger.ROOT_LOGGER.debugf("Logging profile '%s' found in '%s'.", loggingProfile, resourceRoot);
                return loggingProfile;
            }
        }
        return null;
    }

    /**
     * Finds the configuration file to be used and returns the first one found.
     * <p/>
     * Preference is for {@literal logging.properties} or {@literal jboss-logging.properties}.
     *
     * @param resourceRoot the resource to check.
     *
     * @return the configuration file if found, otherwise {@code null}.
     *
     * @throws DeploymentUnitProcessingException
     *          if an error occurs.
     */
    private VirtualFile findConfigFile(ResourceRoot resourceRoot) throws DeploymentUnitProcessingException {
        final VirtualFile root = resourceRoot.getRoot();
        // First check META-INF
        VirtualFile file = root.getChild("META-INF");
        VirtualFile result = findConfigFile(file);
        if (result == null) {
            file = root.getChild("WEB-INF/classes");
            result = findConfigFile(file);
        }
        return result;
    }

    /**
     * Finds the configuration file to be used and returns the first one found.
     * <p/>
     * Preference is for {@literal logging.properties} or {@literal jboss-logging.properties}.
     *
     * @param file the file to check
     *
     * @return the configuration file if found, otherwise {@code null}
     *
     * @throws DeploymentUnitProcessingException
     *          if an error occurs.
     */
    private VirtualFile findConfigFile(final VirtualFile file) throws DeploymentUnitProcessingException {
        VirtualFile result = null;
        try {
            final List<VirtualFile> configFiles = file.getChildren(ConfigFilter.INSTANCE);
            for (final VirtualFile configFile : configFiles) {
                final String fileName = configFile.getName();
                if (DEFAULT_PROPERTIES.equals(fileName) || JBOSS_PROPERTIES.equals(fileName)) {
                    if (result != null) {
                        LoggingLogger.ROOT_LOGGER.debugf("The previously found configuration file '%s' is being ignored in favour of '%s'", result, configFile);
                    }
                    return configFile;
                } else if (LOG4J_PROPERTIES.equals(fileName) || LOG4J_XML.equals(fileName) || JBOSS_LOG4J_XML.equals(fileName)) {
                    result = configFile;
                }
            }
        } catch (IOException e) {
            throw LoggingMessages.MESSAGES.errorProcessingLoggingConfiguration(e);
        }
        return result;
    }

    private void configure(final VirtualFile configFile, final ClassLoader classLoader, final LogContext logContext) throws DeploymentUnitProcessingException {
        InputStream configStream = null;
        try {
            LoggingLogger.ROOT_LOGGER.debugf("Found logging configuration file: %s", configFile);
            LoggingExtension.CONTEXT_SELECTOR.registerLogContext(classLoader, logContext);

            // Get the filname and open the stream
            final String fileName = configFile.getName();
            configStream = configFile.openStream();

            // Check the type of the configuration file
            if (LOG4J_PROPERTIES.equals(fileName) || LOG4J_XML.equals(fileName) || JBOSS_LOG4J_XML.equals(fileName)) {
                final ClassLoader current = SecurityActions.getThreadContextClassLoader();
                final LogContext old = LoggingExtension.THREAD_LOCAL_CONTEXT_SELECTOR.getAndSet(CONTEXT_LOCK, logContext);
                try {
                    SecurityActions.setThreadContextClassLoader(classLoader);
                    if (LOG4J_XML.equals(fileName) || JBOSS_LOG4J_XML.equals(fileName)) {
                        new DOMConfigurator().doConfigure(configStream, org.apache.log4j.JBossLogManagerFacade.getLoggerRepository(logContext));
                    } else {
                        final Properties properties = new Properties();
                        properties.load(new InputStreamReader(configStream, ENCODING));
                        new org.apache.log4j.PropertyConfigurator().doConfigure(properties, org.apache.log4j.JBossLogManagerFacade.getLoggerRepository(logContext));
                    }
                } finally {
                    LoggingExtension.THREAD_LOCAL_CONTEXT_SELECTOR.getAndSet(CONTEXT_LOCK, old);
                    SecurityActions.setThreadContextClassLoader(current);
                }
            } else {
                // Create a properties file
                final Properties properties = new Properties();
                properties.load(new InputStreamReader(configStream, ENCODING));
                // Attempt to see if this is a J.U.L. configuration file
                if (isJulConfiguration(properties)) {
                    LoggingLogger.ROOT_LOGGER.julConfigurationFileFound(configFile.getName());
                } else {
                    // Load non-log4j types
                    final PropertyConfigurator propertyConfigurator = new PropertyConfigurator(logContext);
                    propertyConfigurator.configure(properties);
                }
            }
        } catch (Exception e) {
            throw LoggingMessages.MESSAGES.failedToConfigureLogging(e, configFile.getName());
        } finally {
            safeClose(configStream);
        }
    }

    private static List<DeploymentUnit> getSubDeployments(final DeploymentUnit deploymentUnit) {
        final List<DeploymentUnit> result = new ArrayList<DeploymentUnit>();
        if (deploymentUnit.hasAttachment(Attachments.SUB_DEPLOYMENTS)) {
            result.addAll(deploymentUnit.getAttachmentList(Attachments.SUB_DEPLOYMENTS));
        }
        return result;
    }

    private static void safeClose(final Closeable closable) {
        if (closable != null) try {
            closable.close();
        } catch (Exception e) {
            // no-op
        }
    }

    private static boolean isJulConfiguration(final Properties properties) {
        // First check for .levels as it's the cheapest
        if (properties.containsKey(".level")) {
            return true;
            // Check the handlers, in JBoss Log Manager they should be handler.HANDLER_NAME=HANDLER_CLASS,
            // J.U.L. uses fully.qualified.handler.class.property
        } else if (properties.containsKey("handlers")) {
            final String prop = properties.getProperty("handlers", "");
            if (prop != null && !prop.trim().isEmpty()) {
                final String[] handlers = prop.split("\\s*,\\s*");
                for (String handler : handlers) {
                    final String key = String.format("handler.%s", handler);
                    if (!properties.containsKey(key)) {
                        return true;
                    }
                }
            }
        }
        // Assume it's okay
        return false;
    }

    private static class ConfigFilter implements VirtualFileFilter {

        static final ConfigFilter INSTANCE = new ConfigFilter();
        private final Set<String> configFiles = new HashSet<String>(Arrays.asList(LOG4J_PROPERTIES, LOG4J_XML, JBOSS_LOG4J_XML, JBOSS_PROPERTIES, DEFAULT_PROPERTIES));

        @Override
        public boolean accepts(final VirtualFile file) {
            return configFiles.contains(file.getName());
        }
    }
}
