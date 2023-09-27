/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
public abstract class AbstractComplexSubsystemTestCase extends AbstractSubsystemTest {

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
