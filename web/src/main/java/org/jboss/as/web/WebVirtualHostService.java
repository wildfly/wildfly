/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.web;

import org.apache.catalina.Host;
import org.apache.catalina.Valve;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.valves.AccessLogValve;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.web.rewrite.RewriteValve;

/**
 * Service creating and registering a virtual host.
 *
 * @author Emanuel Muckenhuber
 */
class WebVirtualHostService implements Service<Host> {

    private final String name;
    private final String[] aliases;
    private ModelNode accessLog;
    private ModelNode rewrite;

    private final InjectedValue<String> tempPathInjector = new InjectedValue<String>();
    private final InjectedValue<String> accessLogPathInjector = new InjectedValue<String>();
    private final InjectedValue<WebServer> webServer = new InjectedValue<WebServer>();

    private Host host;

    public WebVirtualHostService(String name, String[] aliases) {
        this.name = name;
        this.aliases = aliases;
    }

    /** {@inheritDoc} */
    public synchronized void start(StartContext context) throws StartException {
        final StandardHost host = new StandardHost();
        host.setAppBase(tempPathInjector.getValue());
        host.setName(name);
        for(final String alias : aliases) {
            host.addAlias(alias);
        }
        if(accessLog != null) {
            host.addValve(createAccessLogValve(accessLogPathInjector.getValue(), accessLog));
        }
        if(rewrite != null) {
            host.addValve(createRewriteValve(rewrite));
        }
        // FIXME: default webapp
        try {
            final WebServer server = webServer.getValue();
            server.addHost(host);
        } catch(Exception e) {
            throw new StartException(e);
        }
        this.host = host;
    }

    /** {@inheritDoc} */
    public synchronized void stop(StopContext context) {
        final Host host = this.host;
        this.host = null;
        final WebServer server = webServer.getValue();
        server.removeHost(host);
    }

    /** {@inheritDoc} */
    public synchronized Host getValue() throws IllegalStateException {
        final Host host = this.host;
        if(host == null) {
            throw new IllegalStateException();
        }
        return host;
    }

    void setAccessLog(final ModelNode accessLog) {
        this.accessLog = accessLog;
    }

    void setRewrite(ModelNode rewrite) {
        this.rewrite = rewrite;
    }

    InjectedValue<String> getAccessLogPathInjector() {
        return accessLogPathInjector;
    }

    InjectedValue<String> getTempPathInjector() {
        return tempPathInjector;
    }

    InjectedValue<WebServer> getWebServer() {
        return webServer;
    }

    static Valve createAccessLogValve(final String logDirectory, final ModelNode element) {
        final AccessLogValve log = new AccessLogValve();
        log.setDirectory(logDirectory);
        log.setResolveHosts(element.get(Constants.RESOLVE_HOSTS).asBoolean());
        log.setRotatable(element.get(Constants.ROTATE).asBoolean());
        log.setPattern(element.get(Constants.PATTERN).asString());
        log.setPrefix(element.get(Constants.PREFIX).asString());
        // TODO extended?
        return log;
    }

    static Valve createRewriteValve(final ModelNode element) {
        final RewriteValve rewrite = new RewriteValve();
        // TODO
        return rewrite;
    }

}
