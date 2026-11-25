/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.mixed.wfly31;

import org.jboss.as.test.integration.domain.mixed.MixedDomainTestSuite;
import org.jboss.as.test.integration.domain.mixed.Version;
import org.jboss.as.test.integration.domain.mixed.Version.AsVersion;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * @author Emmanuel Hugonnet (c) 2017 Red Hat, inc.
 */
@RunWith(Suite.class)
@SuiteClasses(value= {MixedDomainDeploymentOverlayWFLY31TestCase.class})
@Version(AsVersion.WFLY_31_0_0)
public class MixedDomainOverlayWFLY31TestSuite extends MixedDomainTestSuite {

    @BeforeClass
    public static void initializeDomain() {
        MixedDomainTestSuite.getSupport(MixedDomainOverlayWFLY31TestSuite.class, "primary-config/host.xml", "secondary-config/host-secondary-overlay.xml", Profile.DEFAULT, true, false, true);
    }
}
