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

    public JGroupsSubsystemTest() {
        super(JGroupsExtension.SUBSYSTEM_NAME, new JGroupsExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem-jgroups-test.xml");
    }

    @Override
    protected Set<PathAddress> getIgnoredChildResourcesForRemovalTest() {
        // create a collection of resources in the test which are not removed by a "remove" command
        // i.e. all resources of form /subsystem=jgroups/stack=maximal/protocol=*

        String[] protocolList = {"MPING", "MERGE2", "FD_SOCK", "FD", "VERIFY_SUSPECT", "BARRIER",
                "pbcast.NAKACK", "UNICAST2", "pbcast.STABLE", "pbcast.GMS", "UFC", "MFC", "FRAG2",
                "pbcast.STATE_TRANSFER", "pbcast.FLUSH"};
        PathAddress subsystem = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, JGroupsExtension.SUBSYSTEM_NAME));
        PathAddress stack = subsystem.append(PathElement.pathElement(ModelKeys.STACK, "maximal"));
        List<PathAddress> addresses = new ArrayList<PathAddress>();
        for (String protocol : protocolList) {
            PathAddress ignoredChild = stack.append(PathElement.pathElement(ModelKeys.PROTOCOL, protocol));
            addresses.add(ignoredChild);
        }
        return new HashSet<PathAddress>(addresses);
    }

    @Test
    public void testProtocolOrdering() throws Exception {
        String xml = getSubsystemXml();
        // KernelServices kernel = super.installInController(xml);
        KernelServices kernel = super.createKernelServicesBuilder(null).setSubsystemXml(xml).build();

        ModelNode model = kernel.readWholeModel().require("subsystem").require("jgroups").require("stack").require("maximal");
        List<ModelNode> protocols = model.require("protocols").asList();
        Set<String> keys = model.get("protocol").keys();

        Assert.assertEquals(protocols.size(), keys.size());
        int i = 0;
        for (String key : keys) {
            String name = protocols.get(i).asString();
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
        String[] protocolList = {"PING", "MERGE3", "FD_SOCK", "FD", "VERIFY_SUSPECT", "BARRIER", "pbcast.NAKACK2", "UNICAST2",
                          "pbcast.STABLE", "pbcast.GMS", "UFC", "MFC", "FRAG2", "RSVP"} ;

        for (int i = 0; i < protocolList.length; i++) {
            ModelNode protocol = new ModelNode();
            protocol.get(ModelKeys.TYPE).set(protocolList[i]) ;
            protocol.get("socket-binding").set("jgroups-udp");
            System.out.println("adding protovcol = " + protocol.toString());
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
        //Get the model and the persisted xml from the first controller
        final ModelNode modelA = servicesA.readWholeModel();
        validateModel(modelA);
        ModelNode protos = modelA.get(SUBSYSTEM,getMainSubsystemName(),"stack","udp","protocols");
        Assert.assertEquals("we should get back same number of protocols that we send",14,protos.asList().size());
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
