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
package org.jboss.as.test.integration.domain.management.util;

import org.jboss.arquillian.container.spi.ConfigurationException;
import org.jboss.arquillian.container.spi.client.deployment.Validate;
import org.jboss.as.arquillian.container.CommonContainerConfiguration;

/**
 * JBossAsManagedConfiguration
 *
 * @author Brian Stansberry
 */
public class JBossAsManagedConfiguration extends CommonContainerConfiguration {
    private String jbossHome = System.getenv("JBOSS_HOME");

    private String javaHome = System.getenv("JAVA_HOME");

    private String modulePath = System.getProperty("module.path");

    private String javaVmArguments = "-Xmx512m -XX:MaxPermSize=128m";

    private int startupTimeoutInSeconds = 45;

    private boolean outputToConsole = true;

    private String hostControllerManagementAddress = System.getProperty("jboss.test.domain.management.address", "127.0.0.1");

    private int hostControllerManagementPort = 9999;

    private String hostName = "master";

    private String domainDir;

    private String domainConfigFile;

    private String hostConfigFile;

    private String hostCommandLineProperties;

    public JBossAsManagedConfiguration() {
        if (jbossHome == null) {
            jbossHome = System.getProperty("jboss.home");
        }
    }
    /*
     * (non-Javadoc)
     *
     * @see org.jboss.as.arquillian.container.JBossAsContainerConfiguration#validate()
     */
    @Override
    public void validate() throws ConfigurationException {
        super.validate();

        Validate.configurationDirectoryExists(jbossHome, "jbossHome must exist");
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

    public String getHostControllerManagementAddress() {
        return hostControllerManagementAddress;
    }

    public void setHostControllerManagementAddress(String hostControllerManagementAddress) {
        this.hostControllerManagementAddress = hostControllerManagementAddress;
    }

    public int getHostControllerManagementPort() {
        return hostControllerManagementPort;
    }

    public void setHostControllerManagementPort(int hostControllerManagementPort) {
        this.hostControllerManagementPort = hostControllerManagementPort;
    }

    public String getDomainDirectory() {
        return domainDir;
    }

    public void setDomainDirectory(String domainDir) {
        this.domainDir = domainDir;
    }

    public String getDomainConfigFile() {
        return domainConfigFile;
    }

    public void setDomainConfigFile(String domainConfigFile) {
        this.domainConfigFile = domainConfigFile;
    }

    public String getHostConfigFile() {
        return hostConfigFile;
    }

    public void setHostConfigFile(String hostConfigFile) {
        this.hostConfigFile = hostConfigFile;
    }

    public String getHostCommandLineProperties() {
        return hostCommandLineProperties;
    }

    public void setHostCommandLineProperties(String hostCommandLineProperties) {
        this.hostCommandLineProperties = hostCommandLineProperties;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getModulePath() {
        return modulePath;
    }

    public void setModulePath(final String modulePath) {
        this.modulePath = modulePath;
    }
}
