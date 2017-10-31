/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.naming;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.BINDING;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.BINDING_TYPE;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.LOOKUP;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.OBJECT_FACTORY;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.REBIND;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.SIMPLE;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.TYPE;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.VALUE;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

import java.io.FilePermission;
import java.net.URL;

import javax.ejb.EJB;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.naming.NamingContext;
import org.jboss.as.naming.subsystem.NamingExtension;
import org.jboss.dmr.ModelNode;
import org.jboss.remoting3.security.RemotingPermission;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test case for changing JBDI bound values in the naming subsystem without forcing a reload/restart (see WFLY-3239).
 * Uses AS controller to do the bind/rebind, lookup is through an EJB.
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class RebindTestCase {

    @ArquillianResource
    private ManagementClient managementClient;

    @EJB(mappedName = "java:global/RebindTestCase/BindingLookupBean")
    private BindingLookupBean bean;

    @Deployment
    public static Archive<?> deploy() {
        String tmpdir = System.getProperty("jboss.home");
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "RebindTestCase.jar");
        jar.addClasses(RebindTestCase.class, BindingLookupBean.class);
        jar.addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller, " +
                "org.jboss.remoting3\n"
        ), "MANIFEST.MF");

        jar.addAsManifestResource(createPermissionsXmlAsset(
                new RemotingPermission("connect"),
                new RemotingPermission("createEndpoint"),
                new FilePermission(System.getProperty("jboss.inst") + "/standalone/tmp/auth/*", "read")
        ), "permissions.xml");

        return jar;
    }

    @Test
    public void testRebinding() throws Exception {

        final String name = "java:global/rebind";
        final String lookup = "java:global/lookup";
        Exception error = null;
        try {
            ModelNode operation = prepareAddBindingOperation(name, SIMPLE);
            operation.get(VALUE).set("http://localhost");
            operation.get(TYPE).set(URL.class.getName());
            ModelNode operationResult = managementClient.getControllerClient().execute(operation);
            verifyBinding(operationResult, name, "http://localhost");

            operation = prepareRebindOperation(name, SIMPLE);
            operation.get(VALUE).set("http://localhost2");
            operation.get(TYPE).set(URL.class.getName());
            operationResult = managementClient.getControllerClient().execute(operation);
            verifyBinding(operationResult, name, "http://localhost2");

            operation = prepareRebindOperation(name, SIMPLE);
            operation.get(VALUE).set("2");
            operation.get(TYPE).set(Integer.class.getName());
            operationResult = managementClient.getControllerClient().execute(operation);
            verifyBinding(operationResult, name, "2");

            operation = prepareAddBindingOperation(lookup, SIMPLE);
            operation.get(VALUE).set("looked up");
            operation.get(TYPE).set(String.class.getName());
            operationResult = managementClient.getControllerClient().execute(operation);
            verifyBinding(operationResult, lookup, "looked up");

            operation = prepareRebindOperation(name, LOOKUP);
            operation.get(LOOKUP).set(lookup);
            operationResult = managementClient.getControllerClient().execute(operation);
            verifyBinding(operationResult, name, "looked up");

            operation = prepareReadResourceOperation(name);
            operationResult = managementClient.getControllerClient().execute(operation);
            Assert.assertFalse(operationResult.get(FAILURE_DESCRIPTION).toString(), operationResult.get(FAILURE_DESCRIPTION).isDefined());
            Assert.assertEquals("java:global/lookup", operationResult.get(RESULT).get(LOOKUP).asString());
        } catch (Exception e) {
            error = e;
            throw e;
        } finally {
            removeBinding(name, error);
            removeBinding(lookup, error);
        }
    }

    @Test
    public void testRebindingObjectFactory() throws Exception {

        final String bindingName = "java:global/bind";
        Exception error = null;

        try {
            ModelNode operation = prepareAddBindingOperation(bindingName, SIMPLE);
            operation.get(VALUE).set("2");
            operation.get(TYPE).set(Integer.class.getName());
            ModelNode operationResult = managementClient.getControllerClient().execute(operation);
            verifyBindingClass(operationResult, bindingName, Integer.class.getName());

            operation = prepareRebindOperation(bindingName, OBJECT_FACTORY);
            operation.get("module").set("org.jboss.as.naming");
            operation.get("class").set("org.jboss.as.naming.interfaces.java.javaURLContextFactory");
            operationResult = managementClient.getControllerClient().execute(operation);
            verifyBindingClass(operationResult, bindingName, NamingContext.class.getName());
        } catch (Exception e) {
            error = e;
            throw e;
        } finally {
            removeBinding(bindingName, error);
        }
    }

    @Test
    public void testRebindingLookup() throws Exception {

        final String simpleBindingName1 = "java:global/simple1";
        final String simpleBindingName2 = "java:global/simple2";
        final String lookupBindingName = "java:global/lookup";
        Exception error = null;

        try {
            ModelNode operation = prepareAddBindingOperation(simpleBindingName1, SIMPLE);
            operation.get(VALUE).set("simple1");
            operation.get(TYPE).set(String.class.getName());
            ModelNode operationResult = managementClient.getControllerClient().execute(operation);
            verifyBinding(operationResult, simpleBindingName1, "simple1");

            operation = prepareAddBindingOperation(simpleBindingName2, SIMPLE);
            operation.get(VALUE).set("simple2");
            operation.get(TYPE).set(String.class.getName());
            operationResult = managementClient.getControllerClient().execute(operation);
            verifyBinding(operationResult, simpleBindingName2, "simple2");

            operation = prepareAddBindingOperation(lookupBindingName, LOOKUP);
            operation.get(LOOKUP).set(simpleBindingName1);
            operationResult = managementClient.getControllerClient().execute(operation);
            verifyBinding(operationResult, lookupBindingName, "simple1");

            operation = prepareRebindOperation(lookupBindingName, LOOKUP);
            operation.get(LOOKUP).set(simpleBindingName2);
            operationResult = managementClient.getControllerClient().execute(operation);
            verifyBinding(operationResult, lookupBindingName, "simple2");
        } catch (Exception e) {
            error = e;
            throw e;
        } finally {
            removeBinding(simpleBindingName1, error);
            removeBinding(simpleBindingName2, error);
            removeBinding(lookupBindingName, error);
        }
    }

    private void verifyBinding(ModelNode result, String bindingName, String bindingValue) throws Exception {
        Assert.assertFalse(result.get(FAILURE_DESCRIPTION).toString(), result.get(FAILURE_DESCRIPTION).isDefined());
        Assert.assertEquals(bindingValue, bean.lookupBind(bindingName).toString());
    }

    private void verifyBindingClass(ModelNode result, String bindingName, String bindingClassName) throws Exception {
        Class bindingClass = Class.forName(bindingClassName);
        Assert.assertFalse(result.get(FAILURE_DESCRIPTION).toString(), result.get(FAILURE_DESCRIPTION).isDefined());
        Assert.assertTrue(bindingClass.isInstance(bean.lookupBind(bindingName)));
    }

    private ModelNode prepareAddBindingOperation(String bindingName, String bindingType) {
        ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        operation.get(OP_ADDR).set(getBindingAddress(bindingName));
        operation.get(BINDING_TYPE).set(bindingType);
        return operation;
    }

    private ModelNode prepareRebindOperation(String bindingName, String bindingType) {
        ModelNode operation = new ModelNode();
        operation.get(OP).set(REBIND);
        operation.get(OP_ADDR).set(getBindingAddress(bindingName));
        operation.get(BINDING_TYPE).set(bindingType);
        return operation;
    }

    private ModelNode prepareReadResourceOperation(String bindingName) {
        ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(OP_ADDR).set(getBindingAddress(bindingName));
        return operation;
    }

    private ModelNode getBindingAddress(String bindingName) {
        ModelNode bindingAddress = new ModelNode();
        bindingAddress.add(SUBSYSTEM, NamingExtension.SUBSYSTEM_NAME);
        bindingAddress.add(BINDING, bindingName);
        return bindingAddress;
    }

    private void removeBinding (String bindingName, Exception testException) throws Exception {
        ModelNode removeOperation = new ModelNode();
        removeOperation.get(OP).set(REMOVE);
        removeOperation.get(OP_ADDR).set(getBindingAddress(bindingName));
        removeOperation.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        ModelNode removeResult = managementClient.getControllerClient().execute(removeOperation);
        if (testException == null) {
            //Only error here if the test was successful
            Assert.assertFalse(removeResult.get(FAILURE_DESCRIPTION).toString(), removeResult.get(FAILURE_DESCRIPTION)
                    .isDefined());
        } else {
            if (removeResult.get(FAILURE_DESCRIPTION).isDefined()) {
                throw new Exception(removeResult.get(FAILURE_DESCRIPTION) +
                        " - there was an exisiting exception in the test, it is added as the cause", testException);
            }
        }
    }

}
