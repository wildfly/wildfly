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

package org.jboss.as.test.integration.domain.mixed.eap620;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.operations.common.Util.createRemoveOperation;
import static org.jboss.as.controller.operations.common.Util.getUndefineAttributeOperation;
import static org.jboss.as.controller.operations.common.Util.getWriteAttributeOperation;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.test.integration.domain.mixed.eap630.DomainAdjuster630;
import org.jboss.dmr.ModelNode;

/**
 * Does adjustments to the domain model for 6.2.0 legacy slaves
 *
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class DomainAdjuster620 extends DomainAdjuster630 {

    @Override
    protected List<ModelNode> adjustForVersion(final DomainClient client, PathAddress profileAddress, boolean withMasterServers) throws Exception {
        List<ModelNode> list = super.adjustForVersion(client, profileAddress, withMasterServers);
        list.addAll(adjustLogging(profileAddress.append(SUBSYSTEM, "logging")));
        list.add(getWriteAttributeOperation(profileAddress.append(SUBSYSTEM, "datasources").append("data-source","ExampleDS"), "statistics-enabled", new ModelNode("true")));
        return list;
    }

    private List<ModelNode> adjustLogging(PathAddress subsystem) throws Exception {
        List<ModelNode> list = new ArrayList<>();

        //Named formatters don't exist
        list.add(getUndefineAttributeOperation(subsystem.append("console-handler", "CONSOLE"), "named-formatter"));
        list.add(getWriteAttributeOperation(subsystem.append("console-handler", "CONSOLE"), "formatter",
                new ModelNode("%d{HH:mm:ss,SSS} %-5p [%c] (%t) %s%E%n")));
        list.add(getUndefineAttributeOperation(subsystem.append("periodic-rotating-file-handler", "FILE"), "named-formatter"));
        list.add(getWriteAttributeOperation(subsystem.append("periodic-rotating-file-handler", "FILE"), "formatter", new ModelNode("%d{HH:mm:ss,SSS} %-5p [%c] (%t) %s%E%n")));
        list.add(createRemoveOperation(subsystem.append("pattern-formatter", "PATTERN")));
        list.add(createRemoveOperation(subsystem.append("pattern-formatter", "COLOR-PATTERN")));

        return list;
    }

    @Override
    public void adjustInfinispanStatisticsEnabled(final List<ModelNode> list, final PathAddress subsystem, boolean isFullHa) {
        //Statistics need to be enabled for all cache containers and caches
        list.add(setStatisticsEnabledTrue(subsystem.append("cache-container", "server")));
        list.add(setStatisticsEnabledTrue(subsystem.append("cache-container", "web")));
        list.add(setStatisticsEnabledTrue(subsystem.append("cache-container", "ejb")));
        list.add(setStatisticsEnabledTrue(subsystem.append("cache-container", "hibernate")));
        if (isFullHa) {
            list.add(setStatisticsEnabledTrue(
                    subsystem.append("cache-container", "server").append("replicated-cache", "default")));
            list.add(setStatisticsEnabledTrue(
                    subsystem.append("cache-container", "web").append("distributed-cache", "dist")));

            list.add(setStatisticsEnabledTrue(
                    subsystem.append("cache-container", "ejb").append("distributed-cache", "dist")));

            list.add(setStatisticsEnabledTrue(
                    subsystem.append("cache-container", "hibernate").append("invalidation-cache", "entity")));
            list.add(setStatisticsEnabledTrue(
                    subsystem.append("cache-container", "hibernate").append("local-cache", "local-query")));
            list.add(setStatisticsEnabledTrue(
                    subsystem.append("cache-container", "hibernate").append("replicated-cache", "timestamps")));
        } else {
            list.add(setStatisticsEnabledTrue(subsystem.append("cache-container", "server").append("local-cache", "default")));

            list.add(setStatisticsEnabledTrue(subsystem.append("cache-container", "web").append("local-cache", "passivation")));

            list.add(setStatisticsEnabledTrue(subsystem.append("cache-container", "ejb").append("local-cache", "passivation")));

            list.add(setStatisticsEnabledTrue(subsystem.append("cache-container", "hibernate").append("local-cache", "entity")));
            list.add(setStatisticsEnabledTrue(subsystem.append("cache-container", "hibernate").append("local-cache", "local-query")));
            list.add(setStatisticsEnabledTrue(subsystem.append("cache-container", "hibernate").append("local-cache", "timestamps")));
        }
    }

    private ModelNode setStatisticsEnabledTrue(final PathAddress addr) {
        return getWriteAttributeOperation(addr, "statistics-enabled", true);
    }

}