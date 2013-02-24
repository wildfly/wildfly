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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.core.model.test.KernelServices;
import org.jboss.as.core.model.test.KernelServicesBuilder;
import org.jboss.as.core.model.test.TestModelType;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Before;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class StandaloneSystemPropertyTestCase extends AbstractSystemPropertyTest {

    static final String PROP_ONE = "sys.prop.test.one";
    static final String PROP_TWO = "sys.prop.test.two";

    static final PathAddress PARENT = PathAddress.EMPTY_ADDRESS;

    public StandaloneSystemPropertyTestCase() {
        super(true, false);
    }

    @Before
    public void clearProperties() {
        System.clearProperty(PROP_ONE);
        System.clearProperty(PROP_TWO);
    }


    protected PathAddress getSystemPropertyAddress(String propName) {
        return PathAddress.pathAddress(PathElement.pathElement(SYSTEM_PROPERTY, propName));
    }


    protected KernelServicesBuilder createKernelServicesBuilder(boolean xml) {
        return createKernelServicesBuilder(TestModelType.STANDALONE);
    }

    protected KernelServices createEmptyRoot() throws Exception {
        KernelServices kernelServices = createKernelServicesBuilder(false).build();
        Assert.assertTrue(kernelServices.isSuccessfulBoot());
        return kernelServices;
    }

    protected ModelNode readSystemPropertiesParentModel(KernelServices kernelServices) {
        ModelNode model = kernelServices.readWholeModel().get(SYSTEM_PROPERTY);
        return model;
    }

    @Override
    protected String getXmlResource() {
        return "standalone-systemproperties.xml";
    }
}
