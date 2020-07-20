package org.jboss.as.ejb3.subsystem;

import static org.jboss.as.controller.capability.RuntimeCapability.buildDynamicCapabilityName;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.ejb3.subsystem.EJB3Model.VERSION_1_3_0;
import static org.jboss.as.ejb3.subsystem.EJB3Model.VERSION_4_0_0;
import static org.jboss.as.ejb3.subsystem.EJB3Model.VERSION_5_0_0;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.STRICT_MAX_BEAN_INSTANCE_POOL;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.STATISTICS_ENABLED;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.ENABLE_STATISTICS;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelFixer;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.as.threads.PoolAttributeDefinitions;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

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
        return "schema/wildfly-ejb3_8_0.xsd";
    }

    @Override
    protected String[] getSubsystemTemplatePaths() throws IOException {
        return new String[]{
                "/subsystem-templates/ejb3.xml"
        };
    }

    @Test
    @Override
    public void testSchemaOfSubsystemTemplates() throws Exception {
        super.testSchemaOfSubsystemTemplates();
    }

    /*
     * Transformer tests not involving rejection
     */

    @Test
    public void testTransformerEAP640() throws Exception {
        ModelTestControllerVersion controller = ModelTestControllerVersion.EAP_6_4_0;

        testTransformation(VERSION_1_3_0.getVersion(), controller,
                formatLegacySubsystemArtifact(controller),
                formatArtifact("org.jboss.as:jboss-as-threads:%s", controller),
                "org.jboss.spec.javax.rmi:jboss-rmi-api_1.0_spec:1.0.4.Final-redhat-3");
    }

    @Test
    public void testTransformerEAP647() throws Exception {
        ModelTestControllerVersion controller = ModelTestControllerVersion.EAP_6_4_7;

        testTransformation(VERSION_1_3_0.getVersion(), controller,
                formatLegacySubsystemArtifact(controller),
                formatArtifact("org.jboss.as:jboss-as-threads:%s", controller),
                "org.jboss.spec.javax.rmi:jboss-rmi-api_1.0_spec:1.0.4.Final-redhat-3");
    }

    @Test
    public void testTransformerEAP700() throws Exception {
        ModelTestControllerVersion controller = ModelTestControllerVersion.EAP_7_0_0;

        testTransformation(VERSION_4_0_0.getVersion(), controller,
                formatEAPSubsystemArtifact(controller),
                "org.jboss.eap:wildfly-clustering-ejb-spi:7.0.0.GA-redhat-2",
                "org.wildfly.core:wildfly-threads:2.1.2.Final-redhat-1",
                "org.jboss.spec.javax.rmi:jboss-rmi-api_1.0_spec:1.0.4.Final-redhat-4");
    }

    @Test
    public void testTransformerEAP710() throws Exception {
        ModelTestControllerVersion controller = ModelTestControllerVersion.EAP_7_1_0;

        testTransformation(VERSION_5_0_0.getVersion(), controller,
                formatEAPSubsystemArtifact(controller),
                "org.jboss.eap:wildfly-clustering-ejb-spi:7.1.0.GA-redhat-11",
                "org.wildfly.core:wildfly-threads:3.0.10.Final-redhat-1",
                "org.jboss.spec.javax.rmi:jboss-rmi-api_1.0_spec:1.0.5.Final-redhat-1");
    }

    @Test
    public void testTransformerEAP720() throws Exception {
        ModelTestControllerVersion controller = ModelTestControllerVersion.EAP_7_2_0;

        testTransformation(VERSION_5_0_0.getVersion(), controller,
                formatEAPSubsystemArtifact(controller),
                "org.wildfly.core:wildfly-threads:6.0.11.Final-redhat-00001",
                "org.jboss.spec.javax.rmi:jboss-rmi-api_1.0_spec:1.0.6.Final-redhat-1");
    }

    /*
     * Transformer tests involving rejection
     */

    @Test
    public void testRejectionsEAP640() throws Exception {
        ModelTestControllerVersion controller = ModelTestControllerVersion.EAP_6_4_0;

        this.testRejections(VERSION_1_3_0.getVersion(), controller,
                formatLegacySubsystemArtifact(controller),
                LEGACY_EJB_CLIENT_ARTIFACT,
                formatArtifact("org.jboss.as:jboss-as-threads:%s", controller),
                "org.jboss.spec.javax.rmi:jboss-rmi-api_1.0_spec:1.0.4.Final-redhat-3");
    }

    @Test
    public void testRejectionsEAP647() throws Exception {
        ModelTestControllerVersion controller = ModelTestControllerVersion.EAP_6_4_7;

        this.testRejections(VERSION_1_3_0.getVersion(), controller,
                formatLegacySubsystemArtifact(controller),
                formatArtifact("org.jboss.as:jboss-as-threads:%s", controller),
                LEGACY_EJB_CLIENT_ARTIFACT,
                "org.jboss.spec.javax.rmi:jboss-rmi-api_1.0_spec:1.0.4.Final-redhat-3");
    }

    @Test
    public void testRejectionsEAP700() throws Exception {
        ModelTestControllerVersion controller = ModelTestControllerVersion.EAP_7_0_0;

        this.testRejections(VERSION_4_0_0.getVersion(), controller,
                formatEAPSubsystemArtifact(controller),
                "org.jboss.eap:wildfly-clustering-ejb-spi:7.0.0.GA-redhat-2",
                "org.wildfly.core:wildfly-threads:2.1.2.Final-redhat-1",
                LEGACY_EJB_CLIENT_ARTIFACT,
                "org.jboss.spec.javax.rmi:jboss-rmi-api_1.0_spec:1.0.4.Final-redhat-4");
    }

    @Test
    public void testRejectionsEAP710() throws Exception {
        ModelTestControllerVersion controller = ModelTestControllerVersion.EAP_7_1_0;

        this.testRejections(VERSION_5_0_0.getVersion(), controller,
                formatEAPSubsystemArtifact(controller),
                "org.jboss.eap:wildfly-clustering-ejb-spi:7.1.0.GA-redhat-11",
                "org.wildfly.core:wildfly-threads:3.0.10.Final-redhat-1",
                "org.jboss.spec.javax.rmi:jboss-rmi-api_1.0_spec:1.0.5.Final-redhat-1");
    }

    @Test
    public void testRejectionsEAP720() throws Exception {
        ModelTestControllerVersion controller = ModelTestControllerVersion.EAP_7_2_0;

        this.testRejections(VERSION_5_0_0.getVersion(), controller,
                formatEAPSubsystemArtifact(controller),
                "org.wildfly.core:wildfly-threads:6.0.11.Final-redhat-00001",
                "org.jboss.spec.javax.rmi:jboss-rmi-api_1.0_spec:1.0.6.Final-redhat-1");
    }


    /*
     * Add in required capabilities for external subsystem resources the ejb3 subsystem accesses
     */
    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.withCapabilities("org.wildfly.transactions.global-default-local-provider",
                buildDynamicCapabilityName("org.wildfly.security.security-domain", "ApplicationDomain"),
                "org.wildfly.remoting.connector.http-remoting-connector",
                "org.wildfly.remoting.connector.remoting-connector");
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

        if (EJB3Model.VERSION_1_2_1.matches(model) || EJB3Model.VERSION_1_3_0.matches(model)) {
            PathAddress ejb3PathAddress = PathAddress.pathAddress(SUBSYSTEM, EJB3Extension.SUBSYSTEM_NAME);
            Map<String, String> attributeRenames = new HashMap<>();
            attributeRenames.put(EJB3SubsystemRootResourceDefinition.DEFAULT_SFSB_CACHE.getName(), EJB3SubsystemRootResourceDefinition.DEFAULT_CLUSTERED_SFSB_CACHE.getName());
            attributeRenames.put(EJB3SubsystemRootResourceDefinition.DEFAULT_SFSB_PASSIVATION_DISABLED_CACHE.getName(), EJB3SubsystemRootResourceDefinition.DEFAULT_SFSB_CACHE.getName());

            for (Map.Entry<String, String> renames : attributeRenames.entrySet()) {
                ModelNode operation = Util.getWriteAttributeOperation(ejb3PathAddress, renames.getKey(), "test");
                testAttributeRenameTransform(model, services, legacyServices, operation, renames.getKey(), renames.getValue());

                operation = Util.getReadAttributeOperation(ejb3PathAddress, renames.getKey());
                testAttributeRenameTransform(model, services, legacyServices, operation, renames.getKey(), renames.getValue());

                operation = Util.getUndefineAttributeOperation(ejb3PathAddress, renames.getKey());
                testAttributeRenameTransform(model, services, legacyServices, operation, renames.getKey(), renames.getValue());
            }
        }
    }

    private void testRejections(ModelVersion model, ModelTestControllerVersion controller, String... mavenResourceURLs) throws Exception {
        // create builder for current subsystem version
        KernelServicesBuilder builder = createKernelServicesBuilder(this.createAdditionalInitialization());

        // initialize the legacy services and add required jars
        builder.createLegacyKernelServicesBuilder(this.createAdditionalInitialization(), controller, model)
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

        // EAP 6.4.0
        if (EJB3Model.VERSION_1_2_1.matches(version)) {

            // register rejections for changes in 3.0.0

            // register rejections for changes in 4.0.0
            // reject the resource /subsystem=ejb3/mdb-delivery-group=delivery-group-name
            config.addFailedAttribute(subsystemAddress.append(PathElement.pathElement(EJB3SubsystemModel.MDB_DELIVERY_GROUP, "delivery-group-name")), FailedOperationTransformationConfig.REJECTED_RESOURCE);

            // register rejections for changes in 5.0.0

            // reject the resource /subsystem=ejb3/application-security-domain=domain
            config.addFailedAttribute(subsystemAddress.append(PathElement.pathElement(EJB3SubsystemModel.APPLICATION_SECURITY_DOMAIN, "domain")), FailedOperationTransformationConfig.REJECTED_RESOURCE);
            // reject the resource /subsystem=ejb3/service=identity
            config.addFailedAttribute(subsystemAddress.append(EJB3SubsystemModel.IDENTITY_PATH), FailedOperationTransformationConfig.REJECTED_RESOURCE);
            // reject the resource /subsystem=ejb3/remoting-profile=profile and its children
            PathAddress remotingProfileAddress = subsystemAddress.append(PathElement.pathElement(EJB3SubsystemModel.REMOTING_PROFILE, "profile"));
            PathAddress ejbReceiverAddress = remotingProfileAddress.append(PathElement.pathElement(EJB3SubsystemModel.REMOTING_EJB_RECEIVER, "receiver"));
            PathAddress channelCreationOptionsAddress = ejbReceiverAddress.append(PathElement.pathElement(EJB3SubsystemModel.CHANNEL_CREATION_OPTIONS));
            config.addFailedAttribute(remotingProfileAddress, FailedOperationTransformationConfig.REJECTED_RESOURCE);
            config.addFailedAttribute(ejbReceiverAddress, FailedOperationTransformationConfig.REJECTED_RESOURCE);
            config.addFailedAttribute(channelCreationOptionsAddress, FailedOperationTransformationConfig.REJECTED_RESOURCE);

            // register rejections for changes in 6.0.0

            // reject the attribute core-threads from resource /subsystem=ejb3/thread-pool=default
            config.addFailedAttribute(subsystemAddress.append(EJB3SubsystemModel.THREAD_POOL_PATH), new FailedOperationTransformationConfig.NewAttributesConfig(PoolAttributeDefinitions.CORE_THREADS));

            // register rejections for changes in 7.0.0

            // create a chained config to apply multiple transformation configs to each one of a collection of attributes
            FailedOperationTransformationConfig.ChainedConfig chainedSubsystemConfig = FailedOperationTransformationConfig.ChainedConfig.createBuilder(
                    /*EJB3SubsystemRootResourceDefinition.DEFAULT_SFSB_PASSIVATION_DISABLED_CACHE,*/ EJB3SubsystemRootResourceDefinition.DISABLE_DEFAULT_EJB_PERMISSIONS)
                    .addConfig(new FailedOperationTransformationConfig.NewAttributesConfig(
                            /*EJB3SubsystemRootResourceDefinition.DEFAULT_SFSB_PASSIVATION_DISABLED_CACHE,*/ EJB3SubsystemRootResourceDefinition.LOG_EJB_EXCEPTIONS,
                            EJB3SubsystemRootResourceDefinition.ALLOW_EJB_NAME_REGEX, EJB3SubsystemRootResourceDefinition.ENABLE_GRACEFUL_TXN_SHUTDOWN,
                            EJB3SubsystemRootResourceDefinition.SERVER_INTERCEPTORS, EJB3SubsystemRootResourceDefinition.CLIENT_INTERCEPTORS,
                            EJB3SubsystemRootResourceDefinition.DEFAULT_STATEFUL_BEAN_SESSION_TIMEOUT))
                    .addConfig(new CorrectFalseToTrue(EJB3SubsystemRootResourceDefinition.DISABLE_DEFAULT_EJB_PERMISSIONS))
                    .build();

            // discard new attributes default-sfsb-passivation-disabled-cache, disable-default-ejb-permissions
            config.addFailedAttribute(subsystemAddress, chainedSubsystemConfig);

            // make sure that we have a file-data-store matching the custom-data-store attribute value
            final PathAddress timerServiceAddr = subsystemAddress.append(EJB3SubsystemModel.TIMER_SERVICE_PATH);
            final PathAddress badFileStoreAddr = timerServiceAddr.append(PathElement.pathElement(EJB3SubsystemModel.FILE_DATA_STORE, "file-data-store-rename-to-default"));
            final PathAddress newFileStoreAddr = timerServiceAddr.append(PathElement.pathElement(EJB3SubsystemModel.FILE_DATA_STORE, "file-data-store"));
            config.addFailedAttribute(badFileStoreAddr, new ChangeAddressConfig(services, badFileStoreAddr, newFileStoreAddr));

            //Add a config to remove the extra file-data-store. This is against the fixed address from ChangeAddressConfig
            RemoveExtraFileStoreConfig removeExtraFileStoreConfig = new RemoveExtraFileStoreConfig(services, timerServiceAddr);
            config.addFailedAttribute(newFileStoreAddr, removeExtraFileStoreConfig);

            // reject the resource /subsystem=ejb3/service=timer-service/file-data-store=file-data-store-rejected since we already have a file-data-store
            config.addFailedAttribute(subsystemAddress.append(EJB3SubsystemModel.TIMER_SERVICE_PATH, PathElement.pathElement(EJB3SubsystemModel.FILE_DATA_STORE, "file-data-store-rejected")),
                    FailedOperationTransformationConfig.REJECTED_RESOURCE);

            // reject the resource /subsystem=ejb3/service=timer-service/database-data-store=*
            PathAddress databaseDataStore = subsystemAddress.append(EJB3SubsystemModel.TIMER_SERVICE_PATH, EJB3SubsystemModel.DATABASE_DATA_STORE_PATH);
            config.addFailedAttribute(databaseDataStore, FailedOperationTransformationConfig.REJECTED_RESOURCE);

            // reject the attribute 'cluster' from resource /subsystem=ejb3/service=remote
            config.addFailedAttribute(subsystemAddress.append(EJB3SubsystemModel.REMOTE_SERVICE_PATH),
                    new FailedOperationTransformationConfig.NewAttributesConfig(EJB3RemoteResourceDefinition.CLIENT_MAPPINGS_CLUSTER_NAME, EJB3RemoteResourceDefinition.CONNECTORS));

            //Special handling for this test!!!!
            //Don't transform the resulting composite, instead rather transform the individual steps
            config.setDontTransformComposite();

            //Remove the extra file-data-store entries so that our transformers can work
            config.setCallback(() -> removeExtraFileStoreConfig.removeExtraFileDataStore());
        }

        // EAP 6.4.7
        if (EJB3Model.VERSION_1_3_0.matches(version)) {

            // create a chained config to apply multiple transformation configs to each one of a collection of attributes
            FailedOperationTransformationConfig.ChainedConfig chainedConfig = FailedOperationTransformationConfig.ChainedConfig.createBuilder(
                    /*EJB3SubsystemRootResourceDefinition.DEFAULT_SFSB_PASSIVATION_DISABLED_CACHE,*/ EJB3SubsystemRootResourceDefinition.DISABLE_DEFAULT_EJB_PERMISSIONS)
                    .addConfig(new FailedOperationTransformationConfig.NewAttributesConfig(
                            /*EJB3SubsystemRootResourceDefinition.DEFAULT_SFSB_PASSIVATION_DISABLED_CACHE,*/ EJB3SubsystemRootResourceDefinition.LOG_EJB_EXCEPTIONS,
                            EJB3SubsystemRootResourceDefinition.ALLOW_EJB_NAME_REGEX, EJB3SubsystemRootResourceDefinition.ENABLE_GRACEFUL_TXN_SHUTDOWN,
                            EJB3SubsystemRootResourceDefinition.SERVER_INTERCEPTORS, EJB3SubsystemRootResourceDefinition.CLIENT_INTERCEPTORS,
                            EJB3SubsystemRootResourceDefinition.DEFAULT_STATEFUL_BEAN_SESSION_TIMEOUT))
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
            config.addFailedAttribute(subsystemAddress.append(EJB3SubsystemModel.REMOTE_SERVICE_PATH),
                    new FailedOperationTransformationConfig.NewAttributesConfig(EJB3RemoteResourceDefinition.CLIENT_MAPPINGS_CLUSTER_NAME, EJB3RemoteResourceDefinition.CONNECTORS));

            // reject the resource /subsystem=ejb3/application-security-domain=domain
            config.addFailedAttribute(subsystemAddress.append(PathElement.pathElement(EJB3SubsystemModel.APPLICATION_SECURITY_DOMAIN, "domain")), FailedOperationTransformationConfig.REJECTED_RESOURCE);

            // reject the resource /subsystem=ejb3/service=identity
            config.addFailedAttribute(subsystemAddress.append(EJB3SubsystemModel.IDENTITY_PATH), FailedOperationTransformationConfig.REJECTED_RESOURCE);

            // reject the attribute core-threads from resource /subsystem=ejb3/thread-pool=default
            config.addFailedAttribute(subsystemAddress.append(EJB3SubsystemModel.THREAD_POOL_PATH), new FailedOperationTransformationConfig.NewAttributesConfig(PoolAttributeDefinitions.CORE_THREADS));
        }

        // need to include all changes from current to 4.0.0
        if (EJB3Model.VERSION_4_0_0.matches(version)) {

            config.addFailedAttribute(subsystemAddress,
                    new FailedOperationTransformationConfig.NewAttributesConfig(
                            EJB3SubsystemRootResourceDefinition.CLIENT_INTERCEPTORS,
                            EJB3SubsystemRootResourceDefinition.SERVER_INTERCEPTORS,
                            EJB3SubsystemRootResourceDefinition.ENABLE_GRACEFUL_TXN_SHUTDOWN,
                            EJB3SubsystemRootResourceDefinition.DEFAULT_STATEFUL_BEAN_SESSION_TIMEOUT
                            )
            );

            // reject the attribute core-threads from resource /subsystem=ejb3/thread-pool=default
            config.addFailedAttribute(subsystemAddress.append(EJB3SubsystemModel.THREAD_POOL_PATH), new FailedOperationTransformationConfig.NewAttributesConfig(PoolAttributeDefinitions.CORE_THREADS));

            // reject the resource /subsystem=ejb3/application-security-domain=domain
            config.addFailedAttribute(subsystemAddress.append(PathElement.pathElement(EJB3SubsystemModel.APPLICATION_SECURITY_DOMAIN, "domain")), FailedOperationTransformationConfig.REJECTED_RESOURCE);

            // reject the resource /subsystem=ejb3/service=identity
            config.addFailedAttribute(subsystemAddress.append(EJB3SubsystemModel.IDENTITY_PATH), FailedOperationTransformationConfig.REJECTED_RESOURCE);

            // reject the attribute 'connectors' from resource /subsystem=ejb3/service=remote
            config.addFailedAttribute(subsystemAddress.append(EJB3SubsystemModel.REMOTE_SERVICE_PATH),
                    new FailedOperationTransformationConfig.NewAttributesConfig(EJB3RemoteResourceDefinition.CONNECTORS));

        }

        // need to include all changes from current to 5.0.0
        if (EJB3Model.VERSION_5_0_0.matches(version)) {

            config.addFailedAttribute(subsystemAddress,
                    new FailedOperationTransformationConfig.NewAttributesConfig(EJB3SubsystemRootResourceDefinition.CLIENT_INTERCEPTORS,
                            EJB3SubsystemRootResourceDefinition.SERVER_INTERCEPTORS,
                            EJB3SubsystemRootResourceDefinition.DEFAULT_STATEFUL_BEAN_SESSION_TIMEOUT));

            // reject the attribute core-threads from resource /subsystem=ejb3/thread-pool=default
            config.addFailedAttribute(subsystemAddress.append(EJB3SubsystemModel.THREAD_POOL_PATH), new FailedOperationTransformationConfig.NewAttributesConfig(PoolAttributeDefinitions.CORE_THREADS));

            // reject the attribute 'connectors' from resource /subsystem=ejb3/service=remote
            config.addFailedAttribute(subsystemAddress.append(EJB3SubsystemModel.REMOTE_SERVICE_PATH),
                    new FailedOperationTransformationConfig.NewAttributesConfig(EJB3RemoteResourceDefinition.CONNECTORS));
        }

        // need to include all changes from current to 6.0.0
        if (EJB3Model.VERSION_6_0_0.matches(version)) {
            config.addFailedAttribute(subsystemAddress, new FailedOperationTransformationConfig.NewAttributesConfig(EJB3SubsystemRootResourceDefinition.DEFAULT_STATEFUL_BEAN_SESSION_TIMEOUT));

            // reject the attribute 'connectors' from resource /subsystem=ejb3/service=remote
            config.addFailedAttribute(subsystemAddress.append(EJB3SubsystemModel.REMOTE_SERVICE_PATH),
                    new FailedOperationTransformationConfig.NewAttributesConfig(EJB3RemoteResourceDefinition.CONNECTORS));
        }

        // need to include all changes from current to .0.0
        if (EJB3Model.VERSION_7_0_0.matches(version)) {

            // reject the attribute 'connectors' from resource /subsystem=ejb3/service=remote
            config.addFailedAttribute(subsystemAddress.append(EJB3SubsystemModel.REMOTE_SERVICE_PATH),
                    new FailedOperationTransformationConfig.NewAttributesConfig(EJB3RemoteResourceDefinition.CONNECTORS));
        }

        return config;
    }


    /*
     * Class to fix up some irredeemable errors in the legacy model before comparison
     */
    private static class DefaultModelFixer implements ModelFixer {

        private final String SLSB_STRICT_MAX_POOL = "slsb-strict-max-pool";
        private final String MDB_STRICT_MAX_POOL = "mdb-strict-max-pool";

        @Override
        public ModelNode fixModel(ModelNode modelNode) {
            // /subsystem=ejb3/strict-max-pool=* attribute derive-size has a default in legacy but none in current, which fails the model comparison

            if (modelNode.hasDefined(STRICT_MAX_BEAN_INSTANCE_POOL) && modelNode.get(STRICT_MAX_BEAN_INSTANCE_POOL).hasDefined(MDB_STRICT_MAX_POOL)) {
                modelNode.get(STRICT_MAX_BEAN_INSTANCE_POOL, MDB_STRICT_MAX_POOL, StrictMaxPoolResourceDefinition.DERIVE_SIZE.getName()).set(new ModelNode());
            }
            if (modelNode.hasDefined(STRICT_MAX_BEAN_INSTANCE_POOL) && modelNode.get(STRICT_MAX_BEAN_INSTANCE_POOL).hasDefined(SLSB_STRICT_MAX_POOL)) {
                modelNode.get(STRICT_MAX_BEAN_INSTANCE_POOL, SLSB_STRICT_MAX_POOL, StrictMaxPoolResourceDefinition.DERIVE_SIZE.getName()).set(new ModelNode());
            }

            if (modelNode.hasDefined(STATISTICS_ENABLED) && modelNode.hasDefined(ENABLE_STATISTICS)
                                                         && modelNode.get(STATISTICS_ENABLED).equals(modelNode.get(ENABLE_STATISTICS))) {
                // In a legacy kernel before WFCORE-4183 a read-resource result incorrectly includes a value for both an attribute and its alias. Correct that
                modelNode.get(ENABLE_STATISTICS).set(new ModelNode());
            }
            return modelNode;
        }
    }

    private static class CorrectFalseToTrue extends FailedOperationTransformationConfig.AttributesPathAddressConfig<CorrectFalseToTrue> {

        public CorrectFalseToTrue(AttributeDefinition... defs) {
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
            return ModelNode.FALSE;
        }

    }

    private abstract static class BasePathAddressConfig implements FailedOperationTransformationConfig.PathAddressConfig {

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
    }

    private static class ChangeAddressConfig extends BasePathAddressConfig {
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

        private boolean isBadAddress(ModelNode operation) {
            return PathAddress.pathAddress(operation.require(OP_ADDR)).equals(badAddress);
        }
    }

    private static class RemoveExtraFileStoreConfig extends BasePathAddressConfig {
        private final KernelServices kernelServices;
        private final PathAddress timerServiceAddress;
        private final PathAddress rejectedFileDataStoreAddress;
        private ModelNode removedResourceModel;

        public RemoveExtraFileStoreConfig(KernelServices kernelServices, PathAddress timerServiceAddress) {
            this.kernelServices = kernelServices;
            this.timerServiceAddress = timerServiceAddress;
            rejectedFileDataStoreAddress = timerServiceAddress.append(EJB3SubsystemModel.FILE_DATA_STORE_PATH.getKey(), "file-data-store-rejected");
        }

        @Override
        public boolean expectFailed(ModelNode operation) {
            return hasTooManyFileStores();
        }

        @Override
        public boolean canCorrectMore(ModelNode operation) {
            return hasTooManyFileStores();
        }

        private boolean hasTooManyFileStores() {
            ModelNode op = Util.createOperation(READ_CHILDREN_NAMES_OPERATION, timerServiceAddress);
            op.get(CHILD_TYPE).set(EJB3SubsystemModel.FILE_DATA_STORE_PATH.getKey());
            ModelNode result = ModelTestUtils.checkOutcome(kernelServices.executeOperation(op)).get(RESULT);
            List<ModelNode> list = result.asList();
            return list.size() > 1;
        }

        @Override
        public ModelNode correctOperation(ModelNode operation) {
            //Here we don't actually correct the operation, but we remove the extra file-data-store which causes the
            //rejection


            ModelNode rr = Util.createEmptyOperation(READ_RESOURCE_OPERATION, rejectedFileDataStoreAddress);
            removedResourceModel = ModelTestUtils.checkOutcome(kernelServices.executeOperation(rr)).get(RESULT);

            removeExtraFileDataStore();
            return operation;
        }

        void removeExtraFileDataStore() {
            ModelNode remove = Util.createRemoveOperation(rejectedFileDataStoreAddress);
            ModelTestUtils.checkOutcome(kernelServices.executeOperation(remove));
        }

        @Override
        public void operationDone(ModelNode operation) {
            if (removedResourceModel != null) {
                //Re-add the removed resource, since we have more checks in the config for file-data-store=file-data-store-rejected
                ModelNode add = Util.createAddOperation(
                        timerServiceAddress.append(EJB3SubsystemModel.FILE_DATA_STORE_PATH.getKey(), "file-data-store-rejected"));
                for (String key : removedResourceModel.keys()) {
                    add.get(key).set(removedResourceModel.get(key));
                }
                ModelNode result = ModelTestUtils.checkOutcome(kernelServices.executeOperation(add)).get(RESULT);
            }
        }
    }

    private void testAttributeRenameTransform(ModelVersion modelVersion, KernelServices mainServices, KernelServices legacyServices, ModelNode operation, String attributeName, String legacyAttributeName) {
        OperationTransformer.TransformedOperation op = mainServices.executeInMainAndGetTheTransformedOperation(operation, modelVersion);
        if (op.getTransformedOperation() != null) {
            ModelTestUtils.checkOutcome(mainServices.getLegacyServices(modelVersion).executeOperation(op.getTransformedOperation()));
        }

        ModelNode mainModel = mainServices.readWholeModel();
        ModelNode legacyModel = legacyServices.readWholeModel();

        ModelNode mainEjbSubsystem = mainModel.get(SUBSYSTEM, EJB3Extension.SUBSYSTEM_NAME);
        ModelNode legacyEjbSubsystem = legacyModel.get(SUBSYSTEM, EJB3Extension.SUBSYSTEM_NAME);

        Assert.assertEquals(mainEjbSubsystem.get(attributeName), legacyEjbSubsystem.get(legacyAttributeName));
    }
}
