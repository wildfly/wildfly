/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.domain.mixed.eap710;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTO_START;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.domain.mixed.DomainAdjuster;
import org.jboss.dmr.ModelNode;

/**
 * Does adjustments to the domain model for 7.1.0 legacy slaves.
 *
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class DomainAdjuster710 extends DomainAdjuster {

    @Override
    protected List<ModelNode> adjustForVersion(final DomainClient client, PathAddress profileAddress, boolean withMasterServers) throws Exception {
        final List<ModelNode> list = new ArrayList<>();

        adjustUndertow(profileAddress.append(SUBSYSTEM, "undertow"), list);

        if (withMasterServers) {
            list.addAll(reconfigureServers());
        }

        return list;
    }

    private void adjustUndertow(PathAddress undertow, List<ModelNode> ops) {
        // EAP 7.0 and earlier required explicit SSL configuration. Wildfly 10.1 added support
        // for SSL by default, which automatically generates certs.
        // This could be removed if all hosts were configured to contain a security domain with SSL
        // enabled.
        final PathAddress httpsListener = undertow
                .append("server", "default-server")
                .append("https-listener", "https");
        ops.add(Util.getEmptyOperation(ModelDescriptionConstants.REMOVE, httpsListener.toModelNode()));
    }

    private Collection<? extends ModelNode> reconfigureServers() {
        final List<ModelNode> list = new ArrayList<>();
        //Reconfigure master servers
        final PathAddress masterHostAddress = PathAddress.pathAddress(HOST, "master");
        list.add(Util.getWriteAttributeOperation(masterHostAddress.append(SERVER_CONFIG, "server-one"), AUTO_START, true));
        list.add(Util.getWriteAttributeOperation(masterHostAddress.append(SERVER_CONFIG, "server-one"), ModelDescriptionConstants.GROUP, "main-server-group"));
        list.add(Util.getUndefineAttributeOperation(masterHostAddress.append(SERVER_CONFIG, "server-one"), ModelDescriptionConstants.SOCKET_BINDING_PORT_OFFSET));
        list.add(Util.getWriteAttributeOperation(masterHostAddress.append(SERVER_CONFIG, "server-two"), AUTO_START, true));
        list.add(Util.getWriteAttributeOperation(masterHostAddress.append(SERVER_CONFIG, "server-two"), ModelDescriptionConstants.SOCKET_BINDING_PORT_OFFSET, 100));
        return list;
    }

}
