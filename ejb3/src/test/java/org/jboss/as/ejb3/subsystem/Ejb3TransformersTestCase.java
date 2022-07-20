package org.jboss.as.ejb3.subsystem;

import static org.jboss.as.controller.capability.RuntimeCapability.buildDynamicCapabilityName;

import java.io.IOException;
import java.util.List;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelFixer;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;

/**
 * Test cases for transformers used in the EJB3 subsystem.
 * <p>
 * This checks the following features:
 * - basic subsystem testing (i.e. current model version boots successfully)
 * - registered transformers transform model and operations correctly between different API model versions
 * - expressions appearing in XML configurations are correctly rejected if so required
 * - bad attribute values are correctly rejected
 *
 * @author <a href="tomasz.cerar@redhat.com"> Tomasz Cerar</a>
 * @author Richard Achmatowicz (c) 2015 Red Hat Inc.
 * @author Richard Achmatowicz (c) 2020 Red Hat Inc.
 *
 * NOTE: References in this file to Enterprise JavaBeans(EJB) refer to the Jakarta Enterprise Beans unless otherwise noted.
 */
public class Ejb3TransformersTestCase extends AbstractSubsystemBaseTest {

    private static final String LEGACY_EJB_CLIENT_ARTIFACT = "org.jboss:jboss-ejb-client:2.1.2.Final";

    private static String formatWildflySubsystemArtifact(ModelTestControllerVersion version) {
        return formatArtifact("org.wildfly:wildfly-ejb3:%s", version);
    }

    private static String formatEAPSubsystemArtifact(ModelTestControllerVersion version) {
        return formatArtifact("org.jboss.eap:wildfly-ejb3:%s", version);
    }

    private static String formatLegacySubsystemArtifact(ModelTestControllerVersion version) {
        return formatArtifact("org.jboss.as:jboss-as-ejb3:%s", version);
    }

    private static String formatArtifact(String pattern, ModelTestControllerVersion version) {
        return String.format(pattern, version.getMavenGavVersion());
    }

    public Ejb3TransformersTestCase() {
        super(EJB3Extension.SUBSYSTEM_NAME, new EJB3Extension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/wildfly-ejb3_10_0.xsd";
    }

    /*
     * Add in required capabilities for external subsystem resources the ejb3 subsystem accesses
     */
    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.MANAGEMENT;
    }

    /**
     * Tests transformation of model from current version into specified version
     * The xml used should parse on legacy servers without rejections.
     *
     * @param model
     * @param controller
     * @param mavenResourceURLs
     * @throws Exception
     */
    private void testTransformation(ModelVersion model, ModelTestControllerVersion controller, String... mavenResourceURLs) throws Exception {

        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXmlResource("subsystem-ejb3-transform.xml");

        // initialize the legacy services and add required jars
        builder.createLegacyKernelServicesBuilder(null, controller, model).
                addMavenResourceURL(mavenResourceURLs).
                skipReverseControllerCheck();

        KernelServices services = builder.build();
        KernelServices legacyServices = services.getLegacyServices(model);

        Assert.assertTrue(services.isSuccessfulBoot());
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        // check that both versions of the legacy model are the same and valid
        checkSubsystemModelTransformation(services, model, new DefaultModelFixer());
    }

    protected AdditionalInitialization createAdditionalInitializationForRejectionTests() {
        return AdditionalInitialization.withCapabilities("org.wildfly.transactions.global-default-local-provider",
                buildDynamicCapabilityName("org.wildfly.security.security-domain", "ApplicationDomain"),
                buildDynamicCapabilityName("org.wildfly.remoting.connector", "http-remoting-connector"),
                buildDynamicCapabilityName("org.wildfly.remoting.connector", "remoting-connector"),
                buildDynamicCapabilityName("org.wildfly.clustering.infinispan.cache-container", "not-ejb"),
                buildDynamicCapabilityName("org.wildfly.ejb3.timer-service.timer-persistence-service", "file-data-store"),
                buildDynamicCapabilityName("org.wildfly.ejb3.mdb-delivery-group", "1"),
                buildDynamicCapabilityName("org.wildfly.ejb3.mdb-delivery-group", "2"),
                buildDynamicCapabilityName("org.wildfly.ejb3.mdb-delivery-group", "3"),
                buildDynamicCapabilityName("org.wildfly.ejb3.pool-config", "pool"),
                "org.wildfly.remoting.endpoint", "org.wildfly.security.jacc-policy");
    }

    private void testRejections(ModelVersion model, ModelTestControllerVersion controller, String... mavenResourceURLs) throws Exception {
        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder(this.createAdditionalInitializationForRejectionTests());

        // initialize the legacy services and add required jars
        builder.createLegacyKernelServicesBuilder(this.createAdditionalInitializationForRejectionTests(), controller, model)
                .addMavenResourceURL(mavenResourceURLs)
                .dontPersistXml();

        KernelServices services = builder.build();
        Assert.assertTrue(services.isSuccessfulBoot());
        KernelServices legacyServices = services.getLegacyServices(model);
        Assert.assertNotNull(legacyServices);
        Assert.assertTrue(legacyServices.isSuccessfulBoot());

        List<ModelNode> operations = builder.parseXmlResource("subsystem-ejb3-transform-reject.xml");

        ModelTestUtils.checkFailedTransformedBootOperations(services, model, operations, createFailedOperationTransformationConfig(services, model));
    }

    private static FailedOperationTransformationConfig createFailedOperationTransformationConfig(KernelServices services, ModelVersion version) {
        FailedOperationTransformationConfig config = new FailedOperationTransformationConfig();
        PathAddress subsystemAddress = PathAddress.pathAddress(EJB3Extension.SUBSYSTEM_PATH);

        // need to include all changes from current to 9.0.0
        if (EJB3Model.VERSION_9_0_0.requiresTransformation(version)) {
            // reject the resource /subsystem=ejb3/simple-cache
            config.addFailedAttribute(subsystemAddress.append(EJB3SubsystemModel.SIMPLE_CACHE_PATH), FailedOperationTransformationConfig.REJECTED_RESOURCE);
            // reject the resource /subsystem=ejb3/distributable-cache
            config.addFailedAttribute(subsystemAddress.append(EJB3SubsystemModel.DISTRIBUTABLE_CACHE_PATH), FailedOperationTransformationConfig.REJECTED_RESOURCE);
            // Reject when default-data-store is undefined
            config.addFailedAttribute(subsystemAddress.append(EJB3SubsystemModel.TIMER_SERVICE_PATH), FailedOperationTransformationConfig.REJECTED_RESOURCE);
        }
        return config;
    }

    /*
     * Class to fix up some irredeemable errors in the legacy model before comparison
     */
    private static class DefaultModelFixer implements ModelFixer {

        @Override
        public ModelNode fixModel(ModelNode modelNode) {

            // add any fixes required here

            return modelNode;
        }
    }
}
