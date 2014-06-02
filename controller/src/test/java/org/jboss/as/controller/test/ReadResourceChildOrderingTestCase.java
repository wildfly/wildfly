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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;

import java.util.Locale;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.NonResolvingResourceDescriptionResolver;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ReadResourceChildOrderingTestCase extends AbstractControllerTestBase {

    private static final String[] STR = new String[]{"g", "e", "l", "d", "h", "h", "k", "f", "a", "b", "j", "g", "c", "i"};

    ModelNode model;

    public ReadResourceChildOrderingTestCase() {
        model = new ModelNode();
        for (String aSTR : STR) {
            model.get("test", aSTR, "prop").set(aSTR.toUpperCase(Locale.ENGLISH));
        }
    }


    @Test
    public void testOrdering() throws Exception {
        ModelNode op = createOperation(READ_RESOURCE_OPERATION);
        op.get(RECURSIVE).set(true);

        ModelNode result = executeForResult(op);
        Assert.assertEquals(model, result);
    }

    @Override
    protected void initModel(Resource rootResource, ManagementResourceRegistration registration) {
        GlobalOperationHandlers.registerGlobalOperations(registration, processType);
        registration.registerOperationHandler(new SimpleOperationDefinitionBuilder("setup", new NonResolvingResourceDescriptionResolver())
                .setPrivateEntry()
                .build()
                , new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                createModel(context, model);
                context.stepCompleted();
            }
        });

        registration.registerSubModel(new SimpleResourceDefinition(PathElement.pathElement("test"), new NonResolvingResourceDescriptionResolver()));

        rootResource.getModel().set(model);
    }

}
