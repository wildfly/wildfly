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
package org.jboss.as.core.model.test.jvm;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.JVM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;
import junit.framework.Assert;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.core.model.test.KernelServices;
import org.jboss.as.core.model.test.ModelInitializer;
import org.jboss.as.core.model.test.ModelType;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class HostServerJvmModelTestCase extends AbstractJvmModelTest {

    static final PathElement HOST_ELEMENT = PathElement.pathElement(HOST, "master");
    static final PathElement SERVER_ONE_ELEMENT = PathElement.pathElement(SERVER_CONFIG, "server-one");
    static final PathElement SERVER_TWO_ELEMENT = PathElement.pathElement(SERVER_CONFIG, "server-two");
    public HostServerJvmModelTestCase() {
        super(ModelType.HOST, true);
    }

    @Test
    public void testWriteDebugEnabled() throws Exception {
        KernelServices kernelServices = doEmptyJvmAdd();
        ModelNode value = new ModelNode(true);
        Assert.assertEquals(value, writeTest(kernelServices, "debug-enabled", value));
    }

    @Test
    public void testWriteDebugOptions() throws Exception {
        KernelServices kernelServices = doEmptyJvmAdd();
        ModelNode value = new ModelNode("abc");
        Assert.assertEquals(value, writeTest(kernelServices, "debug-options", value));
    }

    @Test
    public void testFullServerJvmXml() throws Exception {
        KernelServices kernelServices = createKernelServicesBuilder()
            .setXmlResource("host-server-full.xml")
            .build();

        Assert.assertTrue(kernelServices.isSuccessfulBoot());
        String xml = kernelServices.getPersistedSubsystemXml();
        ModelTestUtils.compareXml(ModelTestUtils.readResource(this.getClass(), "host-server-full.xml"), xml);

        //Inspect the actual model
        ModelNode full = kernelServices.readWholeModel().get(HOST, "master", SERVER_CONFIG, "server-one", JVM, "full");
        checkFullJvm(full);
    }

    @Test
    public void testEmptyServerJvmXml() throws Exception {
        KernelServices kernelServices = createKernelServicesBuilder()
            .setXmlResource("host-server-empty.xml")
            .build();

        Assert.assertTrue(kernelServices.isSuccessfulBoot());
        String xml = kernelServices.getPersistedSubsystemXml();
        ModelTestUtils.compareXml(ModelTestUtils.readResource(this.getClass(), "host-server-empty.xml"), xml);

        //Inspect the actual model
        ModelNode empty = kernelServices.readWholeModel().get(HOST, "master", SERVER_CONFIG, "server-one", JVM, "empty");
        checkEmptyJvm(empty);
    }

    @Override
    protected ModelInitializer getModelInitializer() {
        return bootOpModelInitializer;
    }

    @Override
    protected ModelNode getPathAddress(String jvmName, String... subaddress) {
        return PathAddress.pathAddress(HOST_ELEMENT, SERVER_ONE_ELEMENT, PathElement.pathElement(JVM, "test")).toModelNode();
    }

    private ModelInitializer bootOpModelInitializer = new ModelInitializer() {
        @Override
        public void populateModel(Resource rootResource) {
            Resource host = Resource.Factory.create();
            rootResource.registerChild(HOST_ELEMENT, host);
            host.registerChild(SERVER_ONE_ELEMENT, Resource.Factory.create());
            host.registerChild(SERVER_TWO_ELEMENT, Resource.Factory.create());
        }
    };
}
