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

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.jboss.arquillian.container.spi.ConfigurationException;
import org.jboss.as.arquillian.container.DistributionContainerConfiguration;

/**
 * The managed container configuration
 *
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @author Thomas.Diesler@jboss.com
 */
public class ManagedContainerConfiguration extends DistributionContainerConfiguration {

    /**
     * Default timeout value waiting on ports is 10 seconds
     */
    private static final Integer DEFAULT_VALUE_WAIT_FOR_PORTS_TIMEOUT_SECONDS = 10;

    private String javaVmArguments = System.getProperty("jboss.options", "-Xmx512m -XX:MaxPermSize=128m");

    private int startupTimeoutInSeconds = 60;

    private int stopTimeoutInSeconds = 60;

    private boolean outputToConsole = true;

    private String serverConfig = System.getProperty("jboss.server.config.file.name", "standalone.xml");

    private boolean allowConnectingToRunningServer = Boolean.parseBoolean(System.getProperty(
        "allowConnectingToRunningServer", "false"));

    private boolean enableAssertions = true;

    private boolean adminOnly = false;

    private Integer[] waitForPorts;

    private Integer waitForPortsTimeoutInSeconds;

    public ManagedContainerConfiguration() {
        super();
    }

    @Override
    public void validate() throws ConfigurationException {
        super.validate();
    }

    /**
     * @return the javaVmArguments
     */
    public String getJavaVmArguments() {
        return javaVmArguments;
    }

    /**
     * @param javaVmArguments
     *            the javaVmArguments to set
     */
    public void setJavaVmArguments(String javaVmArguments) {
        this.javaVmArguments = javaVmArguments;
    }

    /**
     * @param startupTimeoutInSeconds
     *            the startupTimeoutInSeconds to set
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

    public void setStopTimeoutInSeconds(int stopTimeoutInSeconds) {
        this.stopTimeoutInSeconds = stopTimeoutInSeconds;
    }

    /**
     * @return stopTimeoutInSeconds number of seconds to wait for the container process to shutdown;
     *                              defaults to 60
     */
    public int getStopTimeoutInSeconds() {
        return stopTimeoutInSeconds;
    }

    /**
     * @param outputToConsole
     *            the outputToConsole to set
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
     * Get the server configuration file name. Equivalent to [-server-config=...] on the command line.
     *
     * @return the server config
     */
    public String getServerConfig() {
        return serverConfig;
    }

    /**
     * Set the server configuration file name. Equivalent to [-server-config=...] on the command line.
     *
     * @param serverConfig
     *            the server config
     */
    public void setServerConfig(String serverConfig) {
        this.serverConfig = serverConfig;
    }

    public boolean isAllowConnectingToRunningServer() {
        return allowConnectingToRunningServer;
    }

    public void setAllowConnectingToRunningServer(final boolean allowConnectingToRunningServer) {
        this.allowConnectingToRunningServer = allowConnectingToRunningServer;
    }

    public boolean isEnableAssertions() {
        return enableAssertions;
    }

    public void setEnableAssertions(final boolean enableAssertions) {
        this.enableAssertions = enableAssertions;
    }

    public Integer[] getWaitForPorts() {
        return waitForPorts;
    }

    public void setWaitForPorts(String waitForPorts) {
        final Scanner scanner = new Scanner(waitForPorts);
        final List<Integer> list = new ArrayList<Integer>();
        while (scanner.hasNextInt()) {
            list.add(scanner.nextInt());
        }
        this.waitForPorts = list.toArray(new Integer[] {});
    }

    public Integer getWaitForPortsTimeoutInSeconds() {
        return waitForPortsTimeoutInSeconds != null ? waitForPortsTimeoutInSeconds
            : DEFAULT_VALUE_WAIT_FOR_PORTS_TIMEOUT_SECONDS;
    }

    public void setWaitForPortsTimeoutInSeconds(final Integer waitForPortsTimeoutInSeconds) {
        this.waitForPortsTimeoutInSeconds = waitForPortsTimeoutInSeconds;
    }

    public boolean isAdminOnly() {
        return adminOnly;
    }

    public void setAdminOnly(boolean adminOnly) {
        this.adminOnly = adminOnly;
    }

}
