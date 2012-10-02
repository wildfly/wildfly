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
package org.jboss.as.arquillian.container.embedded;

import java.io.File;
import java.util.Properties;

import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.as.arquillian.container.CommonDeployableContainer;
import org.jboss.as.embedded.EmbeddedServerFactory;
import org.jboss.as.embedded.StandaloneServer;

/**
 * Arqullian JBoss Embedded 7 Container implementation.
 *
 * @author Thomas.Diesler@jboss.com
 * @author Kabir Khan
 * @author <a href="mailto:aslak@conduct.no">Aslak Knutsen</a>
 * @author <a href="mailto:mmatloka@gmail.com">Michal Matloka</a>
 * @since 17-Nov-2010
 */
public class EmbeddedDeployableContainer extends CommonDeployableContainer<EmbeddedContainerConfiguration> {

    private StandaloneServer server;

    /**
     * @see org.jboss.as.arquillian.container.CommonDeployableContainer#getDefaultProtocol()
     */
    @Override
    public ProtocolDescription getDefaultProtocol() {
        return new ProtocolDescription("jmx-as7");
    }

    /**
     * @see org.jboss.as.arquillian.container.CommonDeployableContainer#getConfigurationClass()
     */
    @Override
    public Class<EmbeddedContainerConfiguration> getConfigurationClass() {
        return EmbeddedContainerConfiguration.class;
    }

    /**
     * @see org.jboss.as.arquillian.container.CommonDeployableContainer#setup(org.jboss.as.arquillian.container.CommonContainerConfiguration)
     */
    @Override
    public void setup(EmbeddedContainerConfiguration configuration) {
        super.setup(configuration);

        String jbossHome = configuration.getJbossHome();

        File jbossHomeDir = new File(jbossHome).getAbsoluteFile();
        if (!jbossHomeDir.isDirectory()) {
            throw new IllegalStateException("Invalid jboss home directory: " + jbossHomeDir);
        }

        String bundlePath = configuration.getBundlePath();
        if(bundlePath == null) {
            bundlePath = jbossHomeDir + "/bundles";
        }

        String modulePath = configuration.getModulePath();
        if(modulePath == null) {
            modulePath = jbossHomeDir + "/modules";
        }

        File modulesDir = new File(modulePath);
        if (!modulesDir.isDirectory()) {
            throw new IllegalStateException("Invalid modules directory: " + modulesDir);
        }

        File modulesJar = new File(jbossHomeDir + "/jboss-modules.jar");
        if (!modulesJar.exists()) {
            throw new IllegalStateException("Cannot find: " + modulesJar);
        }

        Properties sysprops = new Properties();
        sysprops.putAll(System.getProperties());
        sysprops.setProperty("jboss.home.dir", jbossHomeDir.getAbsolutePath());
        sysprops.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
        sysprops.setProperty("logging.configuration", "file:" + jbossHomeDir + "/standalone/configuration/logging.properties");
        sysprops.setProperty("org.jboss.boot.log.file", jbossHomeDir + "/standalone/log/boot.log");
        sysprops.setProperty("jboss.bundles.dir", bundlePath);

        server = EmbeddedServerFactory.create(jbossHomeDir, modulesDir, sysprops, System.getenv(),
                getSystemPackages(sysprops, "org.jboss.logmanager"));
        // server.getConfiguration()
        // .bindAddress(configuration.getBindAddress())
        // .serverName(configuration.getProfileName());
    }

    private String[] getSystemPackages(Properties props, String... packages) {
        if (Boolean.valueOf(props.getProperty("org.jboss.surefire.modular", Boolean.FALSE.toString()))) {
            // The forked surefire plugin passes in this property, so we don't
            // need system packages to work
            return new String[0];
        }
        return packages;
    }

    @Override
    protected void startInternal() throws LifecycleException {
        try {
            server.start();
        } catch (Exception e) {
            throw new LifecycleException("Could not start container", e);
        }
    }

    @Override
    protected void stopInternal() throws LifecycleException {
        try {
            if (server != null) {
                server.stop();
            }
        } catch (Exception e) {
            throw new LifecycleException("Could not stop container", e);
        }
    }
}
