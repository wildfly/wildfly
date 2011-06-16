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
import org.jboss.as.controller.NewModelController;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.NewStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationHandler;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.persistence.NullConfigurationPersister;
import org.jboss.as.controller.registry.ImmutableModelNodeRegistration;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Emanuel Muckenhuber
 */
public class WildcardUnitTestCase extends AbstractControllerTestBase {

    private static final PathElement host = PathElement.pathElement("host");
    private static final PathElement server = PathElement.pathElement("server");
    private static final PathElement subsystem = PathElement.pathElement("subsystem");
    private static final PathElement connector = PathElement.pathElement("connector");

    static final DescriptionProvider NULL = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            return new ModelNode();
        }
    };

    @Test
    public void test() throws Exception {
        final NewModelController controller = getController();

        final ModelNode address = new ModelNode();
        address.add("host", "*");
        address.add("server", "[one,two]");
        address.add("subsystem", "web");
        address.add("connector", "*");

        final ModelNode read = new ModelNode();
        read.get(OP).set("read-resource");
        read.get(OP_ADDR).set(address);

        ModelNode result = controller.execute(read, null, null, null);
        result = result.get("result");

        Assert.assertEquals(4, result.asInt()); // A,B one,two

        final ModelNode describe = new ModelNode();
        describe.get(OP).set("describe");
        describe.get(OP_ADDR).set(address);

        result = controller.execute(describe, null, null, null).get("result");

    }

    protected ModelNode createCoreModel() {
        final ModelNode model = new ModelNode();

        model.get("host", "A", "server", "one", "subsystem", "web", "connector", "default", "1").setEmptyObject();
        model.get("host", "A", "server", "two", "subsystem", "web", "connector", "default", "2").setEmptyObject();
        model.get("host", "A", "server", "three", "subsystem", "web", "connector", "other", "3").setEmptyObject();
        model.get("host", "B", "server", "one", "subsystem", "web", "connector", "default", "4").setEmptyObject();
        model.get("host", "B", "server", "two", "subsystem", "web", "connector", "default", "5").setEmptyObject();
        model.get("host", "B", "server", "three", "subsystem", "web", "connector", "default", "6").setEmptyObject();

        return model;
    }

    @Override
    DescriptionProvider getRootDescriptionProvider() {
        return NULL;
    }

    @Override
    void initModel(ModelNodeRegistration root) {
            root.registerOperationHandler("read-resource", GlobalOperationHandlers.READ_RESOURCE, NULL, true);
            root.registerOperationHandler("describe", new DescribeHandler(), NULL, true);

            final ModelNodeRegistration hosts = root.registerSubModel(host, NULL);
            final ModelNodeRegistration servers = hosts.registerSubModel(server, NULL);
            final ModelNodeRegistration subsystems = servers.registerSubModel(subsystem, NULL);
            final ModelNodeRegistration connectors = subsystems.registerSubModel(connector, NULL);
    }

    private static class DescribeHandler implements NewStepHandler {

        /** {@inheritDoc} */
        public void execute(NewOperationContext context, ModelNode operation) {

            final ImmutableModelNodeRegistration registry = context.getModelNodeRegistration();
            final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
            final DescriptionProvider descriptionProvider = registry.getModelDescription(address);
            if(descriptionProvider == null) {
                context.getFailureDescription().set(new ModelNode());
            } else {
                context.getResult().set(descriptionProvider.getModelDescription(null));
            }

            context.completeStep();
        }

    }
}
