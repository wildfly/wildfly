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
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author <a href="tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */

public class JGroupsSubsystemTransformerTestCase extends AbstractSubsystemBaseTest {


    public JGroupsSubsystemTransformerTestCase() {
        super(JGroupsExtension.SUBSYSTEM_NAME, new JGroupsExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("jgroups-transformer_1_1.xml");
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
            ;
            addresses.add(ignoredChild);
        }
        return new HashSet<PathAddress>(addresses);
    }

    /**
     * Tests transformation of model from 1.1.1 version into 1.1.0 version.
     *
     * This test does not pass because of an error in the 1.1.0 model:
     * The model entry 'protocols' in /subsystem=jgroups/stack=* is used
     * to store an ordered list of protocol names, but it is not registered
     * as an attribute, where as it is registered in the 1.1.1 model.
     * This breaks the test.
     *
     * @throws Exception
     */
    @Ignore
    @Test
    public void testTransformer_1_1_0() throws Exception {
        ModelVersion version = ModelVersion.create(1, 1);
        KernelServicesBuilder builder = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXml(getSubsystemXml());
        builder.createLegacyKernelServicesBuilder(null, version)
            .addMavenResourceURL("org.jboss.as:jboss-as-clustering-jgroups:7.1.2.Final");

        KernelServices mainServices = builder.build();

        checkSubsystemModelTransformation(mainServices, version);
    }
}
