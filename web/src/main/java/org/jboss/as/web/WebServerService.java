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

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.AprLifecycleListener;
import org.apache.catalina.core.JasperListener;
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
    private final boolean useNative;

    private Engine engine;
    private Catalina catalina;
    private StandardService service;

    private final InjectedValue<MBeanServer> mbeanServer = new InjectedValue<MBeanServer>();
    private final InjectedValue<String> pathInjector = new InjectedValue<String>();

    public WebServerService(final String defaultHost, final boolean useNative) {
        this.defaultHost = defaultHost;
        this.useNative = useNative;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void start(StartContext context) throws StartException {
        if (org.apache.tomcat.util.Constants.ENABLE_MODELER) {
            // Set the MBeanServer
            final MBeanServer mbeanServer = this.mbeanServer.getOptionalValue();
            if(mbeanServer != null) {
                Registry.getRegistry(null, null).setMBeanServer(mbeanServer);
            }
        }

        final Catalina catalina = new Catalina();
        catalina.setCatalinaHome(pathInjector.getValue());

        final StandardServer server = new StandardServer();
        catalina.setServer(server);
        registerObject(mbeanServer, "jboss.web:type=Server", server,  "org.apache.catalina.startup.StandardServer");

        final StandardService service = new StandardService();
        registerObject(mbeanServer, "jboss.web:service=WebServer", service, "org.apache.catalina.core.StandardService");
        service.setName(JBOSS_WEB);
        service.setServer(server);
        server.addService(service);

        final Engine engine = new StandardEngine();
        engine.setName(JBOSS_WEB);
        engine.setService(service);
        engine.setDefaultHost(defaultHost);

        service.setContainer(engine);

        if (useNative) {
            final AprLifecycleListener apr = new AprLifecycleListener();
            apr.setSSLEngine("on");
            server.addLifecycleListener(apr);
        }
        server.addLifecycleListener(new JasperListener());

        try {
            catalina.create();
            server.initialize();
            catalina.start();
            // Register here ?
        } catch (Exception e) {
            throw new StartException(e);
        }
        this.catalina = catalina;
        this.service = service;
        this.engine = engine;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void stop(StopContext context) {
        catalina.stop();
        catalina.destroy();
        engine = null;
        service = null;
        catalina = null;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized WebServer getValue() throws IllegalStateException {
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void addConnector(Connector connector) {
        final StandardService service = this.service;
        service.addConnector(connector);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void removeConnector(Connector connector) {
        final StandardService service = this.service;
        service.removeConnector(connector);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void addHost(Host host) {
        final Engine engine = this.engine;
        engine.addChild(host);
    }

    /** {@inheritDoc} */
    @Override
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

    Registry getRegistry() {
        return Registry.getRegistry(null, null);
    }
    void registerObject(MBeanServer mbeanServer, String name, Object obj, String classname) {
        if (mbeanServer != null) {
            ObjectName objectName;
            try {
                objectName = new ObjectName(name);
                getRegistry().registerComponent(obj, objectName, classname);
            } catch (Exception e) {
                return;
            }
        }
    }

}
