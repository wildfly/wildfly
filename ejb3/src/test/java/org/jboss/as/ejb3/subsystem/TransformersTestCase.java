package org.jboss.as.ejb3.subsystem;

import static org.jboss.as.controller.capability.RuntimeCapability.buildDynamicCapabilityName;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.ejb3.subsystem.IdentityResourceDefinition.IDENTITY_CAPABILITY;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test cases for transformers used in the EJB3 subsystem.
 *
 * @author <a href="tomasz.cerar@redhat.com"> Tomasz Cerar</a>
 * @author Richard Achmatowicz (c) 2015 Red Hat Inc.
 */
public class TransformersTestCase extends AbstractSubsystemBaseTest {

    private static String formatSubsystemArtifact(ModelTestControllerVersion version) {
        return formatArtifact("org.wildfly:wildfly-ejb3:%s", version);
    }

    private static String formatLegacySubsystemArtifact(ModelTestControllerVersion version) {
        return formatArtifact("org.jboss.as:jboss-as-ejb3:%s", version);
    }

    private static String formatArtifact(String pattern, ModelTestControllerVersion version) {
        return String.format(pattern, version.getMavenGavVersion());
    }

    public TransformersTestCase() {
        super(EJB3Extension.SUBSYSTEM_NAME, new EJB3Extension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/wildfly-ejb3_5_0.xsd";
    }

    @Override
    protected String[] getSubsystemTemplatePaths() throws IOException {
        return new String[] {
                "/subsystem-templates/ejb3.xml"
        };
    }

    @Test
    public void testTransformerEAP620() throws Exception {
        ModelTestControllerVersion controller = ModelTestControllerVersion.EAP_6_2_0;
        testTransformation(ModelVersion.create(1, 2, 1), controller,
                formatLegacySubsystemArtifact(controller),
                formatArtifact("org.jboss.as:jboss-as-threads:%s", controller));
    }

    @Test
    public void testTransformerEAP630() throws Exception {
        ModelTestControllerVersion controller = ModelTestControllerVersion.EAP_6_3_0;
        testTransformation(ModelVersion.create(1, 2, 1), controller,
                formatLegacySubsystemArtifact(controller),
                formatArtifact("org.jboss.as:jboss-as-threads:%s", controller));
    }

    @Test
    public void testTransformerEAP640() throws Exception {
        ModelTestControllerVersion controller = ModelTestControllerVersion.EAP_6_4_0;
        testTransformation(ModelVersion.create(1, 3, 0), controller,
                formatLegacySubsystemArtifact(controller),
                formatArtifact("org.jboss.as:jboss-as-threads:%s", controller));
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
    private void testTransformation(ModelVersion model, ModelTestControllerVersion controller, String ... mavenResourceURLs) throws Exception {

        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXmlResource("subsystem-ejb3-transform.xml");

        // initialize the legacy services and add required jars
        builder.createLegacyKernelServicesBuilder(null, controller, model).
                addMavenResourceURL(mavenResourceURLs).
                skipReverseControllerCheck();

        KernelServices services = builder.build();

        Assert.assertTrue(services.isSuccessfulBoot());
        Assert.assertTrue(services.getLegacyServices(model).isSuccessfulBoot());

        // check that both versions of the legacy model are the same and valid
        checkSubsystemModelTransformation(services, model, null);
    }

    @Test
    public void testRejectionsEAP620() throws Exception {
        ModelTestControllerVersion controller = ModelTestControllerVersion.EAP_6_2_0;
        this.testRejections(ModelVersion.create(1, 2, 1), controller,
                formatLegacySubsystemArtifact(controller),
                formatArtifact("org.jboss.as:jboss-as-threads:%s", controller));
    }

    @Test
    public void testRejectionsEAP630() throws Exception {
        ModelTestControllerVersion controller = ModelTestControllerVersion.EAP_6_3_0;
        this.testRejections(ModelVersion.create(1, 2, 1), controller,
                formatLegacySubsystemArtifact(controller),
                formatArtifact("org.jboss.as:jboss-as-threads:%s", controller));
    }

    @Test
    public void testRejectionsEAP640() throws Exception {
        ModelTestControllerVersion controller = ModelTestControllerVersion.EAP_6_4_0;
        this.testRejections(ModelVersion.create(1, 3, 0), controller,
                formatLegacySubsystemArtifact(controller),
                formatArtifact("org.jboss.as:jboss-as-threads:%s", controller));
    }

    private void testRejections(ModelVersion model, ModelTestControllerVersion controller, String ... mavenResourceURLs) throws Exception {
        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder(this.createAdditionalInitialization().withCapabilities(buildDynamicCapabilityName("org.wildfly.security.security-domain", "ApplicationDomain"), IDENTITY_CAPABILITY));

        // initialize the legacy services and add required jars
        builder.createLegacyKernelServicesBuilder(null, controller, model)
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

        if (EJB3Model.VERSION_1_2_1.matches(version)) {

            // create a chained config to apply multiple transformation configs to each one of a collection of attributes
            FailedOperationTransformationConfig.ChainedConfig chainedConfig = FailedOperationTransformationConfig.ChainedConfig.createBuilder(
                    /*EJB3SubsystemRootResourceDefinition.DEFAULT_SFSB_PASSIVATION_DISABLED_CACHE,*/ EJB3SubsystemRootResourceDefinition.DISABLE_DEFAULT_EJB_PERMISSIONS)
                    .addConfig(new FailedOperationTransformationConfig.NewAttributesConfig(
                            /*EJB3SubsystemRootResourceDefinition.DEFAULT_SFSB_PASSIVATION_DISABLED_CACHE,*/ EJB3SubsystemRootResourceDefinition.LOG_EJB_EXCEPTIONS))
                    .addConfig(new CorrectFalseToTrue(EJB3SubsystemRootResourceDefinition.DISABLE_DEFAULT_EJB_PERMISSIONS))
                    .build();

            // discard new attributes default-sfsb-passivation-disabled-cache, disable-default-ejb-permissions
            config.addFailedAttribute(subsystemAddress, chainedConfig);

            // make sure that we have a file-data-store matching the custom-data-store attribute value
            final PathAddress timerServiceAddr = subsystemAddress.append(EJB3SubsystemModel.TIMER_SERVICE_PATH);
            final PathAddress badFileStoreAddr = timerServiceAddr.append(PathElement.pathElement(EJB3SubsystemModel.FILE_DATA_STORE, "file-data-store-rename-to-default"));
            final PathAddress newFileStoreAddr = timerServiceAddr.append(PathElement.pathElement(EJB3SubsystemModel.FILE_DATA_STORE, "file-data-store"));
            config.addFailedAttribute(badFileStoreAddr, new ChangeAddressConfig(services, badFileStoreAddr, newFileStoreAddr));

            // reject the resource /subsystem=ejb3/service=timer-service/file-data-store=file-data-store-rejected since we already have a file-data-store
            config.addFailedAttribute(subsystemAddress.append(EJB3SubsystemModel.TIMER_SERVICE_PATH, PathElement.pathElement(EJB3SubsystemModel.FILE_DATA_STORE, "file-data-store-rejected")),
                    FailedOperationTransformationConfig.REJECTED_RESOURCE);

            // reject the resource /subsystem=ejb3/service=timer-service/database-data-store=*
            PathAddress databaseDataStore = subsystemAddress.append(EJB3SubsystemModel.TIMER_SERVICE_PATH, EJB3SubsystemModel.DATABASE_DATA_STORE_PATH);
            config.addFailedAttribute(databaseDataStore, FailedOperationTransformationConfig.REJECTED_RESOURCE);

            // reject the resource /subsystem=ejb3/mdb-delivery-group=delivery-group-name
            config.addFailedAttribute(subsystemAddress.append(PathElement.pathElement(EJB3SubsystemModel.MDB_DELIVERY_GROUP, "delivery-group-name")), FailedOperationTransformationConfig.REJECTED_RESOURCE);

            // reject the resource /subsystem=ejb3/remoting-profile=profile and its children
            PathAddress remotingProfileAddress = subsystemAddress.append(PathElement.pathElement(EJB3SubsystemModel.REMOTING_PROFILE, "profile"));
            PathAddress ejbReceiverAddress = remotingProfileAddress.append(PathElement.pathElement(EJB3SubsystemModel.REMOTING_EJB_RECEIVER, "receiver"));
            PathAddress channelCreationOptionsAddress = ejbReceiverAddress.append(PathElement.pathElement(EJB3SubsystemModel.CHANNEL_CREATION_OPTIONS));
            config.addFailedAttribute(remotingProfileAddress, FailedOperationTransformationConfig.REJECTED_RESOURCE);
            config.addFailedAttribute(ejbReceiverAddress, FailedOperationTransformationConfig.REJECTED_RESOURCE);
            config.addFailedAttribute(channelCreationOptionsAddress, FailedOperationTransformationConfig.REJECTED_RESOURCE);

            // reject the attribute 'cluster' from resource /subsystem=ejb3/service=remote
            config.addFailedAttribute(subsystemAddress.append(EJB3SubsystemModel.REMOTE_SERVICE_PATH), new FailedOperationTransformationConfig.NewAttributesConfig(EJB3RemoteResourceDefinition.CLIENT_MAPPINGS_CLUSTER_NAME));

            // reject the resource /subsystem=ejb3/application-security-domain=domain
            config.addFailedAttribute(subsystemAddress.append(PathElement.pathElement(EJB3SubsystemModel.APPLICATION_SECURITY_DOMAIN, "domain")), FailedOperationTransformationConfig.REJECTED_RESOURCE);

            // reject the resource /subsystem=ejb3/service=identity
            config.addFailedAttribute(subsystemAddress.append(EJB3SubsystemModel.IDENTITY_PATH), FailedOperationTransformationConfig.REJECTED_RESOURCE);

        }

        if (EJB3Model.VERSION_1_3_0.matches(version)) {

            // create a chained config to apply multiple transformation configs to each one of a collection of attributes
            FailedOperationTransformationConfig.ChainedConfig chainedConfig = FailedOperationTransformationConfig.ChainedConfig.createBuilder(
                    /*EJB3SubsystemRootResourceDefinition.DEFAULT_SFSB_PASSIVATION_DISABLED_CACHE,*/ EJB3SubsystemRootResourceDefinition.DISABLE_DEFAULT_EJB_PERMISSIONS)
                    .addConfig(new FailedOperationTransformationConfig.NewAttributesConfig(
                            /*EJB3SubsystemRootResourceDefinition.DEFAULT_SFSB_PASSIVATION_DISABLED_CACHE,*/ EJB3SubsystemRootResourceDefinition.LOG_EJB_EXCEPTIONS))
                    .addConfig(new CorrectFalseToTrue(EJB3SubsystemRootResourceDefinition.DISABLE_DEFAULT_EJB_PERMISSIONS))
                    .build();

            // discard new attributes default-sfsb-passivation-disabled-cache, disable-default-ejb-permissions
            config.addFailedAttribute(subsystemAddress, chainedConfig);

            // reject the attributes allow execution, refresh interval from resource /subsystem=ejb3/service=timer-service/database-data-store=*
            PathAddress databaseDataStore = subsystemAddress.append(EJB3SubsystemModel.TIMER_SERVICE_PATH, EJB3SubsystemModel.DATABASE_DATA_STORE_PATH);
            // config.addFailedAttribute(databaseDataStore, new FailedOperationTransformationConfig.NewAttributesConfig(DatabaseDataStoreResourceDefinition.ALLOW_EXECUTION, DatabaseDataStoreResourceDefinition.REFRESH_INTERVAL));
            config.addFailedAttribute(databaseDataStore, FailedOperationTransformationConfig.REJECTED_RESOURCE);

            // reject the resource /subsystem=ejb3/mdb-delivery-group=delivery-group-name
            config.addFailedAttribute(subsystemAddress.append(PathElement.pathElement(EJB3SubsystemModel.MDB_DELIVERY_GROUP, "delivery-group-name")), FailedOperationTransformationConfig.REJECTED_RESOURCE);

            // reject the resource /subsystem=ejb3/remoting-profile=profile and its children
            PathAddress remotingProfileAddress = subsystemAddress.append(PathElement.pathElement(EJB3SubsystemModel.REMOTING_PROFILE, "profile"));
            PathAddress ejbReceiverAddress = remotingProfileAddress.append(PathElement.pathElement(EJB3SubsystemModel.REMOTING_EJB_RECEIVER, "receiver"));
            PathAddress channelCreationOptionsAddress = ejbReceiverAddress.append(PathElement.pathElement(EJB3SubsystemModel.CHANNEL_CREATION_OPTIONS));
            config.addFailedAttribute(remotingProfileAddress, FailedOperationTransformationConfig.REJECTED_RESOURCE);
            config.addFailedAttribute(ejbReceiverAddress, FailedOperationTransformationConfig.REJECTED_RESOURCE);
            config.addFailedAttribute(channelCreationOptionsAddress, FailedOperationTransformationConfig.REJECTED_RESOURCE);

            // reject the attribute 'cluster' from resource /subsystem=ejb3/service=remote
            config.addFailedAttribute(subsystemAddress.append(EJB3SubsystemModel.REMOTE_SERVICE_PATH), new FailedOperationTransformationConfig.NewAttributesConfig(EJB3RemoteResourceDefinition.CLIENT_MAPPINGS_CLUSTER_NAME));

            // reject the resource /subsystem=ejb3/application-security-domain=domain
            config.addFailedAttribute(subsystemAddress.append(PathElement.pathElement(EJB3SubsystemModel.APPLICATION_SECURITY_DOMAIN, "domain")), FailedOperationTransformationConfig.REJECTED_RESOURCE);

            // reject the resource /subsystem=ejb3/service=identity
            config.addFailedAttribute(subsystemAddress.append(EJB3SubsystemModel.IDENTITY_PATH), FailedOperationTransformationConfig.REJECTED_RESOURCE);
        }

        return config;
    }

    private static class CorrectFalseToTrue extends FailedOperationTransformationConfig.AttributesPathAddressConfig<CorrectFalseToTrue>{

        public CorrectFalseToTrue(AttributeDefinition...defs) {
            super(convert(defs));
        }

        @Override
        protected boolean isAttributeWritable(String attributeName) {
            return true;
        }

        @Override
        protected boolean checkValue(String attrName, ModelNode attribute, boolean isWriteAttribute) {
            return attribute.asString().equals("true");
        }

        @Override
        protected ModelNode correctValue(ModelNode toResolve, boolean isWriteAttribute) {
            return new ModelNode(false);
        }

    }

    private static class ChangeAddressConfig implements FailedOperationTransformationConfig.PathAddressConfig {
        KernelServices services;
        private final PathAddress badAddress;
        private final PathAddress newAddress;

        private ChangeAddressConfig(KernelServices services, PathAddress badAddress, PathAddress newAddress) {
            this.services = services;
            this.badAddress = badAddress;
            this.newAddress = newAddress;
        }

        @Override
        public boolean expectFailed(ModelNode operation) {
            return isBadAddress(operation);
        }

        @Override
        public boolean canCorrectMore(ModelNode operation) {
            return isBadAddress(operation);
        }

        @Override
        public ModelNode correctOperation(ModelNode operation) {
            //As part of this we also need to update the main model, since the transformer will look at the
            //values already in the model in order to know what to reject. We basically move the
            //resource found at badAddress to newAddress
            try {
                ModelNode ds = services.executeForResult(Util.createEmptyOperation(READ_RESOURCE_OPERATION, badAddress));
                ModelTestUtils.checkOutcome(services.executeOperation(Util.createRemoveOperation(badAddress)));
                ds.get(OP).set(ADD);
                ds.get(OP_ADDR).set(newAddress.toModelNode());
                ModelTestUtils.checkOutcome(services.executeOperation((ds)));
            } catch (OperationFailedException e) {
                throw new RuntimeException(e);
            }

            //Now fix up the operation as normal
            ModelNode op = operation.clone();
            op.get(OP_ADDR).set(newAddress.toModelNode());
            return op;
        }

        @Override
        public boolean expectDiscarded(ModelNode operation) {
            //The reject simply forwards on the original operation to make it fail
            return false;
        }

        @Override
        public List<ModelNode> createWriteAttributeOperations(ModelNode operation) {
            return Collections.emptyList();
        }

        @Override
        public boolean expectFailedWriteAttributeOperation(ModelNode operation) {
            throw new IllegalStateException("Should not get called");
        }

        @Override
        public ModelNode correctWriteAttributeOperation(ModelNode operation) {
            throw new IllegalStateException("Should not get called");
        }

        private boolean isBadAddress(ModelNode operation) {
            return PathAddress.pathAddress(operation.require(OP_ADDR)).equals(badAddress);
        }
    }
}
