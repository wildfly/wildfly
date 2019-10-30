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

public class ServerDomain extends ServerBase {

    ContainerController controller;

    protected ServerDomain(ContainerController controller) {
        this.controller = controller;
    }

    @Override
    public void startServer() {

        ServerConfig serverConfig = getServerConfig();
        Map<String, String> containerProperties = new HashMap<>();
        if (serverConfig != null) {
            if (serverConfig.configuration().startsWith("host")) {  // for modifications in host.xml, see HostXmlSmokeTestCase
                containerProperties.put("domainConfig", DEFAULT_SERVER_CONFIG);
                containerProperties.put("hostConfig", serverConfig.configuration());
            } else {
                containerProperties.put("domainConfig", serverConfig.configuration());
                containerProperties.put("hostConfig", serverConfig.hostConfig());
            }
        } else { // if no server config was specified return arquillian to default // todo take this from arquillian.xml
            containerProperties.put("domainConfig", DEFAULT_SERVER_CONFIG);
            containerProperties.put("hostConfig", DEFAULT_HOST_CONFIG);
        }

        controller.start(TestBase.DOMAIN_ARQUILLIAN_CONTAINER, containerProperties);
    }

    @Override
    public Path getServerLogPath() {
        return Paths.get(JBOSS_HOME, SERVER_MODE, "log", "host-controller.log");
    }

    @Override
    protected void copyConfigFilesFromResourcesIfItDoesNotExist() throws Exception {
        if (Files.notExists(CONFIGURATION_PATH.resolve(getServerConfig().configuration()))) {
            FileUtils.copyFileFromResourcesToServer(RESOURCES_DIRECTORY + getServerConfig().configuration(),
                    CONFIGURATION_PATH, false);
        }
        if (Files.notExists(CONFIGURATION_PATH.resolve(getServerConfig().hostConfig()))) {
            FileUtils.copyFileFromResourcesToServer(RESOURCES_DIRECTORY + getServerConfig().hostConfig(),
                    CONFIGURATION_PATH, false);
        }
    }


    @Override
    protected OfflineManagementClient getOfflineManagementClient() throws Exception {
        return ManagementClient.offline(OfflineOptions
                .domain().build().rootDirectory(new File(JBOSS_HOME))
                .configurationFile(getServerConfig() == null ? DEFAULT_SERVER_CONFIG : getServerConfig().configuration())
                .build());
    }

    @Override
    public void stop() {
        controller.stop(TestBase.DOMAIN_ARQUILLIAN_CONTAINER);
    }

}
