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

import org.apache.catalina.Container;
import org.apache.catalina.Valve;
import org.apache.catalina.authenticator.SingleSignOn;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.valves.AccessLogValve;
import org.apache.catalina.valves.ExtendedAccessLogValve;
import org.jboss.as.clustering.web.sso.SSOClusterManager;
import org.jboss.as.web.sso.ClusteredSingleSignOn;
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
public class WebVirtualHostService implements Service<VirtualHost> {

    private final String name;
    private final String[] aliases;
    private String defaultWebModule;
    private boolean hasWelcomeRoot;
    private ModelNode accessLog;
    private ModelNode rewrite;
    private ModelNode sso;

    private final InjectedValue<String> tempPathInjector = new InjectedValue<String>();
    private final InjectedValue<String> accessLogPathInjector = new InjectedValue<String>();
    private final InjectedValue<WebServer> webServer = new InjectedValue<WebServer>();
    private final InjectedValue<SSOClusterManager> ssoManager = new InjectedValue<SSOClusterManager>();

    private VirtualHost host;

    public WebVirtualHostService(String name, String[] aliases, boolean hasWelcomeRoot) {
        this.name = name;
        this.aliases = aliases;
        this.hasWelcomeRoot = hasWelcomeRoot;
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
            host.addValve(createAccessLogValve(host, accessLogPathInjector.getValue(), accessLog));
        }
        if(rewrite != null) {
            host.addValve(createRewriteValve(host, rewrite));
        }
        if(sso != null) {
            host.addValve(createSsoValve(host, sso));
        }
        if (defaultWebModule != null) {
            host.setDefaultWebapp(defaultWebModule);
        }
        try {
            final WebServer server = webServer.getValue();
            server.addHost(host);
        } catch(Exception e) {
            throw new StartException(e);
        }
        this.host = new VirtualHost(host, hasWelcomeRoot);

    }

    /** {@inheritDoc} */
    public synchronized void stop(StopContext context) {
        final VirtualHost host = this.host;
        this.host = null;
        final WebServer server = webServer.getValue();
        server.removeHost(host.getHost());
    }

    /** {@inheritDoc} */
    public synchronized VirtualHost getValue() throws IllegalStateException {
        final VirtualHost host = this.host;
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

    void setSso(final ModelNode sso) {
        this.sso = sso;
    }

    protected String getDefaultWebModule() {
        return defaultWebModule;
    }

    protected void setDefaultWebModule(String defaultWebModule) {
        this.defaultWebModule = defaultWebModule;
    }

    public InjectedValue<String> getAccessLogPathInjector() {
        return accessLogPathInjector;
    }

    public InjectedValue<String> getTempPathInjector() {
        return tempPathInjector;
    }

    public InjectedValue<WebServer> getWebServer() {
        return webServer;
    }

    public InjectedValue<SSOClusterManager> getSSOClusterManager() {
        return ssoManager;
    }

    static Valve createAccessLogValve(final Container container, final String logDirectory, final ModelNode element) {
        boolean extended = false;
        if (element.hasDefined(Constants.EXTENDED)) {
            extended = element.get(Constants.EXTENDED).asBoolean();
        }
        final AccessLogValve log;
        if (extended) {
            log = new ExtendedAccessLogValve();
        } else {
            log = new AccessLogValve();
        }
        log.setDirectory(logDirectory);
        if (element.hasDefined(Constants.RESOLVE_HOSTS)) log.setResolveHosts(element.get(Constants.RESOLVE_HOSTS).asBoolean());
        if (element.hasDefined(Constants.ROTATE)) log.setRotatable(element.get(Constants.ROTATE).asBoolean());
        if (element.hasDefined(Constants.PATTERN)) {
            log.setPattern(element.get(Constants.PATTERN).asString());
        } else {
            log.setPattern("common");
        }
        if (element.hasDefined(Constants.PREFIX)) log.setPrefix(element.get(Constants.PREFIX).asString());
        return log;
    }

    static Valve createRewriteValve(final Container container, final ModelNode element) throws StartException {
        final RewriteValve rewriteValve = new RewriteValve();
        rewriteValve.setContainer(container);
        StringBuffer configuration = new StringBuffer();
        for (final ModelNode rewrite : element.asList()) {
            if (rewrite.has(Constants.CONDITION)) {
                for (final ModelNode condition : rewrite.get(Constants.CONDITION).asList()) {
                    configuration.append("RewriteCond ")
                    .append(condition.get(Constants.TEST).asString())
                    .append(" ").append(condition.get(Constants.PATTERN).asString());
                    if (condition.hasDefined(Constants.FLAGS)) {
                        configuration.append(" [").append(condition.get(Constants.FLAGS).asString()).append("]\r\n");
                    } else {
                        configuration.append("\r\n");
                    }
                }
            }
            configuration.append("RewriteRule ")
            .append(rewrite.get(Constants.PATTERN).asString())
            .append(" ").append(rewrite.get(Constants.SUBSTITUTION).asString());
            if (rewrite.hasDefined(Constants.FLAGS)) {
                configuration.append(" [").append(rewrite.get(Constants.FLAGS).asString()).append("]\r\n");
            } else {
                configuration.append("\r\n");
            }
        }
        try {
            rewriteValve.setConfiguration(configuration.toString());
        } catch(Exception e) {
            throw new StartException(e);
        }
        return rewriteValve;
    }

    Valve createSsoValve(final Container container, final ModelNode element) throws StartException {
        final SingleSignOn ssoValve = element.hasDefined(Constants.CACHE_CONTAINER) ? new ClusteredSingleSignOn(this.ssoManager.getValue()) : new SingleSignOn();
        if (element.hasDefined(Constants.DOMAIN)) ssoValve.setCookieDomain(element.get(Constants.DOMAIN).asString());
        if (element.hasDefined(Constants.REAUTHENTICATE)) ssoValve.setRequireReauthentication(element.get(Constants.REAUTHENTICATE).asBoolean());
        return ssoValve;
    }

}
