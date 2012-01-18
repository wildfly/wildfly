/*
 * JBoss, Home of Professional Open Source Copyright 2010, Red Hat Inc., and individual contributors as indicated by the
 *
 * @authors tag. See the copyright.txt in the distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.web;

import static org.jboss.as.web.WebMessages.MESSAGES;

import javax.management.MBeanServer;

import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.AprLifecycleListener;
import org.apache.catalina.core.JasperListener;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.core.StandardService;
import org.apache.tomcat.util.modeler.Registry;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Service configuring and starting the web container.
 *
 * @author Emanuel Muckenhuber
 */
class WebServerService implements WebServer, Service<WebServer> {

    private static final String JBOSS_WEB = "jboss.web";

    private final String defaultHost;
    private final boolean useNative;
    private final String instanceId;

    private Engine engine;
    private StandardServer server;
    private StandardService service;

    private final InjectedValue<MBeanServer> mbeanServer = new InjectedValue<MBeanServer>();
    private final InjectedValue<String> pathInjector = new InjectedValue<String>();

    public WebServerService(final String defaultHost, final boolean useNative, final String instanceId) {
        this.defaultHost = defaultHost;
        this.useNative = useNative;
        this.instanceId = instanceId;
    }

    /** {@inheritDoc} */
    public synchronized void start(StartContext context) throws StartException {
        if (org.apache.tomcat.util.Constants.ENABLE_MODELER) {
            // Set the MBeanServer
            final MBeanServer mbeanServer = this.mbeanServer.getOptionalValue();
            if(mbeanServer != null) {
                Registry.getRegistry(null, null).setMBeanServer(mbeanServer);
            }
        }

        System.setProperty("catalina.home", pathInjector.getValue());
        final StandardServer server = new StandardServer();

        final StandardService service = new StandardService();
        service.setName(JBOSS_WEB);
        service.setServer(server);
        server.addService(service);

        final Engine engine = new StandardEngine();
        engine.setName(JBOSS_WEB);
        engine.setService(service);
        engine.setDefaultHost(defaultHost);
        if (instanceId != null) {
            engine.setJvmRoute(instanceId);
        }

        service.setContainer(engine);

        if (useNative) {
            final AprLifecycleListener apr = new AprLifecycleListener();
            apr.setSSLEngine("on");
            server.addLifecycleListener(apr);
        }
        server.addLifecycleListener(new JasperListener());

        try {
            server.init();
            server.start();
        } catch (Exception e) {
            throw new StartException(MESSAGES.errorStartingWeb(), e);
        }
        this.server = server;
        this.service = service;
        this.engine = engine;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void stop(StopContext context) {
        try {
            server.stop();
        } catch (Exception e) {
        }
        engine = null;
        service = null;
        server = null;
    }

    /** {@inheritDoc} */
    public synchronized WebServer getValue() throws IllegalStateException {
        return this;
    }

    /** {@inheritDoc} */
    public synchronized void addConnector(Connector connector) {
        final StandardService service = this.service;
        service.addConnector(connector);
    }

    /** {@inheritDoc} */
    public synchronized void removeConnector(Connector connector) {
        final StandardService service = this.service;
        service.removeConnector(connector);
    }

    /** {@inheritDoc} */
    public synchronized void addHost(Host host) {
        final Engine engine = this.engine;
        engine.addChild(host);
    }

    /** {@inheritDoc} */
    public synchronized void removeHost(Host host) {
        final Engine engine = this.engine;
        engine.removeChild(host);
    }

    InjectedValue<MBeanServer> getMbeanServer() {
        return mbeanServer;
    }

    InjectedValue<String> getPathInjector() {
        return pathInjector;
    }

    public StandardServer getServer() {
        return server;
    }

    public StandardService getService() {
        return service;
    }

}
