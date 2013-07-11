/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
* as indicated by the @author tags. See the copyright.txt file in the
* distribution for a full listing of individual contributors.
*
* This is free software; you can redistribute it and/or modify it
* under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2.1 of
* the License, or (at your option) any later version.
*
* This software is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
package org.jboss.as.clustering.jgroups.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class JGroupsSubsystemTest extends AbstractSubsystemBaseTest {

    private static final String[] PROTOCOL_LIST_MINIMAL = { "MPING" };
    private static final String[] PROTOCOL_LIST_MAXIMAL = { "MPING", "MERGE2", "FD_SOCK", "FD", "VERIFY_SUSPECT", "pbcast.NAKACK2", "UNICAST2", "pbcast.STABLE", "pbcast.GMS", "UFC", "MFC", "FRAG2", "RSVP" };
    private static final String[] PROTOCOL_LIST_B = { "PING", "MERGE3", "FD_SOCK", "FD", "VERIFY_SUSPECT", "BARRIER", "pbcast.NAKACK2", "UNICAST2", "pbcast.STABLE", "pbcast.GMS", "UFC", "MFC", "FRAG2", "RSVP"} ;

    public JGroupsSubsystemTest() {
        super(JGroupsExtension.SUBSYSTEM_NAME, new JGroupsExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem-jgroups-test.xml");
    }


    /*
     * This method is brequired by the subsystem test to identify all resources which are not removed
     * via a standard resource=X:remove() command. This is to allow a generic test for removal.
     *
     * so,create a collection of resources in the test which are not removed by a "remove" command:
     *
     * i.e. all resources of form /subsystem=jgroups/stack=maximal/protocol=*  and
     * i.e. all resources of form /subsystem=jgroups/stack=minimal/protocol=*
     *
     * which are actually removed via stack=X:remove-protocol
     */
    @Override
    protected Set<PathAddress> getIgnoredChildResourcesForRemovalTest() {
        HashSet<PathAddress> result = new HashSet<PathAddress>();

        PathAddress subsystem = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, JGroupsExtension.SUBSYSTEM_NAME));

        // set up the ignored resources for stack maximal
        PathAddress minimalStack = subsystem.append(PathElement.pathElement(ModelKeys.STACK, "minimal"));
        List<PathAddress> minimalAddresses = new ArrayList<PathAddress>();
        for (String protocol : PROTOCOL_LIST_MINIMAL) {
            PathAddress ignoredChild = minimalStack.append(PathElement.pathElement(ModelKeys.PROTOCOL, protocol));
            minimalAddresses.add(ignoredChild);
        }
        result.addAll(minimalAddresses);

        // set up the ignored resources for stack maximal
        PathAddress maximalStack = subsystem.append(PathElement.pathElement(ModelKeys.STACK, "maximal"));
        List<PathAddress> maximalAddresses = new ArrayList<PathAddress>();
        for (String protocol : PROTOCOL_LIST_MAXIMAL) {
            PathAddress ignoredChild = maximalStack.append(PathElement.pathElement(ModelKeys.PROTOCOL, protocol));
            maximalAddresses.add(ignoredChild);
        }
        result.addAll(maximalAddresses);

        return result;
    }

    @Test
    public void testProtocolOrdering() throws Exception {
        String xml = getSubsystemXml();
        // KernelServices kernel = super.installInController(xml);
        KernelServices kernel = super.createKernelServicesBuilder(null).setSubsystemXml(xml).build();

        if (!kernel.isSuccessfulBoot()) {
            System.out.println("testProtocolOrdering: boot error = " + kernel.getBootError());
        }

        ModelNode model = kernel.readWholeModel().require("subsystem").require("jgroups").require("stack").require("maximal");
        Set<String> keys = model.get("protocol").keys();

        // the order should match PROTOCOL_LIST_A
        Assert.assertEquals(PROTOCOL_LIST_MAXIMAL.length, keys.size());
        int i = 0;
        for (String key : keys) {
            String name = PROTOCOL_LIST_MAXIMAL[i];
            Assert.assertEquals(key, name);
            i++;
        }
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return new AdditionalInitialization() {
            protected RunningMode getRunningMode() {
                return RunningMode.ADMIN_ONLY;
            }

            @Override
            protected boolean isValidateOperations() {
                return false;
            }
        };
    }

    @Test
    public void testLegacyOperations() throws Exception {
        List<ModelNode> ops = new LinkedList<ModelNode>();
        PathAddress subsystemAddress = PathAddress.EMPTY_ADDRESS.append(SUBSYSTEM, getMainSubsystemName());
        PathAddress udpAddress = subsystemAddress.append("stack", "udp");
        PathAddress tcpAddress = subsystemAddress.append("stack", "tcp");

        ModelNode op = Util.createAddOperation(subsystemAddress);
        ///subsystem=jgroups:add(default-stack=udp)
        op.get("default-stack").set("udp");
        ops.add(op);
        /*/subsystem=jgroups/stack=udp:add(transport={"type"=>"UDP","socket-binding"=>"jgroups-udp"},protocols=["PING","MERGE3","FD_SOCK","FD","VERIFY_SUSPECT","BARRIER","pbcast.NAKACK2","UNICAST2","pbcast.STABLE","pbcast.GMS","UFC","MFC","FRAG2","RSVP"]) */
        op = Util.createAddOperation(udpAddress);
        ModelNode transport = new ModelNode();
        transport.get("type").set("UDP");
        transport.get("socket-binding").set("jgroups-udp");

        ModelNode protocols = new ModelNode();

        for (int i = 0; i < PROTOCOL_LIST_B.length; i++) {
            ModelNode protocol = new ModelNode();
            protocol.get(ModelKeys.TYPE).set(PROTOCOL_LIST_B[i]) ;
            protocol.get("socket-binding").set("jgroups-udp");
            protocols.add(protocol);
        }

        op.get("transport").set(transport);
        op.get("protocols").set(protocols);
        ops.add(op);
        /*/subsystem=jgroups/stack=udp/protocol= FD_SOCK:write-attribute(name=socket-binding,value=jgroups-udp-fd)*/
        ops.add(Util.getWriteAttributeOperation(udpAddress.append("protocol", "FD_SOCK"), "socket-binding", new ModelNode("jgroups-udp-fd")));


        /*
        /subsystem=jgroups/stack=tcp:add(transport={"type"=>"TCP","socket-binding"=>"jgroups-tcp"},protocols=["MPING","MERGE2","FD_SOCK","FD","VERIFY_SUSPECT","pbcast.NAKACK2","UNICAST2","pbcast.STABLE","pbcast.GMS","UFC","MFC","FRAG2","RSVP"])
        /subsystem=jgroups/stack=tcp/protocol= MPING:write-attribute(name=socket-binding,value=jgroups-mping)
        /subsystem=jgroups/stack=tcp/protocol= FD_SOCK:write-attribute(name=socket-binding,value=jgroups-tcp-fd)*/


        KernelServices servicesA = createKernelServicesBuilder(createAdditionalInitialization())
                .setBootOperations(ops).build();

        Assert.assertTrue("Subsystem boot failed!", servicesA.isSuccessfulBoot());
        // get the model and the persisted xml from the first controller
        final ModelNode modelA = servicesA.readWholeModel();
        validateModel(modelA);
        // check the number of protocol=* elements
        List<ModelNode> protos = modelA.get(SUBSYSTEM,getMainSubsystemName(),"stack","udp","protocol").asList();
        Assert.assertEquals("we should get back same number of protocols that we send", 14, protos.size());
        servicesA.shutdown();

        /*// Test the describe operation
        final ModelNode operation = createDescribeOperation();
        final ModelNode result = servicesA.executeOperation(operation);
        Assert.assertTrue("the subsystem describe operation has to generate a list of operations to recreate the subsystem",
                !result.hasDefined(ModelDescriptionConstants.FAILURE_DESCRIPTION));
        final List<ModelNode> operations = result.get(ModelDescriptionConstants.RESULT).asList();
        servicesA.shutdown();

        final KernelServices servicesC = createKernelServicesBuilder(createAdditionalInitialization()).setBootOperations(operations).build();
        final ModelNode modelC = servicesC.readWholeModel();

        compare(modelA, modelC);

        assertRemoveSubsystemResources(servicesC, getIgnoredChildResourcesForRemovalTest());*/
    }
}
