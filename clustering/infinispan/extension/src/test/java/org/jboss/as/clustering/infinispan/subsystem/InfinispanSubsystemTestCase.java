/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.EnumSet;
import java.util.List;
import java.util.Properties;

import org.jboss.as.clustering.controller.CommonServiceDescriptor;
import org.jboss.as.clustering.jgroups.subsystem.JGroupsSubsystemResourceDefinitionRegistrar;
import org.jboss.as.clustering.subsystem.AdditionalInitialization;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.as.subsystem.test.AbstractSubsystemSchemaTest;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.wildfly.clustering.jgroups.spi.ForkChannelFactory;

/**
 * Tests parsing / booting / marshalling of Infinispan configurations.
 *
 * The current XML configuration is tested, along with supported legacy configurations.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 */
@RunWith(value = Parameterized.class)
public class InfinispanSubsystemTestCase extends AbstractSubsystemSchemaTest<InfinispanSubsystemSchema> {

    @Parameters
    public static Iterable<InfinispanSubsystemSchema> parameters() {
        return EnumSet.allOf(InfinispanSubsystemSchema.class);
    }

    private final InfinispanSubsystemSchema schema;

    public InfinispanSubsystemTestCase(InfinispanSubsystemSchema schema) {
        super(InfinispanSubsystemResourceDefinitionRegistrar.REGISTRATION.getName(), new InfinispanExtension(), schema, InfinispanSubsystemSchema.CURRENT);
        this.schema = schema;
    }

    @Override
    protected String getSubsystemXsdPathPattern() {
        return "schema/jboss-as-%s_%d_%d.xsd";
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return new JGroupsSubsystemInitialization()
                .require(OutboundSocketBinding.SERVICE_DESCRIPTOR, List.of("hotrod-server-1", "hotrod-server-2"))
                .require(CommonServiceDescriptor.DATA_SOURCE, "ExampleDS")
                .require(PathManager.PATH_SERVICE_DESCRIPTOR, "jboss.server.temp.dir")
                .require(ForkChannelFactory.DEFAULT_SERVICE_DESCRIPTOR)
                .require(ForkChannelFactory.SERVICE_DESCRIPTOR, "maximal-channel")
                .require(TransactionResourceDefinitionRegistrar.LOCAL_TRANSACTION_PROVIDER)
                .require(TransactionResourceDefinitionRegistrar.XA_RESOURCE_RECOVERY_REGISTRY)
                ;
    }

    @Override
    protected void compare(ModelNode model1, ModelNode model2) {
        purgeJGroupsModel(model1);
        purgeJGroupsModel(model2);
        super.compare(model1, model2);
    }

    private static void purgeJGroupsModel(ModelNode model) {
        model.get(JGroupsSubsystemResourceDefinitionRegistrar.REGISTRATION.getPathElement().getKey()).remove(JGroupsSubsystemResourceDefinitionRegistrar.REGISTRATION.getPathElement().getValue());
    }

    @Override
    protected Properties getResolvedProperties() {
        Properties properties = new Properties();
        properties.put("java.io.tmpdir", "/tmp");
        return properties;
    }

    @Override
    protected KernelServices standardSubsystemTest(String configId, String configIdResolvedModel, boolean compareXml, org.jboss.as.subsystem.test.AdditionalInitialization additionalInit) throws Exception {
        KernelServices services = super.standardSubsystemTest(configId, configIdResolvedModel, compareXml, additionalInit);

        if (!this.schema.since(InfinispanSubsystemSchema.VERSION_1_5)) {
            ModelNode model = services.readWholeModel();

            Assert.assertTrue(model.hasDefined(InfinispanSubsystemResourceDefinitionRegistrar.REGISTRATION.getPathElement().getKeyValuePair()));
            ModelNode subsystem = model.get(InfinispanSubsystemResourceDefinitionRegistrar.REGISTRATION.getPathElement().getKeyValuePair());

            for (Property containerProp : subsystem.get(CacheContainerResourceDefinitionRegistrar.REGISTRATION.getPathElement().getKey()).asPropertyList()) {
                Assert.assertTrue("cache-container=" + containerProp.getName(),
                        containerProp.getValue().get(CacheContainerResourceDefinitionRegistrar.STATISTICS_ENABLED.getName()).asBoolean());

                for (String key : containerProp.getValue().keys()) {
                    if (key.endsWith("-cache") && !key.equals("default-cache")) {
                        ModelNode caches = containerProp.getValue().get(key);
                        if (caches.isDefined()) {
                            for (Property cacheProp : caches.asPropertyList()) {
                                Assert.assertTrue("cache-container=" + containerProp.getName() + "," + key + "=" + cacheProp.getName(),
                                        containerProp.getValue().get(CacheResourceDefinitionRegistrar.STATISTICS_ENABLED.getName()).asBoolean());
                            }
                        }
                    }
                }
            }
        }

        return services;
    }
}
