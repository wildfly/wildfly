/*
 * Copyright 2016 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  *
 * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.wildfly.test.integration.vdx.utils.server;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.logging.Logger;
import org.junit.Assert;
import org.wildfly.extras.creaper.commands.foundation.offline.ConfigurationFileBackup;
import org.wildfly.extras.creaper.commands.foundation.offline.xml.GroovyXmlTransform;
import org.wildfly.extras.creaper.commands.foundation.offline.xml.Subtree;
import org.wildfly.extras.creaper.core.offline.OfflineCommand;
import org.wildfly.extras.creaper.core.offline.OfflineManagementClient;
import org.wildfly.test.integration.vdx.transformations.DoNothing;
import org.wildfly.test.integration.vdx.utils.FileUtils;

import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public abstract class ServerBase implements Server {

    protected Path testArchiveDirectory = null;
    private ConfigurationFileBackup configurationFileBackup = new ConfigurationFileBackup();
    private static Server server = null;
    private OfflineManagementClient managementClient;
    static final Logger log = Logger.getLogger(ServerBase.class);

    @Override
    public void tryStartAndWaitForFail(OfflineCommand... offlineCommands) throws Exception {

        boolean modifyConfiguration = (offlineCommands != null) || (! getServerConfig().xmlTransformationGroovy().equals(""));

        // stop server if running
        stop();

        // copy logging.properties
        copyLoggingPropertiesToConfiguration();

        // if configuration file is not in configuration directory then copy from resources directory (never override)
        copyConfigFilesFromResourcesIfItDoesNotExist();

        if (modifyConfiguration) {
            managementClient = getOfflineManagementClient();

            // backup config
            backupConfiguration();

            // apply transformation(s)
            if (offlineCommands == null) {
                applyXmlTransformation();
            } else {
                managementClient.apply(offlineCommands);
            }
        } else {
            managementClient = null;  // to see NPE when trying unexpected use-case
        }

        // archive configuration used during server start
        archiveModifiedUsedConfig();

        try {
            // tryStartAndWaitForFail - this must throw exception due invalid xml
            startServer();

            // fail the test if server starts
            Assert.fail("Server started successfully - probably xml was not invalidated/damaged correctly.");

        } catch (Exception ex) {
            log.debug("Start of the server failed. This is expected.");
        } finally {

            // restore original config if it exists
            if (modifyConfiguration) {
                restoreConfigIfBackupExists();
            }
        }
    }

    @Override
    public void tryStartAndWaitForFail() throws Exception {
        tryStartAndWaitForFail(null);
    }

    @Override
    public abstract Path getServerLogPath();

    /**
     * Copies custom logging.properties to server configuration.
     * This will log ERROR messages to target/errors.log file
     *
     * @throws Exception when copy fails
     */
    protected void copyLoggingPropertiesToConfiguration() throws Exception {
        String loggingPropertiesInResources = RESOURCES_DIRECTORY + LOGGING_PROPERTIES_FILE_NAME;
        FileUtils.copyFileFromResourcesToServer(loggingPropertiesInResources, CONFIGURATION_PATH, true);
    }

    /**
     * This will copy config file from resources directory to configuration directory of application server
     * This never overrides existing files, so the file with the same name in configuration directory of server has precedence
     *
     * @throws Exception when copy operation
     */
    protected abstract void copyConfigFilesFromResourcesIfItDoesNotExist() throws Exception;

    protected abstract OfflineManagementClient getOfflineManagementClient() throws Exception;

    protected abstract void startServer() throws Exception;

    @Override
    public String getErrorMessageFromServerStart() throws Exception {
        return String.join("\n", Files.readAllLines(Paths.get(ERRORS_LOG_FILE_NAME), Charset.forName(System.getProperty("file.encoding", "UTF-8"))));
    }

    private void backupConfiguration() throws Exception {
        // destroy any existing backup config
        managementClient.apply(configurationFileBackup.destroy());
        // backup any existing config
        managementClient.apply(configurationFileBackup.backup());
    }

    private void restoreConfigIfBackupExists() throws Exception {
        if (configurationFileBackup == null) {
            throw new Exception("Backup config is null. This can happen if this method is called before " +
                    "startServer() call. Check tryStartAndWaitForFail() sequence that backupConfiguration() was called.");
        }
        log.debug("Restoring server configuration. Configuration to be restored " + getServerConfig());
        managementClient.apply(configurationFileBackup.restore());
    }

    private void archiveModifiedUsedConfig() throws Exception {
        Files.copy(CONFIGURATION_PATH.resolve(getServerConfig().configuration()),
                testArchiveDirectory.resolve(getServerConfig().configuration()), StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Damages xml config file only if config file has valid syntax. This relies on well-formed xml.
     *
     * @throws Exception if not valid xml transformation
     */
    @SuppressWarnings("deprecation")
    private void applyXmlTransformation() throws Exception {
        ServerConfig serverConfig = getServerConfig();

        if (serverConfig.subtreeName().equals("")) {  // standalone or domain case without subtree
            managementClient.apply(GroovyXmlTransform.of(DoNothing.class, serverConfig.xmlTransformationGroovy())
                            .parameter(serverConfig.parameterName(), serverConfig.parameterValue())
                            .build());
            return;
        }
        if (serverConfig.profileName().equals("")) {  // standalone case with subtree
            managementClient.apply(GroovyXmlTransform.of(DoNothing.class, serverConfig.xmlTransformationGroovy())
                            .subtree(serverConfig.subtreeName(), Subtree.subsystem(serverConfig.subsystemName()))
                            .parameter(serverConfig.parameterName(), serverConfig.parameterValue())
                            .build());

        } else {  // domain case with subtree
            managementClient.apply(GroovyXmlTransform.of(DoNothing.class, serverConfig.xmlTransformationGroovy())
                            .subtree(serverConfig.subtreeName(), Subtree.subsystemInProfile(serverConfig.profileName(), serverConfig.subsystemName()))
                            .parameter(serverConfig.parameterName(), serverConfig.parameterValue())
                            .build());

        }
    }

    /**
     * @return returns Search stacktrace for @ServerConfig annotation and return it, returns null if there is none
     */
    static ServerConfig getServerConfig() {
        Throwable t = new Throwable();
        StackTraceElement[] elements = t.getStackTrace();
        String callerMethodName;
        String callerClassName;
        ServerConfig serverConfig = null;

        for (int level = 1; level < elements.length; level++) {
            try {
                callerClassName = elements[level].getClassName();
                callerMethodName = elements[level].getMethodName();
                Method method = Class.forName(callerClassName).getMethod(callerMethodName);
                serverConfig = method.getAnnotation(ServerConfig.class);
                if (serverConfig != null) {
                    break;
                }
            } catch (Exception e) {
                // ignore
            }
        }
        return serverConfig;
    }

    /**
     * Creates instance of server. If -Ddomain=true system property is specified it will be domain server,
     * otherwise standalone server will be used.
     *
     * @param controller arquillian container controller
     * @return Server instance - standalone by default or domain if -Ddomain=true is set
     */
    public static Server getOrCreateServer(ContainerController controller) {
        if (server == null) {
            if (Server.isDomain()) {
                server = new ServerDomain(controller);
            } else {
                server = new ServerStandalone(controller);
            }
        }
        return server;
    }

    @Override
    public void setTestArchiveDirectory(Path testArchiveDirectory) {
        this.testArchiveDirectory = testArchiveDirectory;
    }
}
