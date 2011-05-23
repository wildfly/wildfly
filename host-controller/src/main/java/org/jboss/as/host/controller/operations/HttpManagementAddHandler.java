/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.host.controller.operations;

import java.security.AccessController;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SECURITY_REALM;
import org.jboss.as.controller.descriptions.common.ManagementDescription;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.domain.management.security.SecurityRealmService;
import org.jboss.as.host.controller.HostControllerEnvironment;
import org.jboss.as.server.mgmt.HttpManagementService;
import org.jboss.as.server.services.net.NetworkInterfaceBinding;
import org.jboss.as.server.services.net.NetworkInterfaceService;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.threads.JBossThreadFactory;

/**
 * A handler that activates the HTTP management API.
 *
 * @author Jason T. Greene
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class HttpManagementAddHandler extends AbstractAddStepHandler implements DescriptionProvider {

    public static final String OPERATION_NAME = ModelDescriptionConstants.ADD;

    private final HostControllerEnvironment environment;

    private HttpManagementAddHandler(final HostControllerEnvironment environment) {
        this.environment = environment;
    }

    public static HttpManagementAddHandler getInstance(HostControllerEnvironment environment) {
        return new HttpManagementAddHandler(environment);
    }

    protected void populateModel(ModelNode operation, ModelNode model) {
        final String interfaceName = operation.require(ModelDescriptionConstants.INTERFACE).asString();
        final int port = getIntValue(operation, ModelDescriptionConstants.PORT);
        final int securePort = getIntValue(operation, ModelDescriptionConstants.SECURE_PORT);
        final String securityRealm = operation.hasDefined(SECURITY_REALM) ? operation.get(SECURITY_REALM).asString() : null;

        model.get(ModelDescriptionConstants.INTERFACE).set(interfaceName);
        if (port > -1) {
            model.get(ModelDescriptionConstants.PORT).set(port);
        }
        if (securePort > -1) {
            model.get(ModelDescriptionConstants.SECURE_PORT).set(securePort);
        }
        if (securityRealm != null) {
            model.get(ModelDescriptionConstants.SECURITY_REALM).set(securityRealm);
        }
    }

    protected void performRuntime(NewOperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) {
        final String interfaceName = operation.require(ModelDescriptionConstants.INTERFACE).asString();
        final int port = getIntValue(operation, ModelDescriptionConstants.PORT);
        final int securePort = getIntValue(operation, ModelDescriptionConstants.SECURE_PORT);
        final String securityRealm = operation.hasDefined(SECURITY_REALM) ? operation.get(SECURITY_REALM).asString() : null;

        final ServiceTarget serviceTarget = context.getServiceTarget();

        Logger.getLogger("org.jboss.as").infof("creating http management service using network interface (%s) port (%s) securePort (%s)", interfaceName, port, securePort);

        final HttpManagementService service = new HttpManagementService();
        ServiceBuilder builder = serviceTarget.addService(HttpManagementService.SERVICE_NAME, service)
                .addDependency(
                        NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(interfaceName),
                        NetworkInterfaceBinding.class, service.getInterfaceInjector())
                .addDependency(DomainController.SERVICE_NAME, ModelController.class, service.getModelControllerInjector())
                .addInjection(service.getTempDirInjector(), environment.getDomainTempDir().getAbsolutePath())
                .addInjection(service.getPortInjector(), port)
                .addInjection(service.getSecurePortInjector(), securePort)
                .addInjection(service.getExecutorServiceInjector(), Executors.newCachedThreadPool(new JBossThreadFactory(new ThreadGroup("HttpManagementService-threads"), Boolean.FALSE, null, "%G - %t", null, null, AccessController.getContext())));

        if (securityRealm != null) {
            builder.addDependency(SecurityRealmService.BASE_SERVICE_NAME.append(securityRealm), SecurityRealmService.class, service.getSecurityRealmInjector());
        }
        builder.addListener(verificationHandler);
        newControllers.add(builder.setInitialMode(ServiceController.Mode.ACTIVE)
                .install());

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ModelNode getModelDescription(Locale locale) {
        return ManagementDescription.getAddHttpManagementDescription(locale);
    }

    private int getIntValue(ModelNode source, String name) {
        if (source.has(name)) {
            return source.require(name).asInt();
        }
        return -1;
    }
}
