/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.vdx.standalone;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.vdx.TestBase;
import org.wildfly.test.integration.vdx.category.StandaloneTests;
import org.wildfly.test.integration.vdx.utils.server.ServerConfig;

/**
 * Test cases for Elytron subsystem configuration
 *
 * Created by rsvoboda on 2/4/17.
 */
@RunAsClient
@RunWith(Arquillian.class)
@Category(StandaloneTests.class)
public class ElytronTestCase extends TestBase {

    /*
     * misplaced 'plain-text' attribute for properties-realm definition
     */
    @Test
    @ServerConfig(configuration = "standalone.xml", xmlTransformationGroovy = "elytron/MisplacedAttributeForPropertiesRealm.groovy",
            subtreeName = "elytron", subsystemName = "elytron")
    public void misplacedAttributeForPropertiesRealm()throws Exception {
        container().tryStartAndWaitForFail();

        String errorLog = container().getErrorMessageFromServerStart();
        assertContains(errorLog, "OPVDX001: Validation error in standalone.xml");
        assertContains(errorLog, "^^^^ 'plain-text' isn't an allowed attribute for the 'properties-realm'");
        assertContains(errorLog, "Attributes allowed here are: ");
        assertContains(errorLog, "groups-attribute");
        assertContains(errorLog, "name");
        assertContains(errorLog, "groups-properties");
        assertContains(errorLog, "hash-charset");
        assertContains(errorLog, "hash-encoding");
        assertContains(errorLog, "users-properties");
        assertContains(errorLog, "'plain-text' is allowed on elements:");
        assertContains(errorLog, "server > profile > {urn:wildfly:elytron");
        assertContains(errorLog, "subsystem > security-realms > properties-realm > users-properties");

    }
}
