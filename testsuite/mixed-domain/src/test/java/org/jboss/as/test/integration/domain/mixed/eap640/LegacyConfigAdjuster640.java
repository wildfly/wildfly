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

package org.jboss.as.test.integration.domain.mixed.eap640;

import java.io.IOException;
import java.util.List;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.domain.management.util.DomainTestUtils;
import org.jboss.as.test.integration.domain.mixed.eap700.LegacyConfigAdjuster700;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;

/**
 * Adjusts a config produced from the EAP 6.4 or earlier domain.xml.
 *
 * @author Brian Stansberry
 */
public class LegacyConfigAdjuster640 extends LegacyConfigAdjuster700 {
    @Override
    protected List<ModelNode> adjustForVersion(DomainClient client, PathAddress profileAddress) throws Exception {
        List<ModelNode> result = super.adjustForVersion(client, profileAddress);

        configureCDI10(profileAddress, result);
        removeBV(client, profileAddress, result);

        // DO NOT INTRODUCE NEW ADJUSTMENTS HERE WITHOUT SOME SORT OF PUBLIC DISCUSSION.
        // CAREFULLY DOCUMENT IN THIS CLASS WHY ANY ADJUSTMENT IS NEEDED AND IS THE BEST APPROACH.
        // If an adjustment is needed here, that means our users will need to do the same
        // just to get an existing profile to work, and we want to minimize that.


        return result;
    }

    /**
     *  EAP 6.x uses CDI 1.0 while later releases use later CDI spec versions. If a
     *  profile config uses CDI, it's unclear whether what's wanted is CDI 1.0 behavior
     *  or later behavior, so we require the user to be explicit via setting 2 attributes
     *  to get the 1.0 behavior.
     *
     *  The CDI subsystem parser could do this automatically when a legacy xsd is found, but
     *  then if the user goal is to migrate to the later behavior, they won't get it.
     */
    private void configureCDI10(PathAddress profileAddress, List<ModelNode> ops) {
        PathAddress weld = profileAddress.append(
                PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, "weld")
        );
        ModelNode op = Util.getWriteAttributeOperation(weld, "require-bean-descriptor", true);
        ops.add(op);

        op = op.clone();
        op.get("name").set("non-portable-mode");
        ops.add(op);
    }

    /**
     * WildFly 9 and later broke BV out from the ee subsystem, allowing it to be removed. If a later
     * DC/standalone server sees ee with a legacy xsd, it adds BV into the config, thus assuring that
     * a migrated config does not, perhaps not noticeably, lose existing behavior. But a 6.x slave
     * will not recognize the BV extension/subsystem, so it needs to be removed.
     */
    private void removeBV(DomainClient client, PathAddress profileAddress, List<ModelNode> ops) throws IOException, MgmtOperationException {
        PathAddress bv = profileAddress.append(
                PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, "bean-validation")
        );
        ops.add(Util.createRemoveOperation(bv));

        // If no other profiles still use this extension, we can remove it
        String ourProfile = profileAddress.getLastElement().getValue();
        ModelNode read = Util.createEmptyOperation("read-children-names", PathAddress.EMPTY_ADDRESS);
        read.get("child-type").set("profile");
        for (ModelNode profileMN : DomainTestUtils.executeForResult(read, client).asList()) {
            String profile = profileMN.asString();
            if (!ourProfile.equals(profile)) {
                read = Util.createEmptyOperation("read-children-names", PathAddress.pathAddress("profile", profile));
                read.get("child-type").set("subsystem");
                for (ModelNode sub : DomainTestUtils.executeForResult(read, client).asList()) {
                    if ("bean-validation".equals(sub.asString())) {
                        // this profile still has a subsystem; don't remove extension yet
                        return;
                    }
                }
            }
        }

        // If we got here, no profile has the bv subsystem so we can remove the extension
        ops.add(Util.createRemoveOperation(PathAddress.pathAddress("extension", "org.wildfly.extension.bean-validation")));

    }
}
