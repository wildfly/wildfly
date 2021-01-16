/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.domain.mixed.eap720;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.operations.common.Util.createRemoveOperation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.test.integration.domain.mixed.eap730.DomainAdjuster730;
import org.jboss.dmr.ModelNode;

/**
 * Does adjustments to the domain model for 7.2.0 legacy slaves.
 *
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class DomainAdjuster720 extends DomainAdjuster730 {

    @Override
    protected List<ModelNode> adjustForVersion(final DomainClient client, PathAddress profileAddress, boolean withMasterServers) throws Exception {
        final List<ModelNode> list = super.adjustForVersion(client, profileAddress, withMasterServers);

        list.addAll(removeDistributableWeb(profileAddress.append(SUBSYSTEM, "distributable-web")));

        return list;
    }

    private static Collection<? extends ModelNode> removeDistributableWeb(final PathAddress subsystem) {
        final List<ModelNode> list = new ArrayList<>();
        // distributable-web subsystem and extension don't exist so remove them
        list.add(createRemoveOperation(subsystem));
        list.add(createRemoveOperation(PathAddress.pathAddress(EXTENSION, "org.wildfly.extension.clustering.web")));
        return list;
    }
}
