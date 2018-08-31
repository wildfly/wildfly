/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.test.multinode.security.api;

import java.io.Serializable;

import org.jboss.as.arquillian.container.ManagementClient;

/**
 * @author bmaxwell
 *
 */
public class ServerConfigs implements Serializable {

    private ServerConfig clientServer;
    private ServerConfig serverServer;

    public ServerConfig getClientServer() {
        return clientServer;
    }

    public ServerConfig getServerServer() {
        return serverServer;
    }

    public ServerConfigs(ManagementClient clientServer, String clientNodeName, ManagementClient serverServer, String serverNodeName) {
        this.clientServer = new ServerConfig(clientServer, clientNodeName, 0);
        this.serverServer = new ServerConfig(serverServer, serverNodeName, 100);
    }

    public static class ServerConfig implements Serializable {

        private String managementHost;
        private int managementPort;
        private int portOffset = 0;
        private String nodeName;

        public ServerConfig(ManagementClient managementClient, String nodeName, int portOffset) {
            this.managementHost = managementClient.getMgmtAddress();
            this.managementPort = managementClient.getMgmtPort();
            this.nodeName = nodeName;
            this.portOffset = portOffset;
        }

        public String getManagementHost() {
            return managementHost;
        }

        public int getManagementPort() {
            return managementPort;
        }

        public String getNodeName() {
            return nodeName;
        }

        public String getHost() {
            return managementHost;
        }

        public int getRemotingPort() {
            return 8080 + portOffset;
        }

        public int getHttpPort() {
            return 8080 + portOffset;
        }
    }
}