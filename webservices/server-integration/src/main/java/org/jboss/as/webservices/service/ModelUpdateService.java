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

package org.jboss.as.webservices.service;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.server.Services.JBOSS_SERVER_CONTROLLER;
import static org.jboss.as.webservices.dmr.Constants.ENDPOINT;
import static org.jboss.as.webservices.dmr.Constants.ENDPOINT_CLASS;
import static org.jboss.as.webservices.dmr.Constants.ENDPOINT_CONTEXT;
import static org.jboss.as.webservices.dmr.Constants.ENDPOINT_NAME;
import static org.jboss.as.webservices.dmr.Constants.ENDPOINT_TYPE;
import static org.jboss.as.webservices.dmr.Constants.ENDPOINT_WSDL;
import static org.jboss.msc.service.ServiceBuilder.DependencyType.OPTIONAL;

import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.server.ServerController;
import org.jboss.as.webservices.dmr.WSExtension;
import org.jboss.as.webservices.util.WSServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.jboss.wsf.spi.deployment.Endpoint;

/**
 * Exposes public {@code #add(Endpoint)} & {@code #remove(Endpoint)} methods so
 * WS subsystem can send udpate events to WS DMR Model.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class ModelUpdateService extends AbstractService<Void> {

    private final InjectedValue<ServerController> serverControllerValue = new InjectedValue<ServerController>();
    private static final ModelUpdateService INSTANCE = new ModelUpdateService();

    private ModelUpdateService() {
        super();
    }

    private InjectedValue<ServerController> getServerControllerInjector() {
        return serverControllerValue;
    }

    public static ModelUpdateService getInstance() {
        return INSTANCE;
    }

    public static ServiceController<?> install(final ServiceTarget serviceTarget, final ServiceListener<Object>... listeners) {
        final Injector<ServerController> controllerInjector = INSTANCE.getServerControllerInjector();
        final ServiceBuilder<Void> builder = serviceTarget.addService(WSServices.MODEL_SERVICE, INSTANCE);
        builder.addDependency(OPTIONAL, JBOSS_SERVER_CONTROLLER, ServerController.class, controllerInjector);
        builder.addListener(listeners);
        builder.setInitialMode(Mode.ACTIVE);
        return builder.install();
    }

    public void add(final Endpoint endpoint) {
        final ServerController controller = serverControllerValue.getOptionalValue();
        if (controller != null) {
            final Operation addOperation = newAddOperation(endpoint);
            controller.execute(addOperation);
        }
    }

    public void remove(final Endpoint endpoint) {
        final ServerController controller = serverControllerValue.getOptionalValue();
        if (controller != null) {
            final Operation removeOperation = newRemoveOperation(endpoint);
            controller.execute(removeOperation);
        }
    }

    private Operation newAddOperation(final Endpoint endpoint) {
        final ModelNode op = new ModelNode();
        op.get(OP).set(ADD);
        final ModelNode address = op.get(OP_ADDR);
        address.add(SUBSYSTEM, WSExtension.SUBSYSTEM_NAME);
        address.add(ENDPOINT, getId(endpoint));
        op.get(ENDPOINT_NAME).set(getName(endpoint));
        op.get(ENDPOINT_CONTEXT).set(getContext(endpoint));
        op.get(ENDPOINT_CLASS).set(endpoint.getTargetBeanName());
        op.get(ENDPOINT_TYPE).set(getType(endpoint));
        op.get(ENDPOINT_WSDL).set(endpoint.getAddress() + "?wsdl");
        return OperationBuilder.Factory.create(op).build();
    }

    private Operation newRemoveOperation(final Endpoint endpoint) {
        final ModelNode op = new ModelNode();
        op.get(OP).set(REMOVE);
        final ModelNode address = op.get(OP_ADDR);
        address.add(SUBSYSTEM, WSExtension.SUBSYSTEM_NAME);
        address.add(ENDPOINT, getId(endpoint));
        return OperationBuilder.Factory.create(op).build();
    }

    private String getType(final Endpoint endpoint) {
        return endpoint.getService().getDeployment().getType().toString();
    }

    private String getName(final Endpoint endpoint) {
        return endpoint.getName().getKeyProperty(Endpoint.SEPID_PROPERTY_ENDPOINT);
    }

    private String getContext(final Endpoint endpoint) {
        return endpoint.getName().getKeyProperty(Endpoint.SEPID_PROPERTY_CONTEXT);
    }

    private String getId(final Endpoint endpoint) {
        final StringBuilder sb = new StringBuilder();
        sb.append(getContext(endpoint));
        sb.append(':');
        sb.append(getName(endpoint));
        return sb.toString();
    }

}
