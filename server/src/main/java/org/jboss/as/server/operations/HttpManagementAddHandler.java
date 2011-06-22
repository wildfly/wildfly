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

package org.jboss.as.server.operations;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SECURE_PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SECURITY_REALM;

import java.security.AccessController;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.ManagementDescription;
import org.jboss.as.domain.management.security.SecurityRealmService;
import org.jboss.as.network.NetworkInterfaceBinding;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.Services;
import org.jboss.as.server.mgmt.HttpManagementService;
import org.jboss.as.server.services.net.NetworkInterfaceService;
import org.jboss.as.server.services.path.AbstractPathService;
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

    public static final HttpManagementAddHandler INSTANCE = new HttpManagementAddHandler();
    public static final String OPERATION_NAME = ModelDescriptionConstants.ADD;

    protected void populateModel(ModelNode operation, ModelNode model) {
        model.get(INTERFACE).set(operation.require(INTERFACE).asString());
        if (operation.hasDefined(PORT)) {
            model.get(ModelDescriptionConstants.PORT).set(operation.require(PORT).asInt());
        }
        if (operation.hasDefined(SECURE_PORT)) {
            model.get(ModelDescriptionConstants.SECURE_PORT).set(operation.require(SECURE_PORT).asInt());
        }
        if (operation.hasDefined(SECURITY_REALM)) {
            model.get(ModelDescriptionConstants.SECURITY_REALM).set(operation.get(SECURITY_REALM).asString());
        }
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) {
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
                .addDependency(Services.JBOSS_SERVER_CONTROLLER, ModelController.class, service.getModelControllerInjector())
                .addDependency(AbstractPathService.pathNameOf(ServerEnvironment.SERVER_TEMP_DIR), String.class, service.getTempDirInjector())
                .addInjection(service.getPortInjector(), port)
                .addInjection(service.getSecurePortInjector(), securePort)
                .addInjection(service.getExecutorServiceInjector(), Executors.newCachedThreadPool(new JBossThreadFactory(new ThreadGroup("HttpManagementService-threads"), Boolean.FALSE, null, "%G - %t", null, null, AccessController.getContext())));

        if (securityRealm != null) {
            builder.addDependency(SecurityRealmService.BASE_SERVICE_NAME.append(securityRealm), SecurityRealmService.class, service.getSecurityRealmInjector());
        }

        newControllers.add(builder.addListener(verificationHandler)
                .setInitialMode(ServiceController.Mode.ACTIVE)
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
        if (source.hasDefined(name)) {
            return source.require(name).asInt();
        }
        return -1;
    }
}
