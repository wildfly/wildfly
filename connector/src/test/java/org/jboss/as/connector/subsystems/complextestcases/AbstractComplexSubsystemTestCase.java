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
package org.jboss.as.connector.subsystems.complextestcases;


import org.jboss.as.controller.Extension;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;

/**
 *
 * @author <a href="vrastsel@redhat.com">Vladimir Rastseluev</a>
 */
public class AbstractComplexSubsystemTestCase extends AbstractSubsystemTest {

    public AbstractComplexSubsystemTestCase(final String mainSubsystemName, final Extension mainExtension) {
        super(mainSubsystemName, mainExtension);
    }

    public ModelNode getModel(String resourceFileName, String archiveName) throws Exception {
        return getModel(resourceFileName, true, archiveName);
    }

    public ModelNode getModel(String resourceFileName) throws Exception {
        return getModel(resourceFileName, true, null);
    }

    public ModelNode getModel(String resourceFileName, boolean checkMarshalledXML, String archiveName) throws Exception {

        String xml = readResource(resourceFileName);

        KernelServices services = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXml(xml)
                .build();

        ModelNode model = services.readWholeModel();

        // Marshal the xml to see that it is the same as before
        String marshalled = services.getPersistedSubsystemXml();
        if (checkMarshalledXML)
            Assert.assertEquals(normalizeXML(xml), normalizeXML(marshalled));

        services = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXml(marshalled)
                .build();

        // Check that the model looks the same
        ModelNode modelReloaded = services.readWholeModel();
        compare(model, modelReloaded);

        assertRemoveSubsystemResources(services);
        return model;

    }


}
