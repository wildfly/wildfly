/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.NonResolvingResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.global.ReadAttributeHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test to verify if https://issues.jboss.org/browse/AS7-1960 is an issue
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class DefaultAttributeTestCase extends AbstractControllerTestBase {

    @Test
    public void testCannotAccessAttributeWhenResourceDoesNotExist() throws Exception {
        //Just make sure it works as expected for an existant resource
        ModelNode op = Util.createAddOperation(PathAddress.pathAddress(new ModelNode().add("test", "exists")));
        executeForResult(op);

        op = createOperation(ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION, "test", "exists");
        op.get(ModelDescriptionConstants.NAME).set("attr");
        ModelNode result = executeForResult(op);
        Assert.assertEquals("default", result.asString());

        //This should fail
        op = createOperation(ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION, "test", "notthere");
        op.get(ModelDescriptionConstants.NAME).set("attr");
        executeForFailure(op);
    }


    @Override
    protected void initModel(Resource rootResource, ManagementResourceRegistration registration) {
        registration.registerOperationHandler(ReadAttributeHandler.DEFINITION, ReadAttributeHandler.INSTANCE, true);
        registration.registerSubModel(new TestResource());
    }


    private static AttributeDefinition ATTRIBUTE_WITH_DEFAULT = new SimpleAttributeDefinitionBuilder("attr", ModelType.STRING, true)
        .setDefaultValue(new ModelNode("default"))
        .build();

    private static class TestResource extends SimpleResourceDefinition {
        public TestResource() {
            super(PathElement.pathElement("test"), new NonResolvingResourceDescriptionResolver(), new TestResourceAddHandler(), new AbstractRemoveStepHandler() {});
        }

        @Override
        public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
            resourceRegistration.registerReadOnlyAttribute(ATTRIBUTE_WITH_DEFAULT, null);
        }
    }

    private static class TestResourceAddHandler extends AbstractAddStepHandler {

        @Override
        protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
            ATTRIBUTE_WITH_DEFAULT.validateAndSet(operation, model);
        }
    }
}
