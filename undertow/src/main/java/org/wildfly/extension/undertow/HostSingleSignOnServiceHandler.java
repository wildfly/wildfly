/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow;

import static org.wildfly.extension.undertow.SingleSignOnDefinition.Attribute.COOKIE_NAME;
import static org.wildfly.extension.undertow.SingleSignOnDefinition.Attribute.DOMAIN;
import static org.wildfly.extension.undertow.SingleSignOnDefinition.Attribute.HTTP_ONLY;
import static org.wildfly.extension.undertow.SingleSignOnDefinition.Attribute.PATH;
import static org.wildfly.extension.undertow.SingleSignOnDefinition.Attribute.SECURE;

import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleCapabilityServiceConfigurator;
import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.controller.CapabilityServiceTarget;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.undertow.security.sso.DistributableHostSingleSignOnManagerServiceConfiguratorProvider;

import io.undertow.security.impl.InMemorySingleSignOnManager;
import io.undertow.security.impl.SingleSignOnManager;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2014 Red Hat Inc.
 * @author Paul Ferraro
 */
class HostSingleSignOnServiceHandler implements ResourceServiceHandler {

    @Override
    public void installServices(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        PathAddress hostAddress = address.getParent();
        PathAddress serverAddress = hostAddress.getParent();
        String hostName = hostAddress.getLastElement().getValue();
        String serverName = serverAddress.getLastElement().getValue();

        String domain = ModelNodes.optionalString(DOMAIN.resolveModelAttribute(context, model)).orElse(null);
        String path = PATH.resolveModelAttribute(context, model).asString();
        boolean secure = SECURE.resolveModelAttribute(context, model).asBoolean();
        boolean httpOnly = HTTP_ONLY.resolveModelAttribute(context, model).asBoolean();
        String cookieName = COOKIE_NAME.resolveModelAttribute(context, model).asString();

        ServiceName serviceName = UndertowService.ssoServiceName(serverName, hostName);
        ServiceName virtualHostServiceName = HostDefinition.HOST_CAPABILITY.getCapabilityServiceName(serverName,hostName);

        CapabilityServiceTarget target = context.getCapabilityServiceTarget();

        ServiceName managerServiceName = serviceName.append("manager");
        DistributableHostSingleSignOnManagerServiceConfiguratorProvider.INSTANCE.map(provider -> provider.getServiceConfigurator(managerServiceName, serverName, hostName))
                .orElse(new SimpleCapabilityServiceConfigurator<>(managerServiceName, new InMemorySingleSignOnManager()))
                .configure(context).build(target).setInitialMode(ServiceController.Mode.ON_DEMAND).install();

        SingleSignOnService service = new SingleSignOnService(domain, path, httpOnly, secure, cookieName);
        target.addCapability(HostSingleSignOnDefinition.HOST_SSO_CAPABILITY, service)
                .addAliases(serviceName)
                .addDependency(virtualHostServiceName, Host.class, service.getHost())
                .addDependency(managerServiceName, SingleSignOnManager.class, service.getSingleSignOnSessionManager())
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();
    }

    @Override
    public void removeServices(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        PathAddress hostAddress = address.getParent();
        PathAddress serverAddress = hostAddress.getParent();
        String hostName = hostAddress.getLastElement().getValue();
        String serverName = serverAddress.getLastElement().getValue();

        ServiceName serviceName = UndertowService.ssoServiceName(serverName, hostName);
        context.removeService(serviceName.append("manager"));
        context.removeService(HostSingleSignOnDefinition.HOST_SSO_CAPABILITY.getCapabilityServiceName(address));
    }

}
