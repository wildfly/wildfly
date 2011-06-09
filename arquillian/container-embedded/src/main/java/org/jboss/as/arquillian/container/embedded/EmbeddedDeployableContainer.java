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
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Properties;

import javax.management.MBeanServerConnection;

import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.context.annotation.ContainerScoped;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.as.arquillian.container.CommonDeployableContainer;
import org.jboss.as.embedded.EmbeddedServerFactory;
import org.jboss.as.embedded.StandaloneServer;

/**
 * JBossASEmbeddedContainer
 *
 * @author Thomas.Diesler@jboss.com
 * @author Kabir Khan
 * @author <a href="mailto:alr@jboss.org">Andrew Lee Rubinger</a>
 * @since 17-Nov-2010
 */
public final class EmbeddedDeployableContainer extends CommonDeployableContainer<EmbeddedContainerConfiguration> {
    private StandaloneServer server;

    @Inject
    @ContainerScoped
    private InstanceProducer<MBeanServerConnection> mbeanServerInst;

    @Override
    protected void startInternal() throws LifecycleException {
        try {
            String jbossHomeKey = "jboss.home";
            String jbossHomeProp = System.getProperty(jbossHomeKey);
            if (jbossHomeProp == null)
                throw new IllegalStateException("Cannot find system property: " + jbossHomeKey);

            File jbossHomeDir = new File(jbossHomeProp).getAbsoluteFile();
            if (jbossHomeDir.isDirectory() == false)
                throw new IllegalStateException("Invalid jboss home directory: " + jbossHomeDir);

            File modulesJar = new File(jbossHomeDir + "/jboss-modules.jar");
            if (modulesJar.exists() == false)
                throw new IllegalStateException("Cannot find: " + modulesJar);

            Properties sysprops = new Properties();
            sysprops.putAll(System.getProperties());
            sysprops.setProperty("jboss.home.dir", jbossHomeDir.getAbsolutePath());
            sysprops.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
            sysprops.setProperty("logging.configuration", "file:" + jbossHomeDir + "/standalone/configuration/logging.properties");
            sysprops.setProperty("org.jboss.boot.log.file", jbossHomeDir + "/standalone/log/boot.log");

            mbeanServerInst.set(getMBeanServerConnection(5000));

            server = EmbeddedServerFactory.create(jbossHomeDir, sysprops, System.getenv(), getSystemPackages(sysprops, "org.jboss.logmanager"));
            server.start();

        } catch (Throwable th) {
            throw handleStartThrowable(th);
        }
    }

    private String[] getSystemPackages(Properties props, String...packages) {
        if (Boolean.valueOf(props.getProperty("org.jboss.surefire.modular", Boolean.FALSE.toString()))){
            //The forked surefire plugin passes in this property, so we don't need system packages to work
            return new String[0];
        }
        return packages;
    }

    @Override
    protected void stopInternal() throws LifecycleException {
        try {
            if (server != null)
                server.stop();
        } catch (Exception e) {
            throw new LifecycleException("Could not stop container", e);
        }
    }

    @Override
    public MBeanServerConnection getMBeanServerConnection() {
        return ManagementFactory.getPlatformMBeanServer();
    }

    private LifecycleException handleStartThrowable(Throwable th) throws LifecycleException {
        if (th instanceof UndeclaredThrowableException)
            throw handleStartThrowable(((UndeclaredThrowableException) th).getUndeclaredThrowable());

        if (th instanceof InvocationTargetException)
            throw handleStartThrowable(((InvocationTargetException) th).getTargetException());

        if (th instanceof RuntimeException)
            throw (RuntimeException) th;

        return new LifecycleException("Could not start container", th);
    }

    @Override
    public Class<EmbeddedContainerConfiguration> getConfigurationClass() {
        return EmbeddedContainerConfiguration.class;
    }
}