/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.mixed.eap810;

import static org.jboss.as.test.integration.domain.mixed.Version.AsVersion.EAP_8_1_0;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.jboss.as.test.integration.domain.mixed.DomainHostExcludesTest;
import org.jboss.as.test.integration.domain.mixed.Version;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.junit.BeforeClass;

/**
 * Tests of the ability of a DC to exclude resources from visibility to an EAP 8.1.0 secondary.
 */
@Version(EAP_8_1_0)
public class DomainHostExcludes810TestCase extends DomainHostExcludesTest {

    @BeforeClass
    public static void beforeClass() throws InterruptedException, TimeoutException, MgmtOperationException, IOException {
        LegacyConfig810TestSuite.initializeDomain();
        setup(DomainHostExcludes810TestCase.class, EAP_8_1_0.getHostExclude(), EAP_8_1_0.getModelVersion());
    }
}
