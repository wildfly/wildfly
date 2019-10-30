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
import org.wildfly.extras.creaper.core.ManagementClient;
import org.wildfly.extras.creaper.core.offline.OfflineManagementClient;
import org.wildfly.extras.creaper.core.offline.OfflineOptions;
import org.wildfly.test.integration.vdx.TestBase;
import org.wildfly.test.integration.vdx.utils.FileUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class ServerStandalone extends ServerBase {

    private ContainerController controller;

    protected ServerStandalone(ContainerController controller) {
        this.controller = controller;
    }

    protected void startServer() throws Exception {

        ServerConfig serverConfig = getServerConfig();
        Map<String, String> containerProperties = new HashMap<>();
        if (serverConfig != null) {
            containerProperties.put("serverConfig", serverConfig.configuration());
        } else { // if no server config was specified return arquillian to default
            containerProperties.put("serverConfig", DEFAULT_SERVER_CONFIG);
        }

        controller.start(TestBase.STANDALONE_ARQUILLIAN_CONTAINER, containerProperties);
    }

    @Override
    protected OfflineManagementClient getOfflineManagementClient() throws Exception {
        return ManagementClient.offline(OfflineOptions
                .standalone()
                .rootDirectory(new File(JBOSS_HOME))
                .configurationFile(getServerConfig() == null ? DEFAULT_SERVER_CONFIG : getServerConfig().configuration())
                .build());
    }

    @Override
    public Path getServerLogPath() {
        return Paths.get(JBOSS_HOME, SERVER_MODE, "log", "server.log");
    }

    @Override
    protected void copyConfigFilesFromResourcesIfItDoesNotExist() throws Exception {
        if (Files.notExists(CONFIGURATION_PATH.resolve(getServerConfig().configuration()))) {
            FileUtils.copyFileFromResourcesToServer(RESOURCES_DIRECTORY + getServerConfig().configuration(),
                    CONFIGURATION_PATH, false);
        }
    }

    @Override
    public void stop() {
        controller.stop(TestBase.STANDALONE_ARQUILLIAN_CONTAINER);
    }


}
