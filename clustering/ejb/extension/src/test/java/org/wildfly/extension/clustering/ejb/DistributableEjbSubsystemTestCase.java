/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.ejb;

import org.jboss.as.clustering.subsystem.AdditionalInitialization;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.subsystem.test.AbstractSubsystemSchemaTest;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;

import java.util.EnumSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit test for distributable-ejb subsystem.
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
@RunWith(value = Parameterized.class)
public class DistributableEjbSubsystemTestCase extends AbstractSubsystemSchemaTest<DistributableEjbSubsystemSchema> {

    @Parameters
    public static Iterable<DistributableEjbSubsystemSchema> parameters() {
        return EnumSet.allOf(DistributableEjbSubsystemSchema.class);
    }

    public DistributableEjbSubsystemTestCase(DistributableEjbSubsystemSchema schema) {
        super(DistributableEjbExtension.SUBSYSTEM_NAME, new DistributableEjbExtension(), schema, DistributableEjbSubsystemSchema.CURRENT);
    }

    /**
     * The distributable-ejb subsystem depends on an infinispan cache-container and cache.
     */
    @Override
    protected org.jboss.as.subsystem.test.AdditionalInitialization createAdditionalInitialization() {
        return new AdditionalInitialization()
                .require(InfinispanServiceDescriptor.DEFAULT_CACHE_CONFIGURATION, "foo")
                .require(InfinispanServiceDescriptor.CACHE_CONFIGURATION, "foo", "bar")
                ;
    }

    /**
     * Verifies that we may add and remove additional bean-management providers.
     * @throws Exception for any test failures
     */
    @Test
    public void testInfinispanBeanManagement() throws Exception {
        final String subsystemXml = getSubsystemXml();
        final KernelServices ks = createKernelServicesBuilder(createAdditionalInitialization()).setSubsystemXml(subsystemXml).build();
        assertTrue("Subsystem boot failed!", ks.isSuccessfulBoot());

        final PathAddress distributableEjbAddress = PathAddress.pathAddress(DistributableEjbResourceDefinition.PATH);
        final PathAddress anotherBeanManagementProviderAddress = distributableEjbAddress.append(InfinispanBeanManagementResourceDefinition.pathElement("another-bean-management-provider"));

        // add a new bean-management instance
        ModelNode anotherBeanManagementProvider = Util.createAddOperation(anotherBeanManagementProviderAddress);
        anotherBeanManagementProvider.get(InfinispanBeanManagementResourceDefinition.CACHE_ATTRIBUTE_GROUP.getContainerAttribute().getName()).set("foo");
        anotherBeanManagementProvider.get(InfinispanBeanManagementResourceDefinition.CACHE_ATTRIBUTE_GROUP.getCacheAttribute().getName()).set("bar");
        anotherBeanManagementProvider.get(BeanManagementResourceDefinition.Attribute.MAX_ACTIVE_BEANS.getName()).set(11);
        ModelNode addResponse = ks.executeOperation(anotherBeanManagementProvider);
        assertEquals(addResponse.toString(), ModelDescriptionConstants.SUCCESS, addResponse.get(ModelDescriptionConstants.OUTCOME).asString());

        // check max-active-beans attribute value
        ModelNode readMaxActiveBeansAttribute = Util.getReadAttributeOperation(anotherBeanManagementProviderAddress, BeanManagementResourceDefinition.Attribute.MAX_ACTIVE_BEANS.getName());
        ModelNode readMaxActiveBeansResult = ks.executeOperation(readMaxActiveBeansAttribute);
        assertEquals(readMaxActiveBeansResult.toString(), ModelDescriptionConstants.SUCCESS, readMaxActiveBeansResult.get(ModelDescriptionConstants.OUTCOME).asString());
        assertEquals(readMaxActiveBeansResult.toString(), 11, readMaxActiveBeansResult.get(ModelDescriptionConstants.RESULT).asInt());

        // remove the bean management instance
        ModelNode removeAnotherBeanManagementProvider = Util.createRemoveOperation(anotherBeanManagementProviderAddress);
        ModelNode removeResponse = ks.executeOperation(removeAnotherBeanManagementProvider);
        assertEquals(removeResponse.toString(), ModelDescriptionConstants.SUCCESS, removeResponse.get(ModelDescriptionConstants.OUTCOME).asString());
    }

    /**
     * Verifies that we may not have client mappings registry providers.
     * @throws Exception for any test failures
     */
    @Test
    public void testClientMappingsRegistry() throws Exception {
        final String subsystemXml = getSubsystemXml();
        final KernelServices ks = createKernelServicesBuilder(createAdditionalInitialization()).setSubsystemXml(subsystemXml).build();
        assertTrue("Subsystem boot failed!", ks.isSuccessfulBoot());

        final PathAddress distributableEjbAddress = PathAddress.pathAddress(DistributableEjbResourceDefinition.PATH);
        final PathAddress infinispanClientMappingsRegistryProviderAddress = distributableEjbAddress.append(ClientMappingsRegistryProviderResourceDefinition.pathElement("infinispan"));

        // add a new client-mappings-registry instance
        ModelNode addInfinispanClientMappingsRegistryProvider = Util.createAddOperation(infinispanClientMappingsRegistryProviderAddress);
        addInfinispanClientMappingsRegistryProvider.get(InfinispanClientMappingsRegistryProviderResourceDefinition.CACHE_ATTRIBUTE_GROUP.getContainerAttribute().getName()).set("foo");
        addInfinispanClientMappingsRegistryProvider.get(InfinispanClientMappingsRegistryProviderResourceDefinition.CACHE_ATTRIBUTE_GROUP.getCacheAttribute().getName()).set("bar");
        ModelNode addResponse = ks.executeOperation(addInfinispanClientMappingsRegistryProvider);
        assertEquals(addResponse.toString(), ModelDescriptionConstants.SUCCESS, addResponse.get(ModelDescriptionConstants.OUTCOME).asString());

        // check that the old registry is no longer present and has been replaced by the new registry
        final ModelNode distributableEjbSubsystem = ks.readWholeModel().get(DistributableEjbResourceDefinition.PATH.getKeyValuePair());
        final ModelNode localClientMappingsRegistryProvider = distributableEjbSubsystem.get(LocalClientMappingsRegistryProviderResourceDefinition.PATH.getKeyValuePair());
        final ModelNode infinispanClientMappingsRegistryProvider = distributableEjbSubsystem.get(InfinispanClientMappingsRegistryProviderResourceDefinition.PATH.getKeyValuePair());

        assertEquals(localClientMappingsRegistryProvider.toString(), false, localClientMappingsRegistryProvider.isDefined());
        assertEquals(infinispanClientMappingsRegistryProvider.toString(), true, infinispanClientMappingsRegistryProvider.isDefined());
    }

    /**
     * Verifies that attributes with expression are handled properly.
     * @throws Exception for any test failures
     */
    @Test
    public void testExpressions() throws Exception {
        final String subsystemXml = getSubsystemXml();
        final KernelServices ks = createKernelServicesBuilder(createAdditionalInitialization()).setSubsystemXml(subsystemXml).build();
        assertTrue("Subsystem boot failed!", ks.isSuccessfulBoot());

        final ModelNode subsystem = ks.readWholeModel().get(DistributableEjbResourceDefinition.PATH.getKeyValuePair());
        final ModelNode beanManagement = subsystem.get(InfinispanBeanManagementResourceDefinition.pathElement("default").getKeyValuePair());

        // default within the expression should be 10000
        final int maxActiveBeans = beanManagement.get(BeanManagementResourceDefinition.Attribute.MAX_ACTIVE_BEANS.getName()).resolve().asInt();
        assertEquals(10000, maxActiveBeans);

        ModelNode persistentTimerManagement = subsystem.get(InfinispanTimerManagementResourceDefinition.pathElement("distributed").getKeyValuePair());
        assertEquals(100, persistentTimerManagement.get(InfinispanTimerManagementResourceDefinition.Attribute.MAX_ACTIVE_TIMERS.getName()).resolve().asInt());

        ModelNode transientTimerManagement = subsystem.get(InfinispanTimerManagementResourceDefinition.pathElement("transient").getKeyValuePair());
        assertEquals(1000, transientTimerManagement.get(InfinispanTimerManagementResourceDefinition.Attribute.MAX_ACTIVE_TIMERS.getName()).resolve().asInt());
    }
}
