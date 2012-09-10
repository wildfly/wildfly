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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.JVM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import junit.framework.Assert;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.core.model.test.KernelServices;
import org.jboss.as.core.model.test.ModelInitializer;
import org.jboss.as.core.model.test.ModelWriteSanitizer;
import org.jboss.as.core.model.test.TestModelType;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class DomainModelJvmModelTestCase extends GlobalJvmModelTestCase {

    static final PathElement PARENT = PathElement.pathElement(SERVER_GROUP, "groupA");

    public DomainModelJvmModelTestCase() {
        super(TestModelType.DOMAIN);
    }

    @Test
    public void testFullJvmXml() throws Exception {
        KernelServices kernelServices = createKernelServicesBuilder()
                .setXmlResource("domain-full.xml")
                .setModelInitializer(XML_MODEL_INITIALIZER, XML_MODEL_WRITE_SANITIZER)
                .build();
        Assert.assertTrue(kernelServices.isSuccessfulBoot());
        String xml = kernelServices.getPersistedSubsystemXml();
        ModelTestUtils.compareXml(ModelTestUtils.readResource(this.getClass(), "domain-full.xml"), xml);

        ModelNode model = kernelServices.readWholeModel();
        ModelNode jvmParent = model.get(SERVER_GROUP, "test", JVM);
        Assert.assertEquals(1, jvmParent.keys().size());
        checkFullJvm(jvmParent.get("full"));
    }

    @Test
    public void testEmptyJvmXml() throws Exception {
        KernelServices kernelServices = createKernelServicesBuilder()
                .setXmlResource("domain-empty.xml")
                .setModelInitializer(XML_MODEL_INITIALIZER, XML_MODEL_WRITE_SANITIZER)
                .build();
        Assert.assertTrue(kernelServices.isSuccessfulBoot());
        String xml = kernelServices.getPersistedSubsystemXml();
        ModelTestUtils.compareXml(ModelTestUtils.readResource(this.getClass(), "domain-empty.xml"), xml);

        ModelNode model = kernelServices.readWholeModel();
        ModelNode jvmParent = model.get(SERVER_GROUP, "test", JVM);
        Assert.assertEquals(1, jvmParent.keys().size());
        checkEmptyJvm(jvmParent.get("empty"));
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

    private static final ModelInitializer XML_MODEL_INITIALIZER = new ModelInitializer() {
        public void populateModel(Resource rootResource) {
            rootResource.registerChild(PathElement.pathElement(PROFILE, "test"), Resource.Factory.create());
            rootResource.registerChild(PathElement.pathElement(SOCKET_BINDING_GROUP, "test-sockets"), Resource.Factory.create());
        }
    };

    private final ModelWriteSanitizer XML_MODEL_WRITE_SANITIZER = new ModelWriteSanitizer() {
        @Override
        public ModelNode sanitize(ModelNode model) {
            //Remove the profile and socket-binding-group removed by the initializer so the xml does not include a profile
            model.remove(PROFILE);
            model.remove(SOCKET_BINDING_GROUP);
            return model;
        }
    };
}
