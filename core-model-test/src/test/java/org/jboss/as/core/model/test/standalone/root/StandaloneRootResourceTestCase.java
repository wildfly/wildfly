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
package org.jboss.as.core.model.test.standalone.root;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.net.InetAddress;
import java.util.List;

import junit.framework.Assert;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.NamespaceAddHandler;
import org.jboss.as.controller.operations.common.NamespaceRemoveHandler;
import org.jboss.as.controller.operations.common.SchemaLocationAddHandler;
import org.jboss.as.controller.operations.common.SchemaLocationRemoveHandler;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.Namespace;
import org.jboss.as.core.model.test.AbstractCoreModelTest;
import org.jboss.as.core.model.test.KernelServices;
import org.jboss.as.core.model.test.KernelServicesBuilder;
import org.jboss.as.core.model.test.TestModelType;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.server.controller.descriptions.ServerDescriptionConstants;
import org.jboss.as.version.Version;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class StandaloneRootResourceTestCase extends AbstractCoreModelTest {

    @Test
    public void testEmptyRoot() throws Exception {
        KernelServices kernelServices = createEmptyRoot();

        ModelNode model = kernelServices.readWholeModel(false, true);
        assertAttribute(model, ModelDescriptionConstants.NAMESPACES, new ModelNode().setEmptyList());
        assertAttribute(model, ModelDescriptionConstants.SCHEMA_LOCATIONS, new ModelNode().setEmptyList());
        assertAttribute(model, ModelDescriptionConstants.NAME, new ModelNode(getDefaultServerName()));
        assertAttribute(model, ModelDescriptionConstants.PRODUCT_NAME, null);
        assertAttribute(model, ModelDescriptionConstants.PRODUCT_VERSION, null);
        assertAttribute(model, ModelDescriptionConstants.MANAGEMENT_MAJOR_VERSION, new ModelNode(Version.MANAGEMENT_MAJOR_VERSION));
        assertAttribute(model, ModelDescriptionConstants.MANAGEMENT_MINOR_VERSION, new ModelNode(Version.MANAGEMENT_MINOR_VERSION));
        assertAttribute(model, ModelDescriptionConstants.MANAGEMENT_MICRO_VERSION, new ModelNode(Version.MANAGEMENT_MICRO_VERSION));
        assertAttribute(model, ServerDescriptionConstants.PROCESS_STATE, new ModelNode("running"));
        assertAttribute(model, ServerDescriptionConstants.PROCESS_TYPE, new ModelNode("Server"));
        assertAttribute(model, ServerDescriptionConstants.LAUNCH_TYPE, new ModelNode("STANDALONE"));

        //These two cannot work in tests - placeholder
        assertAttribute(model, ModelDescriptionConstants.RELEASE_VERSION, new ModelNode("Unknown"));
        assertAttribute(model, ModelDescriptionConstants.RELEASE_CODENAME, new ModelNode("Unknown"));


        //Try changing namespaces, schema-locations and name
    }

    @Test
    public void testName() throws Exception {
        KernelServices kernelServices = createEmptyRoot();

        ModelNode read = Util.createOperation(READ_ATTRIBUTE_OPERATION, PathAddress.EMPTY_ADDRESS);
        read.get(NAME).set(NAME);
        ModelNode originalName = kernelServices.executeForResult(read);

        ModelNode write = Util.createOperation(WRITE_ATTRIBUTE_OPERATION, PathAddress.EMPTY_ADDRESS);
        write.get(NAME).set(NAME);
        write.get(VALUE).set("test");
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(write));

        Assert.assertEquals("test", kernelServices.executeForResult(read).asString());

        //Unset the name
        write.get(VALUE).set(new ModelNode());
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(write));
        Assert.assertEquals(originalName, kernelServices.executeForResult(read));
    }

    @Test
    public void testNameSetInXml() throws Exception {
        String originalXml = "<server xmlns=\"" + Namespace.CURRENT.getUriString() + "\" name=\"testing\"/>";
        KernelServices kernelServices = createKernelServicesBuilder()
                .setXml(originalXml)
                .build();
        Assert.assertTrue(kernelServices.isSuccessfulBoot());

        ModelNode read = Util.createOperation(READ_ATTRIBUTE_OPERATION, PathAddress.EMPTY_ADDRESS);
        read.get(NAME).set(NAME);
        Assert.assertEquals("testing", kernelServices.executeForResult(read).asString());

        String persistedXml = kernelServices.getPersistedSubsystemXml();
        ModelTestUtils.compareXml(originalXml, persistedXml);
    }

    @Test
    public void testNoNameSetInXml() throws Exception {
        String originalXml = "<server xmlns=\"" + Namespace.CURRENT.getUriString() + "\"/>";
        KernelServices kernelServices = createKernelServicesBuilder()
                .setXml(originalXml)
                .build();
        Assert.assertTrue(kernelServices.isSuccessfulBoot());

        ModelNode read = Util.createOperation(READ_ATTRIBUTE_OPERATION, PathAddress.EMPTY_ADDRESS);
        read.get(NAME).set(NAME);
        Assert.assertEquals(getDefaultServerName(), kernelServices.executeForResult(read).asString());

        //Add and remove a system property so that some ops get executed on the model to trigger persistence, as it is there are none
        ModelNode add = Util.createAddOperation(PathAddress.pathAddress(PathElement.pathElement(ModelDescriptionConstants.SYSTEM_PROPERTY, "test")));
        add.get(VALUE).set("123");
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(add));
        ModelNode remove = Util.createRemoveOperation(PathAddress.pathAddress(PathElement.pathElement(ModelDescriptionConstants.SYSTEM_PROPERTY, "test")));
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(remove));

        String persistedXml = kernelServices.getPersistedSubsystemXml();
        ModelTestUtils.compareXml(originalXml, persistedXml);
    }

    @Test
    public void testNamespaces() throws Exception {
        KernelServices kernelServices = createEmptyRoot();

        ModelNode read = Util.createOperation(READ_ATTRIBUTE_OPERATION, PathAddress.EMPTY_ADDRESS);
        read.get(NAME).set(ModelDescriptionConstants.NAMESPACES);
        Assert.assertEquals(new ModelNode().setEmptyList(), kernelServices.executeForResult(read));

        ModelNode add = Util.createOperation(NamespaceAddHandler.OPERATION_NAME, PathAddress.EMPTY_ADDRESS);
        add.get(ModelDescriptionConstants.NAMESPACE).set("one");
        add.get(ModelDescriptionConstants.URI).set("urn:uno");
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(add));

        ModelNode result = kernelServices.executeForResult(read);
        List<ModelNode> list = result.asList();
        Assert.assertEquals(1, list.size());
        Property prop = list.get(0).asProperty();
        Assert.assertEquals("one", prop.getName());
        Assert.assertEquals("urn:uno", prop.getValue().asString());

        add.get(ModelDescriptionConstants.NAMESPACE).set("two");
        add.get(ModelDescriptionConstants.URI).set("urn:dos");
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(add));

        result = kernelServices.executeForResult(read);
        list = result.asList();
        Assert.assertEquals(2, list.size());
        prop = list.get(0).asProperty();
        Assert.assertEquals("one", prop.getName());
        Assert.assertEquals("urn:uno", prop.getValue().asString());
        prop = list.get(1).asProperty();
        Assert.assertEquals("two", prop.getName());
        Assert.assertEquals("urn:dos", prop.getValue().asString());

        ModelNode remove = Util.createOperation(NamespaceRemoveHandler.OPERATION_NAME, PathAddress.EMPTY_ADDRESS);
        remove.get(ModelDescriptionConstants.NAMESPACE).set("one");
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(remove));

        result = kernelServices.executeForResult(read);
        list = result.asList();
        Assert.assertEquals(1, list.size());
        prop = list.get(0).asProperty();
        Assert.assertEquals("two", prop.getName());
        Assert.assertEquals("urn:dos", prop.getValue().asString());

        remove.get(ModelDescriptionConstants.NAMESPACE).set("two");
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(remove));

        result = kernelServices.executeForResult(read);
        Assert.assertEquals(new ModelNode().setEmptyList(), result);

        remove.get(ModelDescriptionConstants.NAMESPACE).set("blah");
        kernelServices.executeForFailure(remove);
    }

    @Test
    public void testSchemaLocations() throws Exception {
        KernelServices kernelServices = createEmptyRoot();

        ModelNode read = Util.createOperation(READ_ATTRIBUTE_OPERATION, PathAddress.EMPTY_ADDRESS);
        read.get(NAME).set(ModelDescriptionConstants.SCHEMA_LOCATIONS);
        Assert.assertEquals(new ModelNode().setEmptyList(), kernelServices.executeForResult(read));

        ModelNode add = Util.createOperation(SchemaLocationAddHandler.OPERATION_NAME, PathAddress.EMPTY_ADDRESS);
        add.get(ModelDescriptionConstants.URI).set("one");
        add.get(ModelDescriptionConstants.SCHEMA_LOCATION).set("loc-one");
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(add));

        ModelNode result = kernelServices.executeForResult(read);
        List<ModelNode> list = result.asList();
        Assert.assertEquals(1, list.size());
        Property prop = list.get(0).asProperty();
        Assert.assertEquals("one", prop.getName());
        Assert.assertEquals("loc-one", prop.getValue().asString());

        add.get(ModelDescriptionConstants.URI).set("two");
        add.get(ModelDescriptionConstants.SCHEMA_LOCATION).set("loc-two");
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(add));

        result = kernelServices.executeForResult(read);
        list = result.asList();
        Assert.assertEquals(2, list.size());
        prop = list.get(0).asProperty();
        Assert.assertEquals("one", prop.getName());
        Assert.assertEquals("loc-one", prop.getValue().asString());
        prop = list.get(1).asProperty();
        Assert.assertEquals("two", prop.getName());
        Assert.assertEquals("loc-two", prop.getValue().asString());

        ModelNode remove = Util.createOperation(SchemaLocationRemoveHandler.OPERATION_NAME, PathAddress.EMPTY_ADDRESS);
        remove.get(ModelDescriptionConstants.URI).set("one");
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(remove));

        result = kernelServices.executeForResult(read);
        list = result.asList();
        Assert.assertEquals(1, list.size());
        prop = list.get(0).asProperty();
        Assert.assertEquals("two", prop.getName());
        Assert.assertEquals("loc-two", prop.getValue().asString());

        remove.get(ModelDescriptionConstants.URI).set("two");
        ModelTestUtils.checkOutcome(kernelServices.executeOperation(remove));

        Assert.assertEquals(new ModelNode().setEmptyList(), kernelServices.executeForResult(read));

        remove.get(ModelDescriptionConstants.URI).set("blah");
        kernelServices.executeForFailure(remove);
    }

    private void assertAttribute(ModelNode model, String name, ModelNode expected) {
        if (expected == null) {
            Assert.assertFalse(model.get(name).isDefined());
        } else {
            ModelNode actual = model.get(name);
            Assert.assertEquals(expected, actual);
        }
    }

    private KernelServices createEmptyRoot() throws Exception {
        KernelServices kernelServices = createKernelServicesBuilder().build();
        Assert.assertTrue(kernelServices.isSuccessfulBoot());
        return kernelServices;
    }

    private String getDefaultServerName() throws Exception {
        String hostName = InetAddress.getLocalHost().getHostName().toLowerCase();
        int index = hostName.indexOf('.');
        return index == -1 ? hostName : hostName.substring(0, index);
    }

    private KernelServicesBuilder createKernelServicesBuilder() {
        return super.createKernelServicesBuilder(TestModelType.STANDALONE);
    }
}
