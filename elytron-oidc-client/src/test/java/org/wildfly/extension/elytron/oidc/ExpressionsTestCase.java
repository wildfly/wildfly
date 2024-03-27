/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.elytron.oidc;

import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.version.Stability;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Subsystem parsing test case.
 *
 * <a href="mailto:araskar@redhat.com">Ashpan Raskar</a>
 */
@RunWith(Parameterized.class)
public class ExpressionsTestCase extends AbstractSubsystemTest {

    private KernelServices services = null;

    @Parameterized.Parameters
    public static Iterable<ElytronOidcSubsystemSchema> parameters() {
        return ElytronOidcSubsystemSchema.CURRENT.values();
    }
    public ExpressionsTestCase(ElytronOidcSubsystemSchema schema) {
        super(ElytronOidcExtension.SUBSYSTEM_NAME, new ElytronOidcExtension());
    }

    @Test
    public void testExpressions() throws Throwable {
        if (services != null) return;
        String subsystemXml = "oidc-expressions.xml";
        services = super.createKernelServicesBuilder(new OidcTestCase.DefaultInitializer(Stability.COMMUNITY)).setSubsystemXmlResource(subsystemXml).build();
        if (! services.isSuccessfulBoot()) {
            Assert.fail(services.getBootError().toString());
        }
    }

}