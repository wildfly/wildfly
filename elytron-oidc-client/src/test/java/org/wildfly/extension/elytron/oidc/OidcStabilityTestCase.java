/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.elytron.oidc;

import javax.xml.stream.XMLStreamException;

import org.wildfly.extension.elytron.oidc.OidcTestCase.DefaultInitializer;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.version.Stability;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class OidcStabilityTestCase extends AbstractSubsystemTest {

    private OidcConfigService configService;
    private KernelServices services = null;

    public OidcStabilityTestCase() {
        super(ElytronOidcExtension.SUBSYSTEM_NAME, new ElytronOidcExtension());
    }

    @Before
    public void prepare() throws Throwable {
        if (services != null) services = null;
        services = super.createKernelServicesBuilder(new DefaultInitializer(Stability.DEFAULT)).build();
        if (! services.isSuccessfulBoot()) {
            Assert.fail(services.getBootError().toString());
        }
        configService = OidcConfigService.getInstance();
    }
    @Test
    public void testWithIncorrectStability() {
        try {
            String subsystemXml = "oidc.xml";
            services = super.createKernelServicesBuilder(new DefaultInitializer(Stability.DEFAULT)).setSubsystemXmlResource(subsystemXml).build();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof XMLStreamException);
        }
    }
}
