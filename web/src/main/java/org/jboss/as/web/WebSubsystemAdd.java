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

import javax.management.MBeanServer;

import org.jboss.as.model.AbstractSubsystemAdd;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.as.services.path.AbstractPathService;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceController.Mode;

/**
 * @author Emanuel Muckenhuber
 */
public class WebSubsystemAdd extends AbstractSubsystemAdd<WebSubsystemElement> {

    private static final long serialVersionUID = 1079329665126341623L;
    private static final String BASE_DIR = "jboss.home.dir";
    private String defaultHost;
    private WebContainerConfigElement config;

    public WebSubsystemAdd() {
        super(Namespace.CURRENT.getUriString());
    }

    /** {@inheritDoc} */
    protected <P> void applyUpdate(UpdateContext context, UpdateResultHandler<? super Void, P> resultHandler, P param) {
        final String defaultHost = this.defaultHost != null ? this.defaultHost : "localhost";
        final WebServerService service = new WebServerService(defaultHost);
        context.getBatchBuilder().addService(WebSubsystemElement.JBOSS_WEB, service)
            .addDependency(AbstractPathService.pathNameOf(BASE_DIR), String.class, service.getPathInjector())
            .addOptionalDependency(ServiceName.JBOSS.append("mbean", "server"), MBeanServer.class, service.getMbeanServer())
            .addListener(new UpdateResultHandler.ServiceStartListener<P>(resultHandler, param))
            .setInitialMode(Mode.ON_DEMAND);

        WebDeploymentActivator.activate(defaultHost, new SharedWebMetaDataBuilder(config), context.getBatchBuilder());
    }

    /** {@inheritDoc} */
    protected WebSubsystemElement createSubsystemElement() {
        final WebSubsystemElement element = new WebSubsystemElement(defaultHost);
        element.setConfig(config);
        return element;
    }

    public String getDefaultHost() {
        return defaultHost;
    }

    public void setDefaultHost(String defaultHost) {
        this.defaultHost = defaultHost;
    }

    public WebContainerConfigElement getConfig() {
        return config;
    }

    public void setConfig(WebContainerConfigElement config) {
        this.config = config;
    }

}
