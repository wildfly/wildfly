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

package org.jboss.as.test.integration.domain.mixed.eap620;

import java.util.List;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.domain.mixed.eap630.LegacyConfigAdjuster630;
import org.jboss.dmr.ModelNode;

/**
 * Adjusts a config produced from the EAP 6.2 domain.xml.
 *
 * @author Brian Stansberry
 */
public class LegacyConfigAdjuster620 extends LegacyConfigAdjuster630 {

    @Override
    protected List<ModelNode> adjustForVersion(DomainClient client, PathAddress profileAddress) throws Exception {
        List<ModelNode> result = super.adjustForVersion(client, profileAddress);

        enableDatasourceStatistics(profileAddress, result);

        // DO NOT INTRODUCE NEW ADJUSTMENTS HERE WITHOUT SOME SORT OF PUBLIC DISCUSSION.
        // CAREFULLY DOCUMENT IN THIS CLASS WHY ANY ADJUSTMENT IS NEEDED AND IS THE BEST APPROACH.
        // If an adjustment is needed here, that means our users will need to do the same
        // just to get an existing profile to work, and we want to minimize that.

        return result;
    }

    /**
     * EAP 6.3 switched the default behavior for datasource statistics from enabled to a configurable
     * disabled. If a config is being migrated, we want the new behavior, so we don't use the parser
     * to enable statistics when a legacy xsd is found. But the 6.2 slave cannot boot if the config
     * has statistics disabled, so we need to specifically enable them for the mixed domain case.
     */
    private void enableDatasourceStatistics(PathAddress profileAddress, List<ModelNode> ops) {
        PathAddress ds = profileAddress.append(
                PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, "datasources"),
                PathElement.pathElement("data-source", "ExampleDS")
        );
        ModelNode op = Util.getWriteAttributeOperation(ds, "statistics-enabled", true);
        ops.add(op);

    }
}
