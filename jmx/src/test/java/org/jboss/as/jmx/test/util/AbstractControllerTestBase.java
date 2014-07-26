/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jmx.test.util;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.ResourceBuilder;
import org.jboss.as.controller.access.management.DelegatingConfigurableAuthorizer;
import org.jboss.as.controller.audit.AuditLogger;
import org.jboss.as.controller.audit.ManagedAuditLogger;
import org.jboss.as.controller.descriptions.NonResolvingResourceDescriptionResolver;
import org.jboss.as.controller.persistence.AbstractConfigurationPersister;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.persistence.ModelMarshallingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.staxmapper.XMLElementWriter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

/**
 * @author Emanuel Muckenhuber
 */
public abstract class AbstractControllerTestBase {


    protected abstract void initModel(final Resource rootResource, final ManagementResourceRegistration registration);

    private ServiceContainer container;
    private ModelController controller;
    protected final ProcessType processType;

    protected AbstractControllerTestBase(ProcessType processType) {
        this.processType = processType;
    }

    protected AbstractControllerTestBase() {
        this(ProcessType.EMBEDDED_SERVER);
    }

    public ModelController getController() {
        return controller;
    }

    public ServiceContainer getContainer() {
        return container;
    }

    protected ModelNode createOperation(String operationName, String... address) {
        ModelNode operation = new ModelNode();
        operation.get(OP).set(operationName);
        if (address.length > 0) {
            for (String addr : address) {
                operation.get(OP_ADDR).add(addr);
            }
        } else {
            operation.get(OP_ADDR).setEmptyList();
        }

        return operation;
    }

    public ModelNode executeForResult(ModelNode operation) throws OperationFailedException {
        ModelNode rsp = getController().execute(operation, null, null, null);
        if (FAILED.equals(rsp.get(OUTCOME).asString())) {
            throw new OperationFailedException(rsp.get(FAILURE_DESCRIPTION));
        }
        return rsp.get(RESULT);
    }

    public void executeForFailure(ModelNode operation) throws OperationFailedException {
        try {
            executeForResult(operation);
            Assert.fail("Should have given error");
        } catch (OperationFailedException expected) {
            // ignore
        }
    }

    @Before
    public void setupController() throws InterruptedException {
        container = ServiceContainer.Factory.create("test");
        ServiceTarget target = container.subTarget();
        ModelControllerService svc = new ModelControllerService(processType, getAuditLogger(), getAuthorizer());
        ServiceBuilder<ModelController> builder = target.addService(Services.JBOSS_SERVER_CONTROLLER, svc);
        builder.install();
        svc.awaitStartup(30, TimeUnit.SECONDS);
        controller = svc.getValue();
        //ModelNode setup = Util.getEmptyOperation("setup", new ModelNode());
        //controller.execute(setup, null, null, null);
    }

    @After
    public void shutdownServiceContainer() {
        if (container != null) {
            container.shutdown();
            try {
                container.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                container = null;
            }
        }
    }

    protected void addBootOperations(List<ModelNode> bootOperations) {

    }

    protected ManagedAuditLogger getAuditLogger(){
        return AuditLogger.NO_OP_LOGGER;
    }

    protected DelegatingConfigurableAuthorizer getAuthorizer() {
        return null;
    }

    protected class ModelControllerService extends TestModelControllerService {

        ModelControllerService(final ProcessType processType, final ManagedAuditLogger auditLogger, final DelegatingConfigurableAuthorizer authorizer) {
            super(processType, new EmptyConfigurationPersister(), new ControlledProcessState(true),
                    ResourceBuilder.Factory.create(PathElement.pathElement("root"), new NonResolvingResourceDescriptionResolver()).build(),
                    auditLogger, authorizer);
        }

        @Override
        protected boolean boot(List<ModelNode> bootOperations, boolean rollbackOnRuntimeFailure)
                throws ConfigurationPersistenceException {
            addBootOperations(bootOperations);
            return super.boot(bootOperations, rollbackOnRuntimeFailure);
        }

        protected void initModel(Resource rootResource, ManagementResourceRegistration rootRegistration, Resource modelControllerResource) {
            try {
                AbstractControllerTestBase.this.initModel(rootResource, rootRegistration);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static class EmptyConfigurationPersister extends AbstractConfigurationPersister {

        public EmptyConfigurationPersister() {
            super(null);
        }

        public EmptyConfigurationPersister(XMLElementWriter<ModelMarshallingContext> rootDeparser) {
            super(rootDeparser);
        }

        @Override
        public PersistenceResource store(final ModelNode model, Set<PathAddress> affectedAddresses) {
            return NullPersistenceResource.INSTANCE;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<ModelNode> load() {
            return new ArrayList<ModelNode>();
        }

        private static class NullPersistenceResource implements PersistenceResource {

            private static final NullPersistenceResource INSTANCE = new NullPersistenceResource();

            @Override
            public void commit() {
            }

            @Override
            public void rollback() {
            }
        }
    }

    static void createModel(final OperationContext context, final ModelNode node) {
        createModel(context, PathAddress.EMPTY_ADDRESS, node);
    }

    static void createModel(final OperationContext context, final PathAddress base, final ModelNode node) {
        if (!node.isDefined()) {
            return;
        }
        final ManagementResourceRegistration registration = context.getResourceRegistrationForUpdate();
        final Set<String> children = registration.getChildNames(base);
        final ModelNode current = new ModelNode();
        final Resource resource = base.size() == 0 ? context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS) : context.createResource(base);
        if (node.getType() == ModelType.OBJECT) {
            for (final String key : node.keys()) {
                if (!children.contains(key)) {
                    current.get(key).set(node.get(key));
                }
            }
            resource.getModel().set(current);
        } else {
            resource.getModel().set(node);
            return;
        }
        if (children != null && !children.isEmpty()) {
            for (final String childType : children) {
                if (node.hasDefined(childType)) {
                    for (final String key : node.get(childType).keys()) {
                        createModel(context, base.append(PathElement.pathElement(childType, key)), node.get(childType, key));
                    }
                }
            }
        }
    }

}
