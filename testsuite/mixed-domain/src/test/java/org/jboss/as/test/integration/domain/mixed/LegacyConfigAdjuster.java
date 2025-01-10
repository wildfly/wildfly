/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.mixed;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.domain.management.util.DomainTestUtils;
import org.jboss.dmr.ModelNode;

/**
 * Analogue to {@link DomainAdjuster}, but for use when the domain is running with a legacy domain.xml
 * rather than the current standard domain.xml. So the scope of expected adjustments is expected to be
 * considerably smaller.
 *
 * @author Brian Stansberry
 */
public class LegacyConfigAdjuster {

    protected LegacyConfigAdjuster() {
    }

    static void adjustForVersion(final DomainClient client, final Version.AsVersion asVersion) throws Exception {

        final LegacyConfigAdjuster adjuster = new LegacyConfigAdjuster();

        adjuster.adjust(client);
    }

    final void adjust(final DomainClient client) throws Exception {

        removeIpv4SystemProperty(client);

        //Version specific changes
        ModelNode read = Util.createEmptyOperation(READ_CHILDREN_NAMES_OPERATION, PathAddress.EMPTY_ADDRESS);
        read.get(CHILD_TYPE).set(PROFILE);
        ModelNode result = DomainTestUtils.executeForResult(read, client);
        for (ModelNode profile : result.asList()) {
            final List<ModelNode> adjustments = adjustForVersion(client, PathAddress.pathAddress(PROFILE, profile.asString()));
            applyVersionAdjustments(client, adjustments);
        }
    }

    private void removeIpv4SystemProperty(final DomainClient client) throws Exception {
        //The standard domain configuration contains -Djava.net.preferIPv4Stack=true, remove that
        DomainTestUtils.executeForResult(
                Util.createRemoveOperation(PathAddress.pathAddress(SYSTEM_PROPERTY, "java.net.preferIPv4Stack")), client);

    }

    protected List<ModelNode> adjustForVersion(final DomainClient client, final PathAddress profileAddress) throws  Exception {
        return new ArrayList<>();
    }

    private void applyVersionAdjustments(DomainClient client, List<ModelNode> operations) throws Exception {
        if (operations.isEmpty()) {
            return;
        }
        for (ModelNode op : operations) {
            DomainTestUtils.executeForResult(op, client);
        }
    }
}
