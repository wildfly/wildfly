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
package org.jboss.as.arquillian.container.managed;

import org.jboss.arquillian.container.spi.ConfigurationException;
import org.jboss.arquillian.container.spi.client.deployment.Validate;
import org.jboss.as.arquillian.container.CommonContainerConfiguration;

import java.io.File;

/**
 * JBossAsManagedConfiguration
 *
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @version $Revision: $
 */
public class ManagedContainerConfiguration extends CommonContainerConfiguration {

    private String jbossHome = System.getenv("JBOSS_HOME");

    private String javaHome = System.getenv("JAVA_HOME");

    private String modulePath = System.getProperty("module.path");

    private String javaVmArguments = System.getProperty("jboss.options", "-Xmx512m -XX:MaxPermSize=128m");

    private int startupTimeoutInSeconds = 60;

    private boolean outputToConsole = true;

    private String serverConfig = System.getProperty("jboss.server.config.file.name",  "standalone.xml");

    private boolean allowConnectingToRunningServer = false;

    public ManagedContainerConfiguration() {
        // if no javaHome is set use java.home of already running jvm
        if (javaHome == null || javaHome.isEmpty()) {
            javaHome = System.getProperty("java.home");
        }
    }

    @Override
    public void validate() throws ConfigurationException {
        super.validate();

        Validate.configurationDirectoryExists(jbossHome, "jbossHome '" + jbossHome + "' must exist");
        if (javaHome != null) {
            Validate.configurationDirectoryExists(javaHome, "javaHome must exist");
        }
    }

    /**
     * @return the jbossHome
     */
    public String getJbossHome() {
        return jbossHome;
    }

    /**
     * @param jbossHome the jbossHome to set
     */
    public void setJbossHome(String jbossHome) {
        this.jbossHome = jbossHome;
    }

    /**
     * @return the javaHome
     */
    public String getJavaHome() {
        return javaHome;
    }

    /**
     * @param javaHome the javaHome to set
     */
    public void setJavaHome(String javaHome) {
        this.javaHome = javaHome;
    }

    /**
     * @return the javaVmArguments
     */
    public String getJavaVmArguments() {
        return javaVmArguments;
    }

    /**
     * @param javaVmArguments the javaVmArguments to set
     */
    public void setJavaVmArguments(String javaVmArguments) {
        this.javaVmArguments = javaVmArguments;
    }

    /**
     * @param startupTimeoutInSeconds the startupTimeoutInSeconds to set
     */
    public void setStartupTimeoutInSeconds(int startupTimeoutInSeconds) {
        this.startupTimeoutInSeconds = startupTimeoutInSeconds;
    }

    /**
     * @return the startupTimeoutInSeconds
     */
    public int getStartupTimeoutInSeconds() {
        return startupTimeoutInSeconds;
    }

    /**
     * @param outputToConsole the outputToConsole to set
     */
    public void setOutputToConsole(boolean outputToConsole) {
        this.outputToConsole = outputToConsole;
    }

    /**
     * @return the outputToConsole
     */
    public boolean isOutputToConsole() {
        return outputToConsole;
    }

    /**
     * Get the server configuration file name.  Equivalent to [-server-config=...] on the command line.
     *
     * @return the server config
     */
    public String getServerConfig() {
        return serverConfig;
    }

    /**
     * Set the server configuration file name.  Equivalent to [-server-config=...] on the command line.
     *
     * @param serverConfig the server config
     */
    public void setServerConfig(String serverConfig) {
        this.serverConfig = serverConfig;
    }

    public String getModulePath() {
        return modulePath;
    }

    public void setModulePath(final String modulePath) {
        this.modulePath = modulePath;
    }

    public boolean isAllowConnectingToRunningServer() {
        return allowConnectingToRunningServer;
    }

    public void setAllowConnectingToRunningServer(final boolean allowConnectingToRunningServer) {
        this.allowConnectingToRunningServer = allowConnectingToRunningServer;
    }
}
