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
package org.jboss.as.core.model.test.systemproperty;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import junit.framework.Assert;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.core.model.test.KernelServices;
import org.jboss.as.core.model.test.KernelServicesBuilder;
import org.jboss.as.core.model.test.ModelInitializer;
import org.jboss.as.core.model.test.ModelType;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.dmr.ModelNode;


/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class HostServerSystemPropertyTestCase extends AbstractSystemPropertyTest {
    static final PathElement HOST_ELEMENT = PathElement.pathElement(HOST, "master");
    static final PathElement SERVER_ONE_ELEMENT = PathElement.pathElement(SERVER_CONFIG, "server-one");
    static final PathElement SERVER_TWO_ELEMENT = PathElement.pathElement(SERVER_CONFIG, "server-two");
    static final PathAddress SERVER_ONE_ADDRESS = PathAddress.pathAddress(HOST_ELEMENT, SERVER_ONE_ELEMENT);

    public HostServerSystemPropertyTestCase() {
        super(false);
    }


    protected PathAddress getSystemPropertyAddress(String propName) {
        return SERVER_ONE_ADDRESS.append(PathElement.pathElement(SYSTEM_PROPERTY, propName));
    }

    protected KernelServicesBuilder createKernelServicesBuilder(boolean xml) {
        return createKernelServicesBuilder(ModelType.HOST);
    }

    protected KernelServices createEmptyRoot() throws Exception {
        KernelServices kernelServices = createKernelServicesBuilder(false).setModelInitializer(BOOT_OP_MODEL_INITIALIZER, null).build();
        Assert.assertTrue(kernelServices.isSuccessfulBoot());
        return kernelServices;
    }

    protected ModelNode readSystemPropertiesParentModel(KernelServices kernelServices) {
        ModelNode model = kernelServices.readWholeModel();
        return ModelTestUtils.getSubModel(model, SERVER_ONE_ADDRESS).get(SYSTEM_PROPERTY);
    }

    @Override
    protected String getXmlResource() {
        return "host-server-systemproperties.xml";
    }

    private ModelInitializer BOOT_OP_MODEL_INITIALIZER = new ModelInitializer() {
        @Override
        public void populateModel(Resource rootResource) {
            Resource host = Resource.Factory.create();
            rootResource.registerChild(HOST_ELEMENT, host);
            host.registerChild(SERVER_ONE_ELEMENT, Resource.Factory.create());
            host.registerChild(SERVER_TWO_ELEMENT, Resource.Factory.create());
        }
    };
}
