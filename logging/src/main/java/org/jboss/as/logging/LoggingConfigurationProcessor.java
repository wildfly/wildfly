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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.xml.DOMConfigurator;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.logmanager.LogContext;
import org.jboss.logmanager.PropertyConfigurator;
import org.jboss.modules.Module;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualFileFilter;

/**
 * A processor to search for logging configuration files for the deployment.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class LoggingConfigurationProcessor implements DeploymentUnitProcessor {
    static final LoggingConfigurationProcessor INSTANCE = new LoggingConfigurationProcessor();

    public static final String PER_DEPLOYMENT_LOGGING = "org.jboss.as.logging.per-deployment";

    private static final String LOG4J_PROPERTIES = "log4j.properties";
    private static final String LOG4J_XML = "log4j.xml";
    private static final String JBOSS_LOG4J_XML = "jboss-log4j.xml";
    private static final String DEFAULT_PROPERTIES = "logging.properties";
    private static final String JBOSS_PROPERTIES = "jboss-logging.properties";

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        // OSGi bundles deployments may not have a module attached
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        if (module == null)
            return;

        if (Boolean.valueOf(SecurityActions.getSystemProperty(PER_DEPLOYMENT_LOGGING, Boolean.toString(true)))) {
            LoggingLogger.ROOT_LOGGER.trace("Scanning for logging configuration files.");
            if (deploymentUnit.hasAttachment(Attachments.RESOURCE_ROOTS)) {
                final List<ResourceRoot> deploymentRoots = deploymentUnit.getAttachment(Attachments.RESOURCE_ROOTS);
                final VirtualFile configFile = findConfigFile(deploymentRoots);
                if (configFile != null) {
                    InputStream configStream = null;
                    try {
                        LoggingLogger.ROOT_LOGGER.debugf("Found logging configuration file: %s", configFile);
                        // Create the log context and load into the selector for the module.
                        final LogContext logContext = LogContext.create();
                        LoggingExtension.CONTEXT_SELECTOR.registerLogContext(module.getClassLoader(), logContext);

                        // Get the filname and open the stream
                        final String fileName = configFile.getName();
                        configStream = configFile.openStream();

                        // Check the type of the configuration file
                        if (LOG4J_PROPERTIES.equals(fileName) || LOG4J_XML.equals(fileName) || JBOSS_LOG4J_XML.equals(fileName)) {
                            final ClassLoader current = SecurityActions.getThreadContextClassLoader();
                            try {
                                SecurityActions.setThreadContextClassLoader(module.getClassLoader());
                                if (LOG4J_XML.equals(fileName) || JBOSS_LOG4J_XML.equals(fileName)) {
                                    new DOMConfigurator().doConfigure(configStream, org.apache.log4j.JBossLogManagerFacade.getLoggerRepository(logContext));
                                } else {
                                    final Properties properties = new Properties();
                                    properties.load(new InputStreamReader(configStream, "utf-8"));
                                    new org.apache.log4j.PropertyConfigurator().doConfigure(properties, org.apache.log4j.JBossLogManagerFacade.getLoggerRepository(logContext));
                                }
                            } finally {
                                SecurityActions.setThreadContextClassLoader(current);
                            }
                        } else {
                            // Load non-log4j types
                            new PropertyConfigurator(logContext).configure(configStream);
                        }
                    } catch (Exception e) {
                        throw LoggingMessages.MESSAGES.failedToConfigureLogging(e, configFile.getName());
                    } finally {
                        safeClose(configStream);
                    }
                }
            }
        }
    }

    @Override
    public void undeploy(final DeploymentUnit context) {

        // OSGi bundles deployments may not have a module attached
        final Module module = context.getAttachment(Attachments.MODULE);
        if (module == null)
            return;

        final ClassLoader current = SecurityActions.getThreadContextClassLoader();
        try {
            // Unregister the log context
            SecurityActions.setThreadContextClassLoader(module.getClassLoader());
            LoggingExtension.CONTEXT_SELECTOR.unregisterLogContext(module.getClassLoader(), LogContext.getLogContext());
        } finally {
            SecurityActions.setThreadContextClassLoader(current);
        }
    }

    /**
     * Finds the configuration file to be used and returns the first one found.
     * <p/>
     * Preference is for {@literal logging.properties} or {@literal jboss-logging.properties}.
     *
     * @param deploymentRoots the deployment roots to search.
     *
     * @return the configuration file if found, otherwise {@code null}.
     *
     * @throws DeploymentUnitProcessingException
     *          if an error occurs.
     */
    private VirtualFile findConfigFile(final List<ResourceRoot> deploymentRoots) throws DeploymentUnitProcessingException {
        VirtualFile result = null;
        for (ResourceRoot resourceRoot : deploymentRoots) {
            try {
                final List<VirtualFile> configFiles = resourceRoot.getRoot().getChildrenRecursively(ConfigFilter.INSTANCE);
                for (final VirtualFile file : configFiles) {
                    final String fileName = file.getName();
                    if (DEFAULT_PROPERTIES.equals(fileName) || JBOSS_PROPERTIES.equals(fileName)) {
                        if (result != null) {
                            LoggingLogger.ROOT_LOGGER.debugf("The previously found configuration file '%s' is being ignored in favour of '%s'", result, file);
                        }
                        return file;
                    } else if (LOG4J_PROPERTIES.equals(fileName) || LOG4J_XML.equals(fileName) || JBOSS_LOG4J_XML.equals(fileName)) {
                        result = file;
                    }
                }
            } catch (IOException e) {
                throw LoggingMessages.MESSAGES.errorProcessingLoggingConfiguration(e);
            }
            // TODO (jrp) may be best to check all resource roots
            if (result != null) {
                break;
            }
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

    private static class ConfigFilter implements VirtualFileFilter {

        static final ConfigFilter INSTANCE = new ConfigFilter();
        private final Set<String> configFiles = new HashSet<String>(Arrays.asList(LOG4J_PROPERTIES, LOG4J_XML, JBOSS_LOG4J_XML, JBOSS_PROPERTIES, DEFAULT_PROPERTIES));

        @Override
        public boolean accepts(final VirtualFile file) {
            return file.isDirectory() || configFiles.contains(file.getName());
        }
    }
}
