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
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.web.rewrite.RewriteValve;
import org.wildfly.clustering.web.catalina.sso.LocalSSOContextFactory;
import org.wildfly.clustering.web.catalina.sso.SingleSignOnFacade;
import org.wildfly.clustering.web.sso.SSOManagerFactory;

/**
 * Service creating and registering a virtual host.
 *
 * @author Emanuel Muckenhuber
 */
public class WebVirtualHostService implements Service<VirtualHost> {

    private final String name;
    private final String[] aliases;
    private final String tempPathName;
    private String defaultWebModule;
    private boolean hasWelcomeRoot;
    private ModelNode accessLog;
    private ModelNode rewrite;
    private ModelNode sso;

    private final InjectedValue<PathManager> pathManagerInjector = new InjectedValue<PathManager>();
    private final InjectedValue<WebServer> webServer = new InjectedValue<WebServer>();
    private final InjectedValue<SSOManagerFactory> ssoManagerFactory = new InjectedValue<SSOManagerFactory>();

    private VirtualHost host;

    private volatile String accessLogPath;
    private volatile String accessLogRelativeTo;
    private PathManager.Callback.Handle callbackHandle;

    public WebVirtualHostService(String name, String[] aliases, boolean hasWelcomeRoot, String tempPathName) {
        this.name = name;
        this.aliases = aliases;
        this.hasWelcomeRoot = hasWelcomeRoot;
        this.tempPathName = tempPathName;
    }

    /** {@inheritDoc} */
    public synchronized void start(StartContext context) throws StartException {
        final StandardHost host = new StandardHost();
        host.setAppBase(pathManagerInjector.getValue().getPathEntry(tempPathName).resolvePath());
        host.setName(name);
        for(final String alias : aliases) {
            host.addAlias(alias);
        }
        if(accessLog != null) {
            host.addValve(createAccessLogValve(pathManagerInjector.getValue().resolveRelativePathEntry(accessLogPath, accessLogRelativeTo), accessLog));
            callbackHandle = pathManagerInjector.getValue().registerCallback(accessLogRelativeTo, PathManager.ReloadServerCallback.create(), PathManager.Event.UPDATED, PathManager.Event.REMOVED);
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
        if (callbackHandle != null) {
            callbackHandle.remove();
        }
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

    void setAccessLog(final ModelNode resolvedAccessLogModel) {
        this.accessLog = resolvedAccessLogModel;
    }

    void setAccessLogPaths(String accessLogPath, String accessLogRelativeTo) {
        this.accessLogPath = accessLogPath;
        this.accessLogRelativeTo = accessLogRelativeTo;
    }

    void setRewrite(ModelNode resolvedRewriteModel) {
        this.rewrite = resolvedRewriteModel;
    }

    void setSso(final ModelNode resolvedSsoModel) {
        this.sso = resolvedSsoModel;
    }

    @Deprecated
    protected String getDefaultWebModule() {
        return defaultWebModule;
    }

    protected void setDefaultWebModule(String defaultWebModule) {
        this.defaultWebModule = defaultWebModule;
    }

    public InjectedValue<PathManager> getPathManagerInjector() {
        return pathManagerInjector;
    }

    public InjectedValue<WebServer> getWebServer() {
        return webServer;
    }

    public Injector<SSOManagerFactory> getSSOManagerFactory() {
        return this.ssoManagerFactory;
    }

    static Valve createAccessLogValve(final String logDirectory, final ModelNode element) {
        boolean extended = element.get(Constants.EXTENDED).asBoolean(false);
        String pattern = null;
        if (element.hasDefined(Constants.PATTERN)) {
            pattern = element.get(Constants.PATTERN).asString();
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
        if (pattern != null) {
            log.setPattern(pattern);
        } else {
            if (extended) {
                log.setPattern("time cs-method cs-uri sc-status sc(Referer)");
            } else {
                log.setPattern("common");
            }
        }

        if (element.hasDefined(Constants.PREFIX)) log.setPrefix(element.get(Constants.PREFIX).asString());
        return log;
    }

    static Valve createRewriteValve(final Container container, final ModelNode element) throws StartException {
        final RewriteValve rewriteValve = new RewriteValve();
        rewriteValve.setContainer(container);
        StringBuilder configuration = new StringBuilder();
        for (final ModelNode rewriteElement : element.asList()) {
            final ModelNode rewrite = rewriteElement.asProperty().getValue();
            if (rewrite.has(Constants.CONDITION)) {
                for (final ModelNode conditionElement : rewrite.get(Constants.CONDITION).asList()) {
                    final ModelNode condition = conditionElement.asProperty().getValue();
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
        SSOManagerFactory factory = this.ssoManagerFactory.getOptionalValue();
        final SingleSignOn ssoValve = (factory != null) ? new SingleSignOnFacade(factory.createSSOManager(new LocalSSOContextFactory())) : new SingleSignOn();
        if (element.hasDefined(Constants.DOMAIN)) ssoValve.setCookieDomain(element.get(Constants.DOMAIN).asString());
        if (element.hasDefined(Constants.REAUTHENTICATE)) ssoValve.setRequireReauthentication(element.get(Constants.REAUTHENTICATE).asBoolean());
        return ssoValve;
    }
}
