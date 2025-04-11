/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jboss.as.clustering.controller.CommonServiceDescriptor;
import org.jboss.as.clustering.infinispan.subsystem.remote.RemoteCacheContainerResourceDefinition;
import org.jboss.as.clustering.subsystem.AdditionalInitialization;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.model.test.FailedOperationTransformationConfig;
import org.jboss.as.model.test.ModelFixer;
import org.jboss.as.model.test.ModelTestControllerVersion;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jgroups.conf.ClassConfigurator;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.wildfly.clustering.jgroups.spi.ForkChannelFactory;

/**
 * Test cases for transformers used in the Infinispan subsystem
 *
 * Here we perform two types of tests:
 * - testing transformation between the current model and legacy models, in the case
 * where no rejections are expected, but certain discards/conversions/renames are expected
 * - testing transformation between the current model and legacy models, in the case
 * where specific rejections are expected
 *
 * @author <a href="tomaz.cerar@redhat.com">Tomaz Cerar</a>
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 * @author Radoslav Husar
 */
@RunWith(Parameterized.class)
public class InfinispanTransformersTestCase extends AbstractSubsystemTest {

    private static final Map<ModelTestControllerVersion, InfinispanSubsystemModel> VERSIONS = new EnumMap<>(ModelTestControllerVersion.class);
    static {
        VERSIONS.put(ModelTestControllerVersion.EAP_7_4_0, InfinispanSubsystemModel.VERSION_14_0_0);
    }

    @Parameters
    public static Iterable<ModelTestControllerVersion> parameters() {
        return VERSIONS.keySet();
    }

    private static String formatArtifact(String artifactId, ModelTestControllerVersion version) {
        return String.format("%s:%s:%s", version.getMavenGroupId(), artifactId, version.getMavenGavVersion());
    }

    private static String[] getDependencies(ModelTestControllerVersion version) {
        switch (version) {
            case EAP_7_4_0:
                return new String[] {
                        "org.jboss.spec.javax.transaction:jboss-transaction-api_1.3_spec:2.0.0.Final",
                        "org.jboss.spec.javax.resource:jboss-connector-api_1.7_spec:2.0.0.Final",
                        "org.infinispan:infinispan-commons:11.0.9.Final-redhat-00001",
                        "org.infinispan:infinispan-core:11.0.9.Final-redhat-00001",
                        "org.infinispan:infinispan-cachestore-jdbc:11.0.9.Final-redhat-00001",
                        "org.infinispan:infinispan-client-hotrod:11.0.9.Final-redhat-00001",
                        "org.jboss.spec.javax.resource:jboss-connector-api_1.7_spec:2.0.0.Final-redhat-00001",
                        // Following are needed for InfinispanSubsystemInitialization
                        formatArtifact("wildfly-clustering-api", version),
                        formatArtifact("wildfly-clustering-common", version),
                        formatArtifact("wildfly-clustering-infinispan-client", version),
                        formatArtifact("wildfly-clustering-infinispan-extension", version),
                        formatArtifact("wildfly-clustering-infinispan-spi", version),
                        formatArtifact("wildfly-clustering-jgroups-extension", version),
                        formatArtifact("wildfly-clustering-jgroups-spi", version),
                        formatArtifact("wildfly-clustering-server", version),
                        formatArtifact("wildfly-clustering-service", version),
                        formatArtifact("wildfly-clustering-singleton-api", version),
                        formatArtifact("wildfly-clustering-spi", version),
                        formatArtifact("wildfly-connector", version),
                };
            default: {
                throw new IllegalArgumentException();
            }
        }
    }

    private final ModelTestControllerVersion controllerVersion;
    private final ModelVersion subsystemVersion;

    public InfinispanTransformersTestCase(ModelTestControllerVersion controllerVersion) {
        super(InfinispanExtension.SUBSYSTEM_NAME, new InfinispanExtension());
        this.controllerVersion = controllerVersion;
        this.subsystemVersion = VERSIONS.get(controllerVersion).getVersion();
    }

    private static AdditionalInitialization createAdditionalInitialization() {
        return new DataSourcesSubsystemInitialization()
                .require(PathManager.SERVICE_DESCRIPTOR)
                .require(PathManager.PATH_SERVICE_DESCRIPTOR, ServerEnvironment.SERVER_TEMP_DIR)
                .require(OutboundSocketBinding.SERVICE_DESCRIPTOR, List.of("hotrod-server-1", "hotrod-server-2", "jdg1", "jdg2", "jdg3", "jdg4", "jdg5", "jdg6"))
                .require(CommonServiceDescriptor.DATA_SOURCE, "ExampleDS")
                .require(CommonServiceDescriptor.SSL_CONTEXT, "hotrod-elytron")
                .require(ForkChannelFactory.SERVICE_DESCRIPTOR, "maximal-channel")
                .require(ForkChannelFactory.DEFAULT_SERVICE_DESCRIPTOR)
                .require(TransactionResourceDefinition.LOCAL_TRANSACTION_PROVIDER)
                .require(TransactionResourceDefinition.XA_RESOURCE_RECOVERY_REGISTRY)
                ;
    }

    private KernelServicesBuilder createKernelServicesBuilder() {
        return this.createKernelServicesBuilder(createAdditionalInitialization());
    }

    private KernelServices build(KernelServicesBuilder builder) throws Exception {
        // initialize the legacy services and add required jars
        builder.createLegacyKernelServicesBuilder(createAdditionalInitialization(), this.controllerVersion, this.subsystemVersion)
                .addMavenResourceURL(getDependencies(this.controllerVersion))
                .addSingleChildFirstClass(DataSourcesSubsystemInitialization.class)
                .addSingleChildFirstClass(AdditionalInitialization.class)
                .addSingleChildFirstClass(ClassConfigurator.class)
                .skipReverseControllerCheck()
                .dontPersistXml();

        KernelServices services = builder.build();

        Assert.assertTrue(services.getBootErrorDescription(), services.isSuccessfulBoot());

        KernelServices legacyServices = services.getLegacyServices(this.subsystemVersion);

        Assert.assertTrue(legacyServices.getBootErrorDescription(), legacyServices.isSuccessfulBoot());

        return services;
    }

    @Test
    public void testTransformation() throws Exception {
        KernelServicesBuilder builder = this.createKernelServicesBuilder().setSubsystemXmlResource(String.format("infinispan-transform-%s.xml", this.subsystemVersion));
        KernelServices services = this.build(builder);

        ModelFixer fixer = model -> {
            if (InfinispanSubsystemModel.VERSION_16_0_0.requiresTransformation(this.subsystemVersion)) {
                @SuppressWarnings("deprecation")
                Map<String, List<PathElement>> containers = Map.ofEntries(Map.entry("minimal", List.of(DistributedCacheResourceDefinition.pathElement("dist"))),
                        Map.entry("maximal", List.of(DistributedCacheResourceDefinition.pathElement("dist"), LocalCacheResourceDefinition.pathElement("local"), ReplicatedCacheResourceDefinition.pathElement("cache-with-jdbc-store"), ScatteredCacheResourceDefinition.pathElement("scattered"))));
                for (Map.Entry<String, List<PathElement>> entry : containers.entrySet()) {
                    PathElement containerPath = CacheContainerResourceDefinition.pathElement(entry.getKey());
                    ModelNode containerModel = model.get(containerPath.getKeyValuePair());
                    containerModel.get("module").set(new ModelNode());
                    for (PathElement cachePath : entry.getValue()) {
                        ModelNode cacheModel = containerModel.get(cachePath.getKey()).get(cachePath.getValue());
                        cacheModel.get("module").set(new ModelNode());
                        if (cacheModel.hasDefined(HeapMemoryResourceDefinition.PATH.getKeyValuePair())) {
                            ModelNode memoryModel = cacheModel.get(HeapMemoryResourceDefinition.PATH.getKeyValuePair());
                            memoryModel.get("max-entries").set(new ModelNode());
                        }
                        if (cacheModel.hasDefined(JDBCStoreResourceDefinition.PATH.getKeyValuePair())) {
                            ModelNode storeModel = cacheModel.get(JDBCStoreResourceDefinition.PATH.getKeyValuePair());
                            storeModel.get("datasource").set(new ModelNode());
                            if (storeModel.hasDefined(StringTableResourceDefinition.PATH.getKeyValuePair())) {
                                ModelNode tableModel = storeModel.get(StringTableResourceDefinition.PATH.getKeyValuePair());
                                tableModel.get("batch-size").set(new ModelNode());
                            }
                        }
                    }
                    for (ScheduledThreadPoolResourceDefinition pool : EnumSet.allOf(ScheduledThreadPoolResourceDefinition.class)) {
                        if (containerModel.hasDefined(pool.getPathElement().getKeyValuePair())) {
                            ModelNode poolModel = containerModel.get(pool.getPathElement().getKeyValuePair());
                            poolModel.get("max-threads").set(new ModelNode());
                        }
                    }
                }
            }
            return model;
        };

        // check that both versions of the legacy model are the same and valid
        checkSubsystemModelTransformation(services, this.subsystemVersion, fixer, false);

        // Validate transformed model
        ModelNode legacyModel = services.readTransformedModel(this.subsystemVersion);
        if (InfinispanSubsystemModel.VERSION_18_0_0.requiresTransformation(this.subsystemVersion)) {
            ModelNode subsystemModel = legacyModel.get(InfinispanSubsystemResourceDefinition.PATH.getKeyValuePair());
            // Verify module conversions to legacy module names
            for (Property property : subsystemModel.get(CacheContainerResourceDefinition.WILDCARD_PATH.getKey()).asPropertyListOrEmpty()) {
                List<ModelNode> modules = property.getValue().get(CacheContainerResourceDefinition.ListAttribute.MODULES.getName()).asListOrEmpty();
                Assert.assertTrue(modules.stream().map(ModelNode::asString).noneMatch("org.wildfly.clustering.session.infinispan.embedded"::equals));
            }
            for (Property property : subsystemModel.get(RemoteCacheContainerResourceDefinition.WILDCARD_PATH.getKey()).asPropertyList()) {
                List<ModelNode> modules = property.getValue().get(RemoteCacheContainerResourceDefinition.ListAttribute.MODULES.getName()).asListOrEmpty();
                Assert.assertTrue(modules.stream().map(ModelNode::asString).noneMatch("org.wildfly.clustering.session.infinispan.remote"::equals));
            }
        }
    }

    @Test
    public void testRejections() throws Exception {
        // create builder for current subsystem version
        KernelServicesBuilder builder = this.createKernelServicesBuilder();
        KernelServices services = this.build(builder);

        // test failed operations involving backups
        List<ModelNode> operations = builder.parseXmlResource("infinispan-reject.xml");

        FailedOperationTransformationConfig config = new FailedOperationTransformationConfig();
        PathAddress subsystemAddress = PathAddress.pathAddress(InfinispanSubsystemResourceDefinition.PATH);
        PathAddress containerAddress = subsystemAddress.append(CacheContainerResourceDefinition.WILDCARD_PATH);
        PathAddress remoteContainerAddress = subsystemAddress.append(RemoteCacheContainerResourceDefinition.WILDCARD_PATH);
        List<String> rejectedRemoteContainerAttributes = new LinkedList<>();

        if (InfinispanSubsystemModel.VERSION_16_0_0.requiresTransformation(this.subsystemVersion)) {
            config.addFailedAttribute(containerAddress.append(ReplicatedCacheResourceDefinition.pathElement("repl"), PartitionHandlingResourceDefinition.PATH), new FailedOperationTransformationConfig.NewAttributesConfig(PartitionHandlingResourceDefinition.Attribute.MERGE_POLICY.getDefinition()));
            config.addFailedAttribute(containerAddress.append(DistributedCacheResourceDefinition.pathElement("dist"), PartitionHandlingResourceDefinition.PATH), new FailedOperationTransformationConfig.NewAttributesConfig(PartitionHandlingResourceDefinition.Attribute.WHEN_SPLIT.getDefinition()));
        }

        if (InfinispanSubsystemModel.VERSION_15_0_0.requiresTransformation(this.subsystemVersion)) {
            config.addFailedAttribute(containerAddress, new FailedOperationTransformationConfig.NewAttributesConfig(CacheContainerResourceDefinition.Attribute.MARSHALLER.getDefinition()));
            rejectedRemoteContainerAttributes.add(RemoteCacheContainerResourceDefinition.Attribute.MARSHALLER.getName());
        }

        if (InfinispanSubsystemModel.VERSION_14_0_0.requiresTransformation(this.subsystemVersion)) {
            rejectedRemoteContainerAttributes.add(RemoteCacheContainerResourceDefinition.ListAttribute.MODULES.getName());
        }

        if (!rejectedRemoteContainerAttributes.isEmpty()) {
            config.addFailedAttribute(remoteContainerAddress, new FailedOperationTransformationConfig.NewAttributesConfig(rejectedRemoteContainerAttributes.toArray(new String[rejectedRemoteContainerAttributes.size()])));
        }

        ModelTestUtils.checkFailedTransformedBootOperations(services, this.subsystemVersion, operations, config);
    }
}
