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

package org.jboss.as.remoting;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

import java.io.OutputStream;
import java.util.List;
import java.util.Locale;

import org.jboss.as.controller.BasicModelController;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ExecutionContextBuilder;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.persistence.ConfigurationPersister;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.dmr.ModelNode;

/**
 * @author Emanuel Muckenhuber
 */
public class RemotingSubsystemUnitTestCase {

    static final DescriptionProvider NULL_PROVIDER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(final Locale locale) {
            return new ModelNode();
        }
    };

    static final ModelNode model = new ModelNode();
    static final ModelNode subsystemRoot = new ModelNode();
    static final ModelNode connectorRoot = new ModelNode();
    static final TestController c = new TestController();
    static {
        subsystemRoot.add("profile").add("web").add("subsystem").add("remoting").protect();
        connectorRoot.set(subsystemRoot).add("connector").protect();
        model.get("profile", "web", "subsystem"); // initialize the model structure
    }

    public static void main(final String[] args) throws Exception {

        ModelNodeRegistration reg = c.getRegistry().registerSubModel(PathElement.pathElement("profile", "web"), NULL_PROVIDER);
        reg = reg.registerSubModel(PathElement.pathElement("subsystem", "remoting"), RemotingSubsystemProviders.SUBSYSTEM);
        reg.registerOperationHandler(ADD, new RemotingSubsystemAdd(), RemotingSubsystemProviders.SUBSYSTEM_ADD, false);
        reg = reg.registerSubModel(PathElement.pathElement("connector"), NULL_PROVIDER);
        reg.registerOperationHandler(ADD, new ConnectorAdd(), RemotingSubsystemProviders.CONNECTOR_ADD, false);
        reg.registerOperationHandler(REMOVE, new ConnectorRemove(), RemotingSubsystemProviders.CONNECTOR_REMOVE, false);

        System.out.println(model);

        // Create the subsystem
        {
            final ModelNode operation = new ModelNode();
            operation.get(OP_ADDR).set(subsystemRoot.clone());
            operation.get(OP).set(ADD);
            operation.get("thread-pool").set("remoting-thread-pool");
            final ModelNode response = c.execute(operation);
        }
        System.out.println(model);
        // One
        {
            final ModelNode operation = new ModelNode();
            operation.get(OP_ADDR).set(connectorRoot.clone().add("one"));
            operation.get(OP).set(ADD);
            operation.get("socket-binding").set("sb-one");
            operation.get("sasl");

            final ModelNode response = c.execute(operation);
            System.out.println(model);
        }
        // Two
        {
            final ModelNode operation = new ModelNode();
            operation.get(OP_ADDR).set(connectorRoot.clone().add("two"));
            operation.get(OP).set(ADD);
            operation.get("socket-binding").set("sb-two");

            final ModelNode response = c.execute(operation);
        }
        // Three
        {
            final ModelNode operation = new ModelNode();
            operation.get(OP_ADDR).set(connectorRoot.clone().add("three"));
            operation.get(OP).set(ADD);
            operation.get("socket-binding").set("sb-three");
            operation.get("authentication-provider").set("test");

            final ModelNode response = c.execute(operation);
        }

        System.out.println(model);
        System.out.println("----");

        // Remove two
        {
            final ModelNode operation = new ModelNode();
            operation.get(OP_ADDR).set(connectorRoot.clone().add("two"));
            operation.get(OP).set(REMOVE);

            final ModelNode response = c.execute(operation);
        }

        System.out.println(model);
        System.out.println("----");

        // Add two
        {
            final ModelNode operation = new ModelNode();
            operation.get(OP_ADDR).set(connectorRoot.clone().add("two"));
            operation.get(OP).set(ADD);
            operation.get("socket-binding").set("sb-two");
            operation.get("sasl");

            final ModelNode response = c.execute(operation);
        }

    }

    static class TestController extends BasicModelController {

        protected TestController() {
            super(model, new ConfigurationPersister() {
                @Override
                public void store(ModelNode model) throws ConfigurationPersistenceException {
                }
                @Override
                public void marshallAsXml(ModelNode model, OutputStream output) throws ConfigurationPersistenceException {
                }
                @Override
                public List<ModelNode> load() throws ConfigurationPersistenceException {
                    return null;
                }
            }, (DescriptionProvider) null);
        }

        /** {@inheritDoc} */
        @Override
        protected ModelNodeRegistration getRegistry() {
            return super.getRegistry();
        }

        public ModelNode execute(ModelNode operation) {
            return super.execute(ExecutionContextBuilder.Factory.create(operation).build());
        }
    }

}
