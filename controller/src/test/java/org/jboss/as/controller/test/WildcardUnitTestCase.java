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

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
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
        final ModelController controller = getController();

        final ModelNode address = new ModelNode();
        address.add("host", "*");
        address.add("server", "[one,two]");
        address.add("subsystem", "web");
        address.add("connector", "*");

        final ModelNode read = new ModelNode();
        read.get(OP).set("read-resource");
        read.get(OP_ADDR).set(address);

        ModelNode result = controller.execute(read, null, null, null);
        System.out.println(result);
        result = result.get("result");

        Assert.assertEquals(4, result.asInt()); // A,B one,two

        final ModelNode describe = new ModelNode();
        describe.get(OP).set("describe");
        describe.get(OP_ADDR).set(address);

        result = controller.execute(describe, null, null, null).get("result");

    }

    @Override
    protected DescriptionProvider getRootDescriptionProvider() {
        return NULL;
    }

    @Override
    protected void initModel(Resource rootResource, ManagementResourceRegistration root) {
            root.registerOperationHandler("read-resource", GlobalOperationHandlers.READ_RESOURCE, NULL, true);
            root.registerOperationHandler("describe", new DescribeHandler(), NULL, true);

            root.registerOperationHandler("setup", new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

                    final ModelNode model = new ModelNode();

                    model.get("host", "A", "server", "one", "subsystem", "web", "connector", "default", "1").setEmptyObject();
                    model.get("host", "A", "server", "two", "subsystem", "web", "connector", "default", "2").setEmptyObject();
                    model.get("host", "A", "server", "three", "subsystem", "web", "connector", "other", "3").setEmptyObject();
                    model.get("host", "B", "server", "one", "subsystem", "web", "connector", "default", "4").setEmptyObject();
                    model.get("host", "B", "server", "two", "subsystem", "web", "connector", "default", "5").setEmptyObject();
                    model.get("host", "B", "server", "three", "subsystem", "web", "connector", "default", "6").setEmptyObject();

                    createModel(context, model);

                    context.completeStep();
                }
            }, NULL);



            final ManagementResourceRegistration hosts = root.registerSubModel(host, NULL);
            final ManagementResourceRegistration servers = hosts.registerSubModel(server, NULL);
            final ManagementResourceRegistration subsystems = servers.registerSubModel(subsystem, NULL);
            final ManagementResourceRegistration connectors = subsystems.registerSubModel(connector, NULL);
    }

    private static class DescribeHandler implements OperationStepHandler {

        /** {@inheritDoc} */
        public void execute(OperationContext context, ModelNode operation) {

            final ImmutableManagementResourceRegistration registry = context.getResourceRegistration();
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
