/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.test.integration.domain.mixed.eap630;

import static org.jboss.as.controller.operations.common.Util.createAddOperation;
import static org.jboss.as.controller.operations.common.Util.createRemoveOperation;
import static org.jboss.as.controller.operations.common.Util.getUndefineAttributeOperation;
import static org.jboss.as.controller.operations.common.Util.getWriteAttributeOperation;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.test.integration.domain.mixed.eap640.DomainAdjuster640;
import org.jboss.dmr.ModelNode;

/**
 * Does adjustments to the domain model for 6.3.0 legacy slaves
 *
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class DomainAdjuster630 extends DomainAdjuster640 {
    @Override
    protected List<ModelNode> adjustForVersion(final DomainClient client, PathAddress profileAddress) throws Exception {
        List<ModelNode> list = super.adjustForVersion(client, profileAddress);

//        list.addAll(adjustInfinispan(profileAddress.append(SUBSYSTEM, InfinispanExtension.SUBSYSTEM_NAME)));
//        list.addAll(adjustJGroups(profileAddress.append(SUBSYSTEM, JGroupsExtension.SUBSYSTEM_NAME)));


        return list;
    }






    private List<ModelNode> adjustInfinispan(final PathAddress subsystem) throws Exception {
        final List<ModelNode> list = new ArrayList<>();
        //Statistics need to be enabled for all cache containers and caches
        list.add(setStatisticsEnabledTrue(
                subsystem.append("cache-container", "server")));
        list.add(setStatisticsEnabledTrue(
                subsystem.append("cache-container", "server").append("replicated-cache", "default")));
        list.add(getWriteAttributeOperation(subsystem.append("cache-container", "server").append("transport", "TRANSPORT"), "stack", new ModelNode("udp ")));
        list.add(setStatisticsEnabledTrue(
                subsystem.append("cache-container", "web")));
        list.add(setStatisticsEnabledTrue(
                subsystem.append("cache-container", "web").append("distributed-cache", "dist")));
        list.add(setStatisticsEnabledTrue(
                subsystem.append("cache-container", "ejb")));
        list.add(setStatisticsEnabledTrue(
                subsystem.append("cache-container", "ejb").append("distributed-cache", "dist")));
        list.add(setStatisticsEnabledTrue(
                subsystem.append("cache-container", "hibernate")));
        list.add(setStatisticsEnabledTrue(
                subsystem.append("cache-container", "hibernate").append("invalidation-cache", "entity")));
        list.add(setStatisticsEnabledTrue(
                subsystem.append("cache-container", "hibernate").append("local-cache", "local-query")));
        list.add(setStatisticsEnabledTrue(
                subsystem.append("cache-container", "hibernate").append("replicated-cache", "timestamps")));
        return list;
    }




    private List<ModelNode> adjustJGroups(final PathAddress subsystem) throws Exception {
        final List<ModelNode> list = new ArrayList<>();
        //default-channel is not understood
        list.add(
                getUndefineAttributeOperation(
                        subsystem, "default-channel"));
        //In fact channels don't work
        list.add(createRemoveOperation(subsystem.append("channel", "ee")));

        // pbcast.NAKACK2 does not exist, use pbcast.NAKACK instead
        // UNICAST3 does not exist, use UNICAST2 instead
        //All this code is to remove and re-add the protocols including the rename to preserve the order
        //TODO once we support indexed adds, use those instead
        //udp
        PathAddress udp = subsystem.append("stack", "udp");
        list.add(createRemoveOperation(udp.append("protocol", "pbcast.NAKACK2")));
        list.add(createRemoveOperation(udp.append("protocol", "UNICAST3")));
        list.add(createRemoveOperation(udp.append("protocol", "pbcast.STABLE")));
        list.add(createRemoveOperation(udp.append("protocol", "pbcast.GMS")));
        list.add(createRemoveOperation(udp.append("protocol", "UFC")));
        list.add(createRemoveOperation(udp.append("protocol", "MFC")));
        list.add(createRemoveOperation(udp.append("protocol", "FRAG2")));
        list.add(createAddOperation(udp.append("protocol", "pbcast.NAKACK")));
        list.add(createAddOperation(udp.append("protocol", "UNICAST2")));
        list.add(createAddOperation(udp.append("protocol", "pbcast.STABLE")));
        list.add(createAddOperation(udp.append("protocol", "pbcast.GMS")));
        list.add(createAddOperation(udp.append("protocol", "UFC")));
        list.add(createAddOperation(udp.append("protocol", "MFC")));
        list.add(createAddOperation(udp.append("protocol", "FRAG2")));
        list.add(createAddOperation(udp.append("protocol", "RSVP")));

        //udp
        PathAddress tcp = subsystem.append("stack", "tcp");
        list.add(createRemoveOperation(tcp.append("protocol", "pbcast.NAKACK2")));
        list.add(createRemoveOperation(tcp.append("protocol", "UNICAST3")));
        list.add(createRemoveOperation(tcp.append("protocol", "pbcast.STABLE")));
        list.add(createRemoveOperation(tcp.append("protocol", "pbcast.GMS")));
        list.add(createRemoveOperation(tcp.append("protocol", "MFC")));
        list.add(createRemoveOperation(tcp.append("protocol", "FRAG2")));
        list.add(createAddOperation(tcp.append("protocol", "pbcast.NAKACK")));
        list.add(createAddOperation(tcp.append("protocol", "UNICAST2")));
        list.add(createAddOperation(tcp.append("protocol", "pbcast.STABLE")));
        list.add(createAddOperation(tcp.append("protocol", "pbcast.GMS")));
        list.add(createAddOperation(tcp.append("protocol", "UFC")));
        list.add(createAddOperation(tcp.append("protocol", "FRAG2")));
        list.add(createAddOperation(tcp.append("protocol", "RSVP")));

        return list;
    }



    private ModelNode setStatisticsEnabledTrue(final PathAddress addr) {
        return getWriteAttributeOperation(addr, "statistics-enabled", true);
    }

    @Override
    protected String getJaspiTestAuthModuleName() {
        return "org.jboss.as.web.security.jaspi.modules.HTTPBasicServerAuthModule";
    }
}
