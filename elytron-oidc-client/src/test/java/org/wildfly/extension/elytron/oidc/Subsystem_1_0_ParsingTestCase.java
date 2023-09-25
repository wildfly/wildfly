/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.elytron.oidc;

import java.io.IOException;
import java.util.Properties;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;

/**
 * Subsystem parsing test case.
 *
 * <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
public class Subsystem_1_0_ParsingTestCase extends AbstractSubsystemBaseTest {

    public Subsystem_1_0_ParsingTestCase() {
        super(ElytronOidcExtension.SUBSYSTEM_NAME, new ElytronOidcExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("legacy_subsystem_1_0.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws IOException {
        return "schema/wildfly-elytron-oidc-client_1_0.xsd";
    }

    @Override
    protected void compareXml(String configId, String original, String marshalled) throws Exception {
        //
    }

    protected Properties getResolvedProperties() {
        return System.getProperties();
    }
}
