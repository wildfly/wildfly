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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.util.Locale;

import junit.framework.Assert;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.CommonProviders;
import org.jboss.as.controller.operations.common.ProcessEnvironment;
import org.jboss.as.controller.operations.common.SystemPropertyAddHandler;
import org.jboss.as.controller.operations.common.SystemPropertyRemoveHandler;
import org.jboss.as.controller.operations.common.SystemPropertyValueWriteAttributeHandler;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class SystemPropertyReferencesTestCase extends AbstractControllerTestBase {

    @Override
    protected DescriptionProvider getRootDescriptionProvider() {
        return new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(Locale locale) {
                return new ModelNode();
            }
        };
    }

    @Before
    public void clearProperties() {
        System.clearProperty("test.one");
        System.clearProperty("test.two");
        System.clearProperty("test.referencing");


    }

    @Test
    public void testSystemPropertyReferences() {
        Assert.assertNull(System.getProperty("test.one"));
        Assert.assertNull(System.getProperty("test.two"));
        Assert.assertNull(System.getProperty("test.referencing"));

        ModelNode op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(OP_ADDR).add("system-property", "test.one");
        op.get(VALUE).set("ONE");
        ModelNode result = getController().execute(op, OperationMessageHandler.DISCARD, ModelController.OperationTransactionControl.COMMIT, null);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());

        op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(OP_ADDR).add("system-property", "test.two");
        op.get(VALUE).set("TWO");
        result = getController().execute(op, OperationMessageHandler.DISCARD, ModelController.OperationTransactionControl.COMMIT, null);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());

        op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(OP_ADDR).add("system-property", "test.referencing");
        op.get(VALUE).setExpression("${test.one} ${test.two}");
        result = getController().execute(op, OperationMessageHandler.DISCARD, ModelController.OperationTransactionControl.COMMIT, null);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());

        Assert.assertEquals("ONE", System.getProperty("test.one"));
        Assert.assertEquals("TWO", System.getProperty("test.two"));
        Assert.assertEquals("ONE TWO", System.getProperty("test.referencing"));

        op = new ModelNode();
        op.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        op.get(OP_ADDR).add("system-property", "test.referencing");
        op.get(NAME).set(VALUE);
        op.get(VALUE).setExpression("${test.one}---${test.two}");
        result = getController().execute(op, OperationMessageHandler.DISCARD, ModelController.OperationTransactionControl.COMMIT, null);
        Assert.assertEquals(result.toString(), SUCCESS, result.get(OUTCOME).asString());

        Assert.assertEquals("ONE---TWO", System.getProperty("test.referencing"));
    }

    @Override
    protected void initModel(Resource rootResource, ManagementResourceRegistration registration) {
        registration.registerOperationHandler(WRITE_ATTRIBUTE_OPERATION, GlobalOperationHandlers.WRITE_ATTRIBUTE, CommonProviders.WRITE_ATTRIBUTE_PROVIDER, true);

        ManagementResourceRegistration sysProps = registration.registerSubModel(PathElement.pathElement(SYSTEM_PROPERTY), new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(Locale locale) {
                return new ModelNode();
            }
        });

        ProcessEnvironment environment = new ProcessEnvironment() {

            @Override
            protected void systemPropertyUpdated(String propertyName, String propertyValue) {
            }

            @Override
            protected void setProcessName(String processName) {
            }

            @Override
            protected boolean isRuntimeSystemPropertyUpdateAllowed(String propertyName, String propertyValue, boolean bootTime)
                    throws OperationFailedException {
                return true;
            }

            @Override
            protected String getProcessName() {
                return "xxx";
            }
        };
        sysProps.registerOperationHandler(SystemPropertyAddHandler.OPERATION_NAME, new SystemPropertyAddHandler(environment, false), SystemPropertyAddHandler.INSTANCE_WITHOUT_BOOTTIME, false);
        sysProps.registerOperationHandler(SystemPropertyRemoveHandler.OPERATION_NAME, SystemPropertyRemoveHandler.INSTANCE, SystemPropertyRemoveHandler.INSTANCE, false);
        sysProps.registerReadWriteAttribute(VALUE, null, new SystemPropertyValueWriteAttributeHandler(environment), AttributeAccess.Storage.CONFIGURATION);

    }

}
