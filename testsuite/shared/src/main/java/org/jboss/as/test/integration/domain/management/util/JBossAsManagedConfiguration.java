/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2014, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */
package org.jboss.as.test.integration.domain.management.util;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import javax.security.auth.callback.CallbackHandler;

import org.jboss.arquillian.container.spi.ConfigurationException;
import org.jboss.arquillian.container.spi.client.deployment.Validate;
import org.jboss.as.arquillian.container.CommonContainerConfiguration;
import org.jboss.as.test.shared.TimeoutUtil;

/**
 * JBossAsManagedConfiguration
 *
 * @author Brian Stansberry
 */
public class JBossAsManagedConfiguration extends CommonContainerConfiguration {

    public static JBossAsManagedConfiguration createFromClassLoaderResources(String domainConfigPath,
                                                                             String hostConfigPath) {
        JBossAsManagedConfiguration result = new JBossAsManagedConfiguration();
        if (domainConfigPath != null) {
            result.setDomainConfigFile(loadConfigFileFromContextClassLoader(domainConfigPath));
        }
        if (hostConfigPath != null) {
            result.setHostConfigFile(hostConfigPath);
        }
        return result;
    }

    public static String loadConfigFileFromContextClassLoader(String resourcePath) {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        URL url = tccl.getResource(resourcePath);
        assert url != null : "cannot find resource at path " + resourcePath;
        return new File(toURI(url)).getAbsolutePath();
    }

    private static URI toURI(URL url) {
        try {
            return url.toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private String jbossHome = System.getProperty("jboss.home");

    private String javaHome = System.getenv("JAVA_HOME");

    private String controllerJavaHome = System.getenv("JAVA_HOME");

    private String modulePath = System.getProperty("module.path");

    private String javaVmArguments = System.getProperty("server.jvm.args", "-Xmx512m");

    private int startupTimeoutInSeconds = TimeoutUtil.adjust(120);

    private boolean outputToConsole = true;

    private String hostControllerManagementProtocol = "remote";

    private String hostControllerManagementAddress = System.getProperty("jboss.test.host.master.address", "localhost");

    private int hostControllerManagementPort = 9999;

    private String hostName = "master";

    private String domainDir;

    private String domainConfigFile;

    private String hostConfigFile;

    private String hostCommandLineProperties;

    private boolean adminOnly;

    private boolean readOnlyDomain;

    private boolean readOnlyHost;

    private CallbackHandler callbackHandler = Authentication.getCallbackHandler();

    private String mgmtUsersFile;

    private String mgmtGroupsFile;

    private boolean backupDC;

    private boolean cachedDC;

    public JBossAsManagedConfiguration(String jbossHome) {
        if (jbossHome != null) {
            this.jbossHome = jbossHome;
            this.modulePath = new File(jbossHome, "modules").getAbsolutePath();
        }
    }

    public JBossAsManagedConfiguration() {
    }

    /*
     * (non-Javadoc)
     *
     * @see org.jboss.as.arquillian.container.JBossAsContainerConfiguration#validate()
     */
    @Override
    public void validate() throws ConfigurationException {
        super.validate();

        Validate.configurationDirectoryExists(jbossHome, "jbossHome must exist at " + jbossHome);
        if (javaHome != null) {
            Validate.configurationDirectoryExists(javaHome, "javaHome must exist at " + javaHome);
        }
        if (controllerJavaHome != null) {
            Validate.configurationDirectoryExists(controllerJavaHome, "controllerJavaHome must exist at " + controllerJavaHome);
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
     * @return the controllerJavaHome
     */
    public String getControllerJavaHome() {
        return controllerJavaHome;
    }

    /**
     * @param controllerJavaHome the javaHome to set
     */
    public void setControllerJavaHome(String controllerJavaHome) {
        this.controllerJavaHome = controllerJavaHome;
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

    public String getHostControllerManagementProtocol() {
        return hostControllerManagementProtocol;
    }

    public void setHostControllerManagementProtocol(String hostControllerManagementProtocol) {
        this.hostControllerManagementProtocol = hostControllerManagementProtocol;
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

    public String getMgmtUsersFile() {
        return mgmtUsersFile;
    }

    public void setMgmtUsersFile(String mgmtUsersFile) {
        this.mgmtUsersFile = mgmtUsersFile;
    }

    public String getMgmtGroupsFile() {
        return mgmtGroupsFile;
    }

    public void setMgmtGroupsFile(String mgmtGroupsFile) {
        this.mgmtGroupsFile = mgmtGroupsFile;
    }

    public String getHostCommandLineProperties() {
        return hostCommandLineProperties;
    }

    public void setHostCommandLineProperties(String hostCommandLineProperties) {
        this.hostCommandLineProperties = hostCommandLineProperties;
    }

    public void addHostCommandLineProperty(String hostCommandLineProperty) {
        this.hostCommandLineProperties = this.hostCommandLineProperties == null
                ? hostCommandLineProperty : this.hostCommandLineProperties + " " + hostCommandLineProperty;
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

    public boolean isAdminOnly() {
        return adminOnly;
    }

    public void setAdminOnly(boolean adminOnly) {
        this.adminOnly = adminOnly;
    }

    public boolean isReadOnlyDomain() {
        return readOnlyDomain;
    }

    public void setReadOnlyDomain(boolean readOnlyDomain) {
        this.readOnlyDomain = readOnlyDomain;
    }

    public boolean isReadOnlyHost() {
        return readOnlyHost;
    }

    public void setReadOnlyHost(boolean readOnlyHost) {
        this.readOnlyHost = readOnlyHost;
    }

    public CallbackHandler getCallbackHandler() {
        return callbackHandler;
    }

    public void setCallbackHandler(CallbackHandler callbackHandler) {
        this.callbackHandler = callbackHandler;
    }

    public boolean isBackupDC() {
        return backupDC;
    }

    public void setBackupDC(boolean backupDC) {
        this.backupDC = backupDC;
    }

    public boolean isCachedDC() {
        return cachedDC;
    }

    public void setCachedDC(boolean cachedDC) {
        this.cachedDC = cachedDC;
    }
}
