/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.core.model.test.socketbindinggroups;

import org.jboss.as.core.model.test.AbstractCoreModelTest;
import org.jboss.as.core.model.test.KernelServices;
import org.jboss.as.core.model.test.KernelServicesBuilder;
import org.jboss.as.core.model.test.TestModelType;
import org.jboss.as.model.test.ModelTestUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public abstract class AbstractSocketBindingGroupTest extends AbstractCoreModelTest {

    private final TestModelType type;

    public AbstractSocketBindingGroupTest(TestModelType type) {
        this.type = type;
    }

    @Test
    public void testSocketBindingGroups() throws Exception {
        KernelServices kernelServices = createKernelServicesBuilder()
                .setXmlResource(getXmlResource())
                .build();
            Assert.assertTrue(kernelServices.isSuccessfulBoot());

            String marshalled = kernelServices.getPersistedSubsystemXml();
            ModelTestUtils.compareXml(ModelTestUtils.readResource(this.getClass(), getXmlResource()), marshalled);

            kernelServices = createKernelServicesBuilder()
                    .setXml(marshalled)
                    .build();
                Assert.assertTrue(kernelServices.isSuccessfulBoot());
        }

        KernelServicesBuilder createKernelServicesBuilder() {
            return createKernelServicesBuilder(type);
    }

    protected abstract String getXmlResource();
}
