package org.jboss.as.core.model.test.host;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.core.model.test.*;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN_CONTROLLER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LOCAL;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public class HostModelTestCase extends AbstractCoreModelTest {

    @Test
    public void testDefaultHostXml() throws Exception {
        doHostXml("host.xml");
    }

    @Test
    public void testDefaultHostXmlWithExpressions() throws Exception {
        doHostXml("host-with-expressions.xml");
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

    private void doHostXml(String hostXmlFile) throws Exception {
        KernelServices kernelServices = createKernelServicesBuilder(TestModelType.HOST)
                .setXmlResource(hostXmlFile)
                .setModelInitializer(MODEL_SANITIZER, MODEL_WRITER_SANITIZER)
                .build();
        Assert.assertTrue(kernelServices.isSuccessfulBoot());
        String xml = kernelServices.getPersistedSubsystemXml();
        ModelTestUtils.compareXml(ModelTestUtils.readResource(this.getClass(), hostXmlFile), xml);
        ModelTestUtils.validateModelDescriptions(PathAddress.EMPTY_ADDRESS, kernelServices.getRootRegistration());
    }
}
