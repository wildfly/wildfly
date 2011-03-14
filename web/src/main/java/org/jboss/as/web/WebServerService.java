/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.web;

import javax.management.MBeanServer;

import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.ServiceMapperListener;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.core.StandardService;
import org.apache.catalina.startup.Catalina;
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

    private Engine engine;
    private Catalina catalina;
    private StandardService service;

    private final InjectedValue<MBeanServer> mbeanServer = new InjectedValue<MBeanServer>();
    private final InjectedValue<String> pathInjector = new InjectedValue<String>();

    public WebServerService(final String defaultHost) {
        this.defaultHost = defaultHost != null ? defaultHost : "localhost";
    }

    /** {@inheritDoc} */
    public synchronized void start(StartContext context) throws StartException {
        if (org.apache.tomcat.util.Constants.ENABLE_MODELER) {
            // Set the MBeanServer
            final MBeanServer mbeanServer = this.mbeanServer.getOptionalValue();
            if(mbeanServer != null) {
                getRegistry().setMBeanServer(mbeanServer);
            }
        }

        final Catalina catalina = new Catalina();
        catalina.setCatalinaHome(pathInjector.getValue());

        final StandardServer server = new StandardServer();
        catalina.setServer(server);

        final StandardService service = new StandardService();
        service.setName(JBOSS_WEB);
        service.setServer(server);
        server.addService(service);

        final Engine engine = new StandardEngine();
        engine.setName(JBOSS_WEB);
        engine.setService(service);
        engine.setDefaultHost(defaultHost);

        service.setContainer(engine);

        // final AprLifecycleListener apr = new AprLifecycleListener();
        //apr.setSSLEngine("on");
        // server.addLifecycleListener(apr);
        // server.addLifecycleListener(new JasperListener());

        try {
            catalina.create();
            server.initialize();
            catalina.start();
        } catch (Exception e) {
            throw new StartException(e);
        }
        this.catalina = catalina;
        this.service = service;
        this.engine = engine;
    }

    /** {@inheritDoc} */
    public synchronized void stop(StopContext context) {
        catalina.stop();
        catalina.destroy();
        engine = null;
        service = null;
        catalina = null;
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
        // FIXME: Hack, remove with next JBW build
        for (LifecycleListener listener : service.findLifecycleListeners()) {
            if (listener instanceof ServiceMapperListener) {
                host.addContainerListener((ServiceMapperListener) listener);
            }
        }
    }

    /** {@inheritDoc} */
    public synchronized void removeHost(Host host) {
        final Engine engine = this.engine;
        engine.removeChild(host);
        // FIXME: Hack, remove with next JBW build
        for (LifecycleListener listener : service.findLifecycleListeners()) {
            if (listener instanceof ServiceMapperListener) {
                host.removeContainerListener((ServiceMapperListener) listener);
            }
        }
    }

    InjectedValue<MBeanServer> getMbeanServer() {
        return mbeanServer;
    }

    InjectedValue<String> getPathInjector() {
        return pathInjector;
    }

    Registry getRegistry() {
        return Registry.getRegistry(null, null);
    }

}
