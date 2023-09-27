/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.mixed.eap740;

import static org.jboss.as.test.integration.domain.mixed.Version.AsVersion.EAP_7_4_0;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.jboss.as.test.integration.domain.mixed.DomainHostExcludesTest;
import org.jboss.as.test.integration.domain.mixed.Version;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.junit.BeforeClass;

/**
 * Tests of the ability of a DC to exclude resources from visibility to an EAP 7.4.0 secondary.
 *
 * @author Brian Stansberry
 */
@Version(EAP_7_4_0)
public class DomainHostExcludes740TestCase extends DomainHostExcludesTest {

    @BeforeClass
    public static void beforeClass() throws InterruptedException, TimeoutException, MgmtOperationException, IOException {
        LegacyConfig740TestSuite.initializeDomain();
        setup(DomainHostExcludes740TestCase.class, EAP_7_4_0.getHostExclude(), EAP_7_4_0.getModelVersion());
    }
}
