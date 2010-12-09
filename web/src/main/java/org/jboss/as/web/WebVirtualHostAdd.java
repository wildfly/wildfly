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

import java.util.Set;

import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.as.server.services.path.AbstractPathService;
import org.jboss.as.server.services.path.RelativePathService;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.ServiceBuilder;

/**
 * @author Emanuel Muckenhuber
 */
public class WebVirtualHostAdd extends AbstractWebSubsystemUpdate<Void> {

    private static final long serialVersionUID = 727085265030986640L;
    private static final String TEMP_DIR = "jboss.server.temp.dir";

    private final String name;
    private Set<String> aliases;
    private WebHostAccessLogElement accessLog;
    private WebHostRewriteElement rewrite;

    public WebVirtualHostAdd(String name) {
        if(name == null) {
            throw new IllegalArgumentException("null host name");
        }
        this.name = name;
    }

    /** {@inheritDoc} */
    protected void applyUpdate(WebSubsystemElement element) throws UpdateFailedException {
        final WebVirtualHostElement host = element.addHost(name);
        if(host == null) {
            throw new IllegalStateException("duplicate host " + name);
        }
        host.setAliases(aliases);
    }

    /** {@inheritDoc} */
    protected <P> void applyUpdate(UpdateContext updateContext, UpdateResultHandler<? super Void, P> resultHandler, P param) {
        final BatchBuilder builder = updateContext.getServiceTarget();
        final WebVirtualHostService service = new WebVirtualHostService(name, aliases());
        final ServiceBuilder<?> serviceBuilder =  builder.addService(WebSubsystemElement.JBOSS_WEB_HOST.append(name), service)
            .addDependency(AbstractPathService.pathNameOf(TEMP_DIR), String.class, service.getTempPathInjector())
            .addDependency(WebSubsystemElement.JBOSS_WEB, WebServer.class, service.getWebServer());
        if(accessLog != null) {
            service.setAccessLog(accessLog);
            // Create the access log service
            accessLogService(name, accessLog.getDirectory(), builder);
            serviceBuilder.addDependency(WebSubsystemElement.JBOSS_WEB_HOST.append(name, "access-log"), String.class, service.getAccessLogPathInjector());
        }
        if(rewrite != null) {
            service.setRewrite(rewrite);
        }
        serviceBuilder.install();
    }

    /** {@inheritDoc} */
    public AbstractSubsystemUpdate<WebSubsystemElement, ?> getCompensatingUpdate(WebSubsystemElement original) {
        return new WebVirtualHostRemove(name);
    }

    public String getName() {
        return name;
    }

    public Set<String> getAliases() {
        return aliases;
    }

    public void setAliases(Set<String> aliases) {
        this.aliases = aliases;
    }

    public WebHostAccessLogElement getAccessLog() {
        return accessLog;
    }

    public void setAccessLog(WebHostAccessLogElement accessLog) {
        this.accessLog = accessLog;
    }

    public WebHostRewriteElement getRewrite() {
        return rewrite;
    }

    public void setRewrite(WebHostRewriteElement rewrite) {
        this.rewrite = rewrite;
    }

    private String[] aliases() {
        if(aliases != null && ! aliases.isEmpty()) {
            return aliases.toArray(new String[aliases.size()]);
        }
        return new String[0];
    }

    static WebVirtualHostAdd create(final WebVirtualHostElement host) {
        final WebVirtualHostAdd action = new WebVirtualHostAdd(host.getName());
        action.setAliases(host.getAliases());
        action.setAccessLog(host.getAccessLog());
        action.setRewrite(host.getRewrite());
        return action;
    }

    static final String DEFAULT_RELATIVE_TO = "jboss.server.log.dir";

    static void accessLogService(final String hostName, final WebHostAccessLogElement.LogDirectory directory, final BatchBuilder batchBuilder) {
        if(directory == null) {
            RelativePathService.addService(WebSubsystemElement.JBOSS_WEB_HOST.append(hostName, "access-log"),
                    hostName, DEFAULT_RELATIVE_TO, batchBuilder);
        } else {
            final String relativeTo = directory.getRelativeTo() != null ? directory.getRelativeTo() : DEFAULT_RELATIVE_TO;
            final String path = directory.getPath() != null ? directory.getPath() : hostName;
            RelativePathService.addService(WebSubsystemElement.JBOSS_WEB_HOST.append(hostName, "access-log"),
                    path, relativeTo, batchBuilder);
        }
    }

}
