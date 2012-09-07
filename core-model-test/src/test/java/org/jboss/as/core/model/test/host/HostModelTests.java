package org.jboss.as.core.model.test.host;

import junit.framework.Assert;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.core.model.test.AbstractCoreModelTest;
import org.jboss.as.core.model.test.KernelServices;
import org.jboss.as.core.model.test.ModelType;
import org.jboss.as.model.test.ModelTestUtils;
import org.junit.Test;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public class HostModelTests extends AbstractCoreModelTest {

    @Test
    public void testDefaultHostXml() throws Exception {
        KernelServices kernelServices = createKernelServicesBuilder(ModelType.HOST)
                .setXmlResource("host.xml")
                .build();
        Assert.assertTrue(kernelServices.isSuccessfulBoot());
        String xml = kernelServices.getPersistedSubsystemXml();
        ModelTestUtils.compareXml(ModelTestUtils.readResource(this.getClass(), "host.xml"), xml);
        ModelTestUtils.validateModelDescriptions(PathAddress.EMPTY_ADDRESS, kernelServices.getRootRegistration());


    }


}
