/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.elytron.oidc;

import java.util.EnumSet;
import java.util.Properties;

import org.jboss.as.subsystem.test.AbstractSubsystemSchemaTest;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * A test class that tests the subsystem parsing of each subsystem
 * version and stability.
 * @author <a href="mailto:prpaul@redhat.com">Prarthona Paul</a>
 */
@RunWith(Parameterized.class)
public class ElytronOidcClientSubsystemTestCase extends AbstractSubsystemSchemaTest<ElytronOidcSubsystemSchema> {

    @Parameters
    public static Iterable<ElytronOidcSubsystemSchema> parameters() {
        return EnumSet.allOf(ElytronOidcSubsystemSchema.class);
    }

    public ElytronOidcClientSubsystemTestCase(ElytronOidcSubsystemSchema schema) {
        super(ElytronOidcExtension.SUBSYSTEM_NAME, new ElytronOidcExtension(), schema, ElytronOidcSubsystemSchema.CURRENT.get(schema.getStability()));
    }

    @Override
    protected void compareXml(String configId, String original, String marshalled) {
        //
    }

    protected Properties getResolvedProperties() {
        return System.getProperties();
    }

}
