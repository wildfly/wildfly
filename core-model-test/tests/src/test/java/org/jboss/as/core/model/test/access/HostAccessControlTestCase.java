/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.core.model.test.access;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN_CONTROLLER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LOCAL;

import org.jboss.as.controller.registry.Resource;
import org.jboss.as.core.model.test.AbstractCoreModelTest;
import org.jboss.as.core.model.test.KernelServices;
import org.jboss.as.core.model.test.ModelInitializer;
import org.jboss.as.core.model.test.ModelWriteSanitizer;
import org.jboss.as.core.model.test.TestModelType;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test of access control handling in the host model.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public class HostAccessControlTestCase extends AbstractCoreModelTest {

    @Test
    public void testConfiguration() throws Exception {

        KernelServices kernelServices = createKernelServicesBuilder(TestModelType.HOST)
                .setXmlResource("host.xml")
                .setModelInitializer(MODEL_SANITIZER, MODEL_WRITER_SANITIZER)
                .validateDescription()
                .build();
        Assert.assertTrue(kernelServices.isSuccessfulBoot());

//        System.out.println(kernelServices.readWholeModel());

        String marshalled = kernelServices.getPersistedSubsystemXml();
        ModelTestUtils.compareXml(ModelTestUtils.readResource(this.getClass(), "host.xml"), marshalled);
    }

    private final ModelInitializer MODEL_SANITIZER = new ModelInitializer() {
        @Override
        public void populateModel(Resource rootResource) {
        }
    };

    private final ModelWriteSanitizer MODEL_WRITER_SANITIZER = new ModelWriteSanitizer() {
        @Override
        public ModelNode sanitize(ModelNode model) {
            //The write-local-domain-controller operation gets removed on boot by TestModelControllerService
            //so add that resource here since we're using that from the xml
            model.get(DOMAIN_CONTROLLER, LOCAL).setEmptyObject();
            return model;
        }
    };
}

