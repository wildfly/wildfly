/*
 * JBoss, Home of Professional Open Source
 * Copyright 2018, Red Hat Inc., and individual contributors as indicated
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
package org.wildfly.extension.jaxrs;

import io.undertow.servlet.api.ServletContainer;
import io.undertow.servlet.core.ManagedServlet;
import io.undertow.servlet.handlers.ServletHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.wildfly.extension.undertow.Server;
import org.wildfly.extension.undertow.UndertowExtension;
import org.wildfly.extension.undertow.UndertowService;

import javax.servlet.Servlet;
import java.util.Map;

public class ResetStatisticsOperation implements OperationStepHandler {
    public static final String RESET_STATISTICS = "reset-statistics";

    public static final OperationDefinition DEFINITION =
            new SimpleOperationDefinitionBuilder(RESET_STATISTICS,
                    UndertowExtension.getResolver())
            .setRuntimeOnly()
            .build();

    public static final ResetStatisticsOperation INSTANCE = new ResetStatisticsOperation();

    private ResetStatisticsOperation() {
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        ServiceController<?> controller = context.getServiceRegistry(false)
                .getService(UndertowService.UNDERTOW);

        if (controller != null) {
            UndertowService service = (UndertowService) controller.getService();

            if (service != null) {

                for (Server server : service.getServers()) {
                    ServletContainer servletContainer = server.getServletContainer()
                            .getValue().getValue().getServletContainer();

                    for (String name : servletContainer.listDeployments()) {
                        for (Map.Entry<String, ServletHandler> entry : servletContainer
                                .getDeployment(name).getDeployment().getServlets().getServletHandlers().entrySet()) {

                            ManagedServlet managedServlet = entry.getValue().getManagedServlet();

                            if (HttpServletDispatcher.class.isAssignableFrom(
                                    managedServlet.getServletInfo().getServletClass())) {

                                try {
                                    Servlet resteasyServlet = managedServlet.getServlet().getInstance();
                                    ((HttpServletDispatcher) resteasyServlet).getDispatcher()
                                            .getProviderFactory().getStatisticsController().reset();
                                } catch (Exception e) {
                                    // no-op
                                }
                            }
                        }
                    }
                }
            }
        }
        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }
}
