/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.util.EnumSet;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.clustering.controller.CommonServiceDescriptor;
import org.jboss.as.clustering.subsystem.AdditionalInitialization;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.subsystem.test.AbstractSubsystemSchemaTest;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.as.version.Stability;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests parsing / booting / marshalling of JGroups configurations.
 *
 * The current XML configuration is tested, along with supported legacy configurations.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 * @author Radoslav Husar
 */
@RunWith(value = Parameterized.class)
public class JGroupsSubsystemTestCase extends AbstractSubsystemSchemaTest<JGroupsSubsystemSchema> {

    @Parameters
    public static Iterable<JGroupsSubsystemSchema> parameters() {
        return EnumSet.allOf(JGroupsSubsystemSchema.class);
    }

    private final JGroupsSubsystemSchema schema;

    public JGroupsSubsystemTestCase(JGroupsSubsystemSchema schema) {
        super(JGroupsSubsystemResourceDefinitionRegistrar.REGISTRATION.getName(), new JGroupsExtension(), schema, JGroupsSubsystemSchema.CURRENT);
        this.schema = schema;
    }

    @Override
    protected String getSubsystemXsdPathPattern() {
        return (this.schema.getStability() == Stability.DEFAULT) ? "schema/jboss-as-%1$s_%2$d_%3$d.xsd" : "schema/jboss-as-%1$s_%4$s_%2$d_%3$d.xsd";
    }

    private KernelServices buildKernelServices() throws Exception {
        return this.buildKernelServices(this.getSubsystemXml());
    }

    private KernelServices buildKernelServices(String xml) throws Exception {
        return this.createKernelServicesBuilder(xml).build();
    }

    private KernelServicesBuilder createKernelServicesBuilder() {
        return this.createKernelServicesBuilder(this.createAdditionalInitialization());
    }

    private KernelServicesBuilder createKernelServicesBuilder(String xml) throws XMLStreamException {
        return this.createKernelServicesBuilder().setSubsystemXml(xml);
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return new AdditionalInitialization(this.schema)
                .require(SocketBinding.SERVICE_DESCRIPTOR, List.of("jgroups-tcp", "jgroups-udp", "some-binding", "some-other-binding", "jgroups-diagnostics", "jgroups-mping", "jgroups-tcp-fd", "jgroups-client-fd"))
                .require(OutboundSocketBinding.SERVICE_DESCRIPTOR, List.of("node1", "node2"))
                .require(CommonServiceDescriptor.KEY_STORE, "my-key-store")
                .require(CommonServiceDescriptor.CREDENTIAL_STORE, "my-credential-store")
                .require(CommonServiceDescriptor.DATA_SOURCE, "ExampleDS")
                ;
    }

    /**
     * Tests that the 'fork' and 'stack' resources allow indexed adds for the 'protocol' children. This is important for
     * the work being done for WFCORE-401. This work involves calculating the operations to bring the secondary Host Controller's domain model
     * into sync with the primary Host Controller's domain model. Without ordered resources, that would mean on reconnect if the primary
     * had added a protocol somewhere in the middle, the protocol would get added to the end rather at the correct place.
     */
    @Test
    public void testIndexedAdds() throws Exception {
        if (!this.schema.since(JGroupsSubsystemSchema.VERSION_3_0)) return;

        final KernelServices services = this.buildKernelServices();

        ModelNode originalSubsystemModel = services.readWholeModel().get(JGroupsSubsystemResourceDefinitionRegistrar.REGISTRATION.getPathElement().getKeyValuePair());
        ModelNode originalChannelModel = originalSubsystemModel.get(JGroupsResourceRegistration.CHANNEL.pathElement("ee").getKeyValuePair());
        ModelNode originalForkModel = originalChannelModel.get(JGroupsResourceRegistration.FORK.pathElement("web").getKeyValuePair());

        Assert.assertTrue(originalForkModel.isDefined());
        originalForkModel.protect();
        Assert.assertFalse(originalForkModel.get(StackResourceDefinitionRegistrar.Component.PROTOCOL.getPathElement().getKey()).keys().isEmpty());

        ModelNode originalStackModel = originalSubsystemModel.get(JGroupsResourceRegistration.STACK.pathElement("maximal").getKeyValuePair());
        Assert.assertTrue(originalStackModel.isDefined());
        originalStackModel.protect();


        final PathAddress subsystemAddress = PathAddress.pathAddress(JGroupsSubsystemResourceDefinitionRegistrar.REGISTRATION.getPathElement());
        final PathAddress forkAddress = subsystemAddress.append(JGroupsResourceRegistration.CHANNEL.pathElement("ee")).append(JGroupsResourceRegistration.FORK.pathElement("web"));
        final PathAddress stackAddress = subsystemAddress.append(JGroupsResourceRegistration.STACK.pathElement("maximal"));

        //Check the fork protocols honour indexed adds by inserting a protocol at the start
        ModelNode add = Util.createAddOperation(forkAddress.append(StackResourceDefinitionRegistrar.Component.PROTOCOL.pathElement("MERGE3")), 0);
        ModelTestUtils.checkOutcome(services.executeOperation(add));

        ModelNode subsystemModel = services.readWholeModel().get(JGroupsSubsystemResourceDefinitionRegistrar.REGISTRATION.getPathElement().getKeyValuePair());
        ModelNode channelModel = subsystemModel.get(JGroupsResourceRegistration.CHANNEL.pathElement("ee").getKeyValuePair());
        ModelNode forkModel = channelModel.get(JGroupsResourceRegistration.FORK.pathElement("web").getKeyValuePair());

        Assert.assertEquals(originalForkModel.keys().size() + 1, forkModel.get(StackResourceDefinitionRegistrar.Component.PROTOCOL.getPathElement().getKey()).keys().size());
        Assert.assertEquals("MERGE3", forkModel.get(StackResourceDefinitionRegistrar.Component.PROTOCOL.getPathElement().getKey()).keys().iterator().next());

        //Check the stack protocols honour indexed adds by removing a protocol in the middle and re-adding it
        ModelNode remove = Util.createRemoveOperation(stackAddress.append(StackResourceDefinitionRegistrar.Component.PROTOCOL.pathElement("FD")));
        ModelTestUtils.checkOutcome(services.executeOperation(remove));
        add = Util.createAddOperation(stackAddress.append(StackResourceDefinitionRegistrar.Component.PROTOCOL.pathElement("FD")), 3); //The original index of the FD protocol
        ModelTestUtils.checkOutcome(services.executeOperation(add));

        subsystemModel = services.readWholeModel().get(JGroupsSubsystemResourceDefinitionRegistrar.REGISTRATION.getPathElement().getKeyValuePair());
        ModelNode stackModel = subsystemModel.get(JGroupsResourceRegistration.STACK.pathElement("maximal").getKeyValuePair());
        Assert.assertEquals(originalStackModel, stackModel);
    }
}
