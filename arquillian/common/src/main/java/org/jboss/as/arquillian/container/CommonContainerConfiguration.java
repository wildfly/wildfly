/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
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
package org.jboss.as.arquillian.container;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.jboss.arquillian.container.spi.ConfigurationException;
import org.jboss.arquillian.container.spi.client.container.ContainerConfiguration;

/**
 * JBossAS7 server configuration
 *
 * @author Thomas.Diesler@jboss.com
 * @since 17-Nov-2010
 */
public class CommonContainerConfiguration implements ContainerConfiguration {

    private InetAddress managementAddress;
    private int managementPort;

    private String username;
    private String password;

    public CommonContainerConfiguration() {
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

    @Override
    public void validate() throws ConfigurationException {
        if (username != null && password == null) {
            throw new ConfigurationException("username has been set, but no password given");
        }
    }
}
