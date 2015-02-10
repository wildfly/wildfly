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

import org.jboss.arquillian.container.spi.ConfigurationException;
import org.jboss.arquillian.container.spi.client.container.ContainerConfiguration;

/**
 * JBossAS7 server configuration
 *
 * @author Thomas.Diesler@jboss.com
 * @author <a href="mailto:alr@jboss.org">Andrew Lee Rubinger</a>
 */
public class CommonContainerConfiguration implements ContainerConfiguration {

    private static final ThreadLocal<CommonContainerConfiguration> containerConfigurationAssociation = new ThreadLocal<>();

    private String managementProtocol = "http-remoting";
    private String managementAddress;
    private int managementPort;

    private String username;
    private String password;

    private boolean enableThreadContextClassLoader = true;

    public CommonContainerConfiguration() {
        managementAddress = "127.0.0.1";
        managementPort = 9990;
    }

    public String getManagementAddress() {
        return managementAddress;
    }

    public void setManagementAddress(String host) {
        this.managementAddress = host;
    }

    public int getManagementPort() {
        return managementPort;
    }

    public void setManagementPort(int managementPort) {
        this.managementPort = managementPort;
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

    public String getManagementProtocol() {
        return managementProtocol;
    }

    public void setManagementProtocol(final String managementProtocol) {
        this.managementProtocol = managementProtocol;
    }

    public boolean isEnableThreadContextClassLoader() {
        return enableThreadContextClassLoader;
    }

    public void setEnableThreadContextClassLoader(boolean enableThreadContextClassLoader) {
        this.enableThreadContextClassLoader = enableThreadContextClassLoader;
    }

    public static CommonContainerConfiguration getContainerConfiguration() {
        return containerConfigurationAssociation.get();
    }

    public static void setContainerConfiguration(CommonContainerConfiguration containerConfiguration) {
        containerConfigurationAssociation.set(containerConfiguration);
    }

    @Override
    public void validate() throws ConfigurationException {
        if (username != null && password == null) {
            throw new ConfigurationException("username has been set, but no password given");
        }
    }
}
