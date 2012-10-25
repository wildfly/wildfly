/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.logging;

import java.io.IOException;

import junit.framework.Assert;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.junit.Test;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class SubsystemParsing11Test extends AbstractSubsystemBaseTest {


    public SubsystemParsing11Test() {
        super(LoggingExtension.SUBSYSTEM_NAME, new LoggingExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("/logging_1_1.xml");
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return LoggingTestEnvironment.getManagementInstance();
    }

    @Override
    @Test
    public void testSubsystem() throws Exception {
        // Don't compare xml
        standardSubsystemTest(null, false);
    }

    @Test
    public void testTransformers() throws Exception {
        String subsystemXml = readResource("/logging_1_1.xml");
        ModelVersion modelVersion = ModelVersion.create(1, 1, 0);
        KernelServicesBuilder builder = createKernelServicesBuilder(LoggingTestEnvironment.getManagementInstance())
                .setSubsystemXml(subsystemXml);

        //which is why we need to include the jboss-as-controller artifact.
        builder.createLegacyKernelServicesBuilder(LoggingTestEnvironment.getManagementInstance(), modelVersion)
                .addMavenResourceURL("org.jboss.as:jboss-as-logging:7.1.2.Final")
                .addMavenResourceURL("org.jboss.as:jboss-as-controller:7.1.2.Final")
                .addParentFirstClassPattern("org.jboss.as.controller.*");

        KernelServices mainServices = builder.build();
        Assert.assertTrue(mainServices.isSuccessfulBoot());
        Assert.assertTrue(mainServices.getLegacyServices(modelVersion).isSuccessfulBoot());
    }
}
