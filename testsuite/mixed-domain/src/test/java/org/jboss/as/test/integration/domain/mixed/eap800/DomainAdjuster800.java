/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.mixed.eap800;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.test.integration.domain.mixed.DomainAdjuster;
import org.jboss.dmr.ModelNode;

/**
 * Does adjustments to the domain model for 8.0.0 legacy secondary hosts.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class DomainAdjuster800 extends DomainAdjuster {

    @Override
    protected List<ModelNode> adjustForVersion(final DomainClient client, PathAddress profileAddress, boolean withPrimaryServers) {
        return new ArrayList<>();
    }

    @Override
    protected void adjustExpansionExtensions(DomainClient client, PathAddress profileAddress) throws Exception {
        removeSubsystemExtensionIfExist(client, profileAddress.append(SUBSYSTEM, "microprofile-jwt-smallrye"), PathAddress.pathAddress(EXTENSION, "org.wildfly.extension.microprofile.jwt-smallrye"));
        removeSubsystemExtensionIfExist(client, profileAddress.append(SUBSYSTEM, "microprofile-config-smallrye"), PathAddress.pathAddress(EXTENSION, "org.wildfly.extension.microprofile.config-smallrye"));
    }
}
