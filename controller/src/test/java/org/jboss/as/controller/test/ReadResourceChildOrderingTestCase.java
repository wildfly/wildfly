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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;

import java.util.EnumSet;
import java.util.Locale;

import junit.framework.Assert;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.CommonProviders;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.OperationEntry.Flag;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ReadResourceChildOrderingTestCase extends AbstractControllerTestBase {

    private String[] str = new String[] {"g", "e", "l", "d", "h", "h", "k", "f", "a", "b", "j", "g", "c", "i"};

    ModelNode model;
    public ReadResourceChildOrderingTestCase() {
        model = new ModelNode();
        for (int i = 0 ; i < str.length ; i++) {
            model.get("test", str[i], "prop").set(str[i].toUpperCase(Locale.ENGLISH));
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
    protected DescriptionProvider getRootDescriptionProvider() {
        return new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(Locale locale) {
                return new ModelNode();
            }
        };
    }

    @Override
    protected void initModel(Resource rootResource, ManagementResourceRegistration registration) {
        registration.registerOperationHandler(READ_RESOURCE_OPERATION, GlobalOperationHandlers.READ_RESOURCE, CommonProviders.READ_RESOURCE_PROVIDER, true, EnumSet.of(Flag.RUNTIME_ONLY));
        registration.registerOperationHandler("setup", new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    createModel(context, model);
                    context.completeStep();
                }
            }, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return new ModelNode();
            }
        }, false, OperationEntry.EntryType.PRIVATE);
        ManagementResourceRegistration reg = registration.registerSubModel(PathElement.pathElement("test"), new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(Locale locale) {
                ModelNode node = new ModelNode();
                node.get(DESCRIPTION).set("a test node");
                node.get(ATTRIBUTES, "prop", TYPE).set(ModelType.STRING);
                node.get(ATTRIBUTES, "prop", DESCRIPTION).set("A test property");
                return node;
            }
        });
        rootResource.getModel().set(model);
        System.out.println(model);
    }

}
