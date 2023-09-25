/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.mixed.eap740;

import org.jboss.as.test.integration.domain.mixed.MixedDeploymentOverlayTestCase;
import org.jboss.as.test.integration.domain.mixed.Version;
import org.jboss.as.test.integration.domain.mixed.Version.AsVersion;
import org.junit.BeforeClass;

/**
 * @author Emmanuel Hugonnet (c) 2017 Red Hat, inc.
 */
@Version(AsVersion.EAP_7_4_0)
public class MixedDomainDeploymentOverlay740TestCase extends MixedDeploymentOverlayTestCase {
    @BeforeClass
    public static void beforeClass() {
        MixedDomainOverlay740TestSuite.initializeDomain();
        MixedDeploymentOverlayTestCase.setupDomain();
    }
}
