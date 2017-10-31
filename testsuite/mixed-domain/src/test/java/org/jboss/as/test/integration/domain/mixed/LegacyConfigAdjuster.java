/*
Copyright 2016 Red Hat, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
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
import org.jboss.as.test.integration.domain.mixed.eap640.LegacyConfigAdjuster640;
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

        final LegacyConfigAdjuster adjuster;
        switch (asVersion) {
            case EAP_6_2_0:
            case EAP_6_3_0:
                throw new UnsupportedOperationException();
            case EAP_6_4_0:
                adjuster = new LegacyConfigAdjuster640();
                break;
            default:
                adjuster = new LegacyConfigAdjuster();
        }

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
        if (operations.size() == 0) {
            return;
        }
        for (ModelNode op : operations) {
            DomainTestUtils.executeForResult(op, client);
        }
    }
}
