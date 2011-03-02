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

package org.jboss.as.controller.test;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.Locale;

import junit.framework.TestCase;

import org.jboss.as.controller.BasicModelController;
import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.ModelQueryOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationHandler;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.client.ExecutionContextBuilder;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.persistence.NullConfigurationPersister;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.dmr.ModelNode;

/**
 * @author Emanuel Muckenhuber
 */
public class WildcardUnitTestCase extends TestCase {

    private static final PathElement host = PathElement.pathElement("host");
    private static final PathElement server = PathElement.pathElement("server");
    private static final PathElement subsystem = PathElement.pathElement("subsystem");
    private static final PathElement connector = PathElement.pathElement("connector");

    private ModelController controller;


    public void test() throws Exception {
        final TestController controller = new TestController(createModel());

        final ModelNode address = new ModelNode();
        address.add("host", "*");
        address.add("server", "[one,two]");
        address.add("subsystem", "web");
        address.add("connector", "*");

        final ModelNode read = new ModelNode();
        read.get(OP).set("read-resource");
        read.get(OP_ADDR).set(address);

        System.out.println(controller.execute(ExecutionContextBuilder.Factory.create(read).build()));

        final ModelNode describe = new ModelNode();
        describe.get(OP).set("describe");
        describe.get(OP_ADDR).set(address);

        System.out.println(controller.execute(ExecutionContextBuilder.Factory.create(describe).build()));

    }

    private static ModelNode createModel() {
        final ModelNode model = new ModelNode();

        model.get("host", "A", "server", "one", "subsystem", "web", "connector", "default", "1").setEmptyObject();
        model.get("host", "A", "server", "two", "subsystem", "web", "connector", "default", "2").setEmptyObject();
        model.get("host", "A", "server", "two", "subsystem", "web", "connector", "other", "3").setEmptyObject();
        model.get("host", "B", "server", "one", "subsystem", "web", "connector", "default", "4").setEmptyObject();
        model.get("host", "B", "server", "two", "subsystem", "web", "connector", "default", "5").setEmptyObject();
        model.get("host", "B", "server", "three", "subsystem", "web", "connector", "default", "6").setEmptyObject();

        return model;
    }

    private static class TestController extends BasicModelController {
        static final DescriptionProvider NULL = new DescriptionProvider() {
            public ModelNode getModelDescription(Locale locale) {
                return new ModelNode();
            }
        };
        protected TestController(final ModelNode node) {
            super(node, new NullConfigurationPersister(null), NULL);
            initialize(getRegistry());
        }

        private static void initialize(final ModelNodeRegistration root) {
            root.registerOperationHandler("read-resource", new TestHandler(), NULL, true);
            root.registerOperationHandler("describe", new DescribeHandler(), NULL, true);
            root.registerOperationHandler(GlobalOperationHandlers.ResolveAddressOperationHandler.OPERATION_NAME, GlobalOperationHandlers.RESOLVE, GlobalOperationHandlers.RESOLVE, false);

            final ModelNodeRegistration hosts = root.registerSubModel(host, NULL);
            final ModelNodeRegistration servers = hosts.registerSubModel(server, NULL);
            final ModelNodeRegistration subsystems = servers.registerSubModel(subsystem, NULL);
            final ModelNodeRegistration connectors = subsystems.registerSubModel(connector, NULL);

        }

        /** {@inheritDoc} */
        protected ModelNodeRegistration getRegistry() {
            return super.getRegistry();
        }
    }

    private static class TestHandler implements ModelQueryOperationHandler {
        /** {@inheritDoc} */
        @Override
        public OperationResult execute(OperationContext context, ModelNode operation, ResultHandler resultHandler) {

            resultHandler.handleResultFragment(new String[0], context.getSubModel());
            resultHandler.handleResultComplete();

            return new BasicOperationResult();
        }
    }

    private static class DescribeHandler implements OperationHandler {

        /** {@inheritDoc} */
        public OperationResult execute(OperationContext context, ModelNode operation, ResultHandler resultHandler) throws OperationFailedException {

            final ModelNodeRegistration registry = context.getRegistry();
            final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
            final DescriptionProvider descriptionProvider = registry.getModelDescription(address);
            if(descriptionProvider == null) {
                resultHandler.handleFailed(new ModelNode());
            } else {
                resultHandler.handleResultFragment(new String[0], descriptionProvider.getModelDescription(null));
                resultHandler.handleResultComplete();
            }

            return new BasicOperationResult();
        }

    }
}