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
package org.jboss.as.clustering.infinispan.subsystem;

import java.io.IOException;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author <a href="tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */

//@RunWith(BMUnitRunner.class)
public class InfinispanSubsystemTransformerTestCase extends AbstractSubsystemBaseTest {


    public InfinispanSubsystemTransformerTestCase() {
        super(InfinispanExtension.SUBSYSTEM_NAME, new InfinispanExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("infinispan-transformer_1_4.xml");
    }

    @Ignore
    @Test
//    @BMRule(name="Debugging support",
//            targetClass="^org.jboss.as.subsystem.test.SubsystemTestDelegate",
//            targetMethod="checkSubsystemModelTransformation",
//            targetLocation="AT INVOKE ModelTestUtils.compare",
//            binding="legacy:ModelNode = $1; transformed:ModelNode = $2",
//            condition="TRUE",
//            action="traceln(\"legacy = \" + legacy.toString() + \" transformed = \" + transformed.toString()")
    public void testTransformer_1_3_0() throws Exception {
        ModelVersion version = ModelVersion.create(1, 3);
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXml(getSubsystemXml());
        builder.createLegacyKernelServicesBuilder(null, version)
            .addMavenResourceURL("org.jboss.as:jboss-as-clustering-infinispan:7.1.2.Final");

        KernelServices mainServices = builder.build();

        checkSubsystemModelTransformation(mainServices, version);
    }
}
