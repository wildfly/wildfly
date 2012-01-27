/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.webservices.config;

import java.io.File;

import javax.management.MBeanServer;

import org.jboss.as.server.ServerEnvironment;
import org.jboss.msc.value.InjectedValue;
import org.jboss.ws.common.management.AbstractServerConfig;
import org.jboss.ws.common.management.AbstractServerConfigMBean;

/**
 * AS specific ServerConfig.
 *
 * @author <a href="mailto:asoldano@redhat.com">Alessio Soldano</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:tdiesler@redhat.com">Thomas Diesler</a>
 */
public final class ServerConfigImpl extends AbstractServerConfig implements AbstractServerConfigMBean {

    private final InjectedValue<MBeanServer> injectedMBeanServer = new InjectedValue<MBeanServer>();
    private final InjectedValue<ServerEnvironment> injectedServerEnvironment = new InjectedValue<ServerEnvironment>();

    private ServerConfigImpl() {
        // forbidden inheritance
    }

    public File getServerTempDir() {
        return getServerEnvironment().getServerTempDir();
    }

    public File getHomeDir() {
        return getServerEnvironment().getHomeDir();
    }

    public File getServerDataDir() {
        return getServerEnvironment().getServerDataDir();
    }

    @Override
    public MBeanServer getMbeanServer() {
        return injectedMBeanServer.getValue();
    }

    @Override
    public void setMbeanServer(final MBeanServer mbeanServer) {
        throw new UnsupportedOperationException();
    }

    public InjectedValue<MBeanServer> getMBeanServerInjector() {
        return injectedMBeanServer;
    }

    public InjectedValue<ServerEnvironment> getServerEnvironmentInjector() {
        return injectedServerEnvironment;
    }

    private ServerEnvironment getServerEnvironment() {
        return injectedServerEnvironment.getValue();
    }

    public static ServerConfigImpl newInstance() {
        return new ServerConfigImpl();
    }

}
