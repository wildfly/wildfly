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
import org.wildfly.extras.creaper.commands.foundation.offline.xml.GroovyXmlTransform;
import org.wildfly.extras.creaper.commands.foundation.offline.xml.Subtree;
import org.wildfly.extras.creaper.core.offline.OfflineCommand;
import org.wildfly.test.integration.vdx.TestBase;
import org.wildfly.test.integration.vdx.category.StandaloneTests;
import org.wildfly.test.integration.vdx.transformations.DoNothing;
import org.wildfly.test.integration.vdx.utils.server.ServerConfig;

/**
 *
 * Created by rsvoboda on 08/24/17.
 */

@RunAsClient
@RunWith(Arquillian.class)
@Category(StandaloneTests.class)
public class LoggingTestCase extends TestBase {

    /*
     * Duplicate logger category
     */
    @Test
    @ServerConfig()
    public void duplicateLoggerCategory() throws Exception {
        container().tryStartAndWaitForFail(
                (OfflineCommand) ctx -> ctx.client.apply(GroovyXmlTransform.of(DoNothing.class, "AddElement.groovy")
                        .subtree("path", Subtree.subsystem("logging")).parameter("elementXml",
                                  "            <logger category=\"sun.rmi\">\n" +
                                  "                <level name=\"WARN\"/>\n" +
                                  "            </logger>")
                        .build()));
        String errorLog = container().getErrorMessageFromServerStart();
        assertContains(errorLog, "OPVDX001: Validation error in standalone.xml");
        assertContains(errorLog, "^^^^ 'logger' with a name of 'sun.rmi' can't appear more than once");
        assertContains(errorLog, "An element of this type named 'sun.rmi' has");
        assertContains(errorLog, "WFLYCTL0073");
    }

    /*
     * No package specified, just empty sting in category attribute
     */
    @Test
    @ServerConfig()
    public void invalidLoggerCategoryValue() throws Exception {
        container().tryStartAndWaitForFail(
                (OfflineCommand) ctx -> ctx.client.apply(GroovyXmlTransform.of(DoNothing.class, "AddElement.groovy")
                        .subtree("path", Subtree.subsystem("logging")).parameter("elementXml",
                                "            <logger category=\"\">\n" +
                                "                <level name=\"WARN\"/>\n" +
                                "            </logger>")
                        .build()));
        String errorLog = container().getErrorMessageFromServerStart();
        assertContains(errorLog, "OPVDX001: Validation error in standalone.xml");
        assertContains(errorLog, "^^^^ '' isn't a valid value for the 'category' attribute");
        assertContains(errorLog, "WFLYCTL0106: Invalid value '' for attribute 'category'");
    }

    /*
     * No category attribute specified
     */
    @Test
    @ServerConfig()
    public void noLoggerCategory() throws Exception {
        container().tryStartAndWaitForFail(
                (OfflineCommand) ctx -> ctx.client.apply(GroovyXmlTransform.of(DoNothing.class, "AddElement.groovy")
                        .subtree("path", Subtree.subsystem("logging")).parameter("elementXml",
                                "            <logger>\n" +
                                "                <level name=\"WARN\"/>\n" +
                                "            </logger>")
                        .build()));
        String errorLog = container().getErrorMessageFromServerStart();
        assertContains(errorLog, "WFLYCTL0133: Missing required attribute(s): CATEGORY");
    }
}
