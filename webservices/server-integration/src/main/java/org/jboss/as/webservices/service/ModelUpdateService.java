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

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.webservices.dmr.WSExtension;
import org.jboss.as.webservices.util.WSServices;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.wsf.spi.deployment.Endpoint;

/**
 * Exposes public {@code #add(Endpoint)} & {@code #remove(Endpoint)} methods so
 * WS subsystem can send udpate events to WS DMR Model.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class ModelUpdateService extends AbstractService<Void> {

    private static final Logger log = Logger.getLogger(ModelUpdateService.class);

    private final InjectedValue<ModelController> controllerValue = new InjectedValue<ModelController>();
    private static final ModelUpdateService INSTANCE = new ModelUpdateService();
    private volatile ModelControllerClient client;

    private ModelUpdateService() {
        super();
    }

    private InjectedValue<ModelController> getServerControllerInjector() {
        return controllerValue;
    }

    public static ModelUpdateService getInstance() {
        return INSTANCE;
    }

    @Override
    public void start(StartContext context) throws StartException {
        super.start(context);
        // client = controllerValue.getValue().createClient(Executors.newCachedThreadPool());

    }

    @Override
    public void stop(StopContext context) {
        // StreamUtils.safeClose(client);
        client = null;
        super.stop(context);
    }

    public static ServiceController<?> install(final ServiceTarget serviceTarget, final ServiceListener<Object>... listeners) {
        final Injector<ModelController> controllerInjector = INSTANCE.getServerControllerInjector();
        final ServiceBuilder<Void> builder = serviceTarget.addService(WSServices.MODEL_SERVICE, INSTANCE);
        builder.addDependency(OPTIONAL, JBOSS_SERVER_CONTROLLER, ModelController.class, controllerInjector);
        builder.addListener(listeners);
        builder.setInitialMode(Mode.ACTIVE);
        return builder.install();
    }

    public void add(final Endpoint endpoint) {
        final ModelController controller = controllerValue.getOptionalValue();
        if (controller != null) {
            // TODO AS7-855
            log.warn("Registering webservice endpoints in the management model is temporarily disabled (AS7-855)");
//            final ModelNode addOperation = newAddOperation(endpoint);
//            try {
//                client.execute(addOperation);
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
        }
    }

    public void remove(final Endpoint endpoint) {
        final ModelController controller = controllerValue.getOptionalValue();
        if (controller != null) {
            // TODO AS7-855
            log.warn("Registering webservice endpoints in the management model is temporarily disabled (AS7-855)");
//            final ModelNode removeOperation = newRemoveOperation(endpoint);
//            try {
//                client.execute(removeOperation);
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
        }
    }

    private ModelNode newAddOperation(final Endpoint endpoint) {
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
        return op;
    }

    private ModelNode newRemoveOperation(final Endpoint endpoint) {
        final ModelNode op = new ModelNode();
        op.get(OP).set(REMOVE);
        final ModelNode address = op.get(OP_ADDR);
        address.add(SUBSYSTEM, WSExtension.SUBSYSTEM_NAME);
        address.add(ENDPOINT, getId(endpoint));
        return op;
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
