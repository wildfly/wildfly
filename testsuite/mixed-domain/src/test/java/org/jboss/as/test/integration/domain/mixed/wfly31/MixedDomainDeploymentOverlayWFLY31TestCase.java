/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.mixed.wfly31;

import org.jboss.as.test.integration.domain.mixed.MixedDeploymentOverlayTestCase;
import org.jboss.as.test.integration.domain.mixed.Version;
import org.jboss.as.test.integration.domain.mixed.Version.AsVersion;
import org.junit.BeforeClass;

/**
 * @author Emmanuel Hugonnet (c) 2017 Red Hat, inc.
 */
@Version(AsVersion.WFLY_31_0_0)
public class MixedDomainDeploymentOverlayWFLY31TestCase extends MixedDeploymentOverlayTestCase {
    @BeforeClass
    public static void beforeClass() {
        MixedDomainOverlayWFLY31TestSuite.initializeDomain();
        MixedDeploymentOverlayTestCase.setupDomain();
    }
}
