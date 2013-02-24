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

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.core.model.test.KernelServices;
import org.jboss.as.core.model.test.ModelInitializer;
import org.jboss.as.core.model.test.TestModelType;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class HostModelJvmModelTestCase extends GlobalJvmModelTestCase {

    static final PathElement PARENT = PathElement.pathElement(HOST, "master");

    public HostModelJvmModelTestCase() {
        super(TestModelType.HOST);
    }


    @Test
    public void testXml() throws Exception {
        KernelServices kernelServices = createKernelServicesBuilder()
                .setXmlResource("host-global.xml")
                .build();
        Assert.assertTrue(kernelServices.isSuccessfulBoot());
        String xml = kernelServices.getPersistedSubsystemXml();
        ModelTestUtils.compareXml(ModelTestUtils.readResource(this.getClass(), "host-global.xml"), xml);

        ModelNode model = kernelServices.readWholeModel();
        ModelNode jvmParent = model.get(HOST, "master", JVM);
        Assert.assertEquals(2, jvmParent.keys().size());

        checkEmptyJvm(jvmParent.get("empty"));
        checkFullJvm(jvmParent.get("full"));
    }

    @Override
    protected ModelInitializer getModelInitializer() {
        return new ModelInitializer() {
            @Override
            public void populateModel(Resource rootResource) {
                //Register the server group resource that will be the parent of the domain
                rootResource.registerChild(PARENT, Resource.Factory.create());
            }
        };
    }

    @Override
    protected ModelNode getPathAddress(String jvmName, String... subaddress) {
        return PathAddress.pathAddress(PARENT, PathElement.pathElement(JVM, "test")).toModelNode();
    }

}
