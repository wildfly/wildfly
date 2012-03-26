/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.arquillian.container.domain;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.jboss.arquillian.container.spi.ConfigurationException;
import org.jboss.arquillian.container.spi.client.container.ContainerConfiguration;

/**
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @version $Revision: $
 */
public class CommonDomainContainerConfiguration implements ContainerConfiguration {

    private InetAddress managementAddress;
    private int managementPort;

    private String username;
    private String password;

    private Map<String, String> containerNameMap;

    private Map<String, String> containerModeMap;

    private int serverGroupOperationTimeoutInSeconds = 60;

    private int serverOperationTimeoutInSeconds = 60;

    public CommonDomainContainerConfiguration() {
        managementAddress = getInetAddress("127.0.0.1");
        managementPort = 9999;
    }

    public InetAddress getManagementAddress() {
        return managementAddress;
    }

    public void setManagementAddress(String host) {
        this.managementAddress = getInetAddress(host);
    }

    public int getManagementPort() {
        return managementPort;
    }

    public void setManagementPort(int managementPort) {
        this.managementPort = managementPort;
    }

    private InetAddress getInetAddress(String name) {
        try {
            return InetAddress.getByName(name);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Unknown host: " + name);
        }
    }

    public String getUsername() {
        return username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return the containerNameMap
     */
    public Map<String, String> getContainerNameMap() {
        if (containerNameMap == null) {
            return new HashMap<String, String>();
        }
        return containerNameMap;
    }

    /**
     * Change the container name as seen by Arquillian of the Servers or ServerGroups in the Domain.
     * format: host:server-name=new-name,server-group-name=crm-servers
     *
     * @param containerNameMap
     */
    public void setContainerNameMap(String containerNameMap) {
        this.containerNameMap = convertToMap(containerNameMap);
    }

    /**
     * @return the containerModeMap
     */
    public Map<String, String> getContainerModeMap() {
        if (containerModeMap == null) {
            return new HashMap<String, String>();
        }
        return containerModeMap;
    }

    /**
     * Change the container mode of the Servers or ServerGroups in the Domain.
     * format: host:server-name=manual,host:.*=suite
     *
     * @param containerModeMap
     */
    public void setContainerModeMap(String containerModeString) {
        this.containerModeMap = convertToMap(containerModeString);
    }

    /**
     * The number of seconds to wait before failing when starting/stopping a server group in the Domain.
     *
     * @param serverGroupStartupTimeoutInSeconds
     */
    public void setServerGroupOperationTimeoutInSeconds(int serverGroupStartupTimeoutInSeconds) {
        this.serverGroupOperationTimeoutInSeconds = serverGroupStartupTimeoutInSeconds;
    }

    public int getServerGroupOperationTimeoutInSeconds() {
        return serverGroupOperationTimeoutInSeconds;
    }

    /**
     * The number of seconds to wait before failing when starting/stopping a single server in the Domain.
     *
     * @param serverStartupTimeoutInSeconds
     */
    public void setServerOperationTimeoutInSeconds(int serverStartupTimeoutInSeconds) {
        this.serverOperationTimeoutInSeconds = serverStartupTimeoutInSeconds;
    }

    public int getServerOperationTimeoutInSeconds() {
        return serverOperationTimeoutInSeconds;
    }

    @Override
    public void validate() throws ConfigurationException {
        if (username != null && password == null) {
            throw new ConfigurationException("username has been set, but no password given");
        }
    }

    private Map<String, String> convertToMap(String data) {
        Map<String, String> map = new HashMap<String, String>();
        String[] values = data.split(",");

        for (String value : values) {
            String[] content = value.split("=");
            if(content.length != 2) {
                throw new IllegalArgumentException("Could not parse map data from '" + data +"'. Missing value or key in '" + value + "'");
            }
            map.put(clean(content[0]), clean(content[1]));
        }
        return map;
    }

    private String clean(String data) {
        return data.replaceAll("\\r\\n|\\r|\\n", " ").trim();
    }
}
