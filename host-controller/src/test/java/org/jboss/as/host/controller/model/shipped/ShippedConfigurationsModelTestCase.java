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
package org.jboss.as.host.controller.model.shipped;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import junit.framework.Assert;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.host.controller.test.AbstractCoreModelTest;
import org.jboss.as.host.controller.test.KernelServices;
import org.jboss.as.host.controller.test.ModelInitializer;
import org.jboss.as.host.controller.test.ModelWriteSanitizer;
import org.jboss.as.host.controller.test.Type;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ShippedConfigurationsModelTestCase extends AbstractCoreModelTest {

    @Test
    public void testDomainXml() throws Exception {
        testConfiguration(Type.DOMAIN, "domain.xml",
                new ModelInitializer() {
                    public void populateModel(Resource rootResource) {
                        rootResource.registerChild(PathElement.pathElement(PROFILE, "full"), Resource.Factory.create());
                        rootResource.registerChild(PathElement.pathElement(PROFILE, "full-ha"), Resource.Factory.create());
                    }
                },
                new ModelWriteSanitizer() {
                    public ModelNode sanitize(ModelNode model) {
                        model.remove(PROFILE);
                        return model;
                    }
                });
    }

    @Test
    public void testHostXml() throws Exception {
        testConfiguration(Type.HOST, "host.xml", null, null);
    }

    @Test
    public void testStandaloneXml() throws Exception {
        testConfiguration(Type.STANDALONE, "standalone.xml", null, null);
    }

    private void testConfiguration(Type type, String xmlResource, ModelInitializer initializer, ModelWriteSanitizer sanitizer) throws Exception {
        KernelServices kernelServices = createKernelServicesBuilder(type)
                .setXmlResource(xmlResource)
                .setModelInitializer(initializer, sanitizer)
                .build();
            Assert.assertTrue(kernelServices.isSuccessfulBoot());

            String marshalled = kernelServices.getPersistedSubsystemXml();
            ModelTestUtils.compareXml(ModelTestUtils.readResource(this.getClass(), xmlResource), marshalled);

    }

}
