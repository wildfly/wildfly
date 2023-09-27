/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.vdx.domain;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.wildfly.extras.creaper.commands.foundation.offline.xml.GroovyXmlTransform;
import org.wildfly.extras.creaper.commands.foundation.offline.xml.Subtree;
import org.wildfly.extras.creaper.core.offline.OfflineCommand;
import org.wildfly.test.integration.vdx.TestBase;
import org.wildfly.test.integration.vdx.category.DomainTests;
import org.wildfly.test.integration.vdx.transformations.DoNothing;
import org.wildfly.test.integration.vdx.utils.server.ServerConfig;

/**
 * Tests for messaging subsystem in domain mode
 *
 * Created by mnovak on 1/19/17.
 */

@RunAsClient
@RunWith(Arquillian.class)
@Category(DomainTests.class)
public class MessagingDomainTestCase extends TestBase {

    @BeforeClass
    public static void noPreview() {
        // WildFly Preview doesn't configure a messaging broker
        AssumeTestGroupUtil.assumeNotWildFlyPreview();
    }

    /*
    * provide invalid value to address-full-policy which is enum - only allowed are PAGE,BLOCK,FAIL,DROP, try to use "PAGES"
    */
    @Test
    @ServerConfig(configuration = "domain.xml", xmlTransformationGroovy = "messaging/InvalidAddressSettingFullPolicy.groovy",
            subtreeName = "messaging", subsystemName = "messaging-activemq", profileName = "full-ha")
    public void testInvalidEnumValueInAddressSettingsFullPolicy() throws Exception {
        container().tryStartAndWaitForFail();
        String errorLog = container().getErrorMessageFromServerStart();
        assertContains(errorLog, "^^^^ Invalid value PAGES for address-full-policy; legal values are [BLOCK");
        assertContains(errorLog, "PAGE, FAIL, DROP]");
        assertContains(errorLog, "\"WFLYCTL0248: Invalid value PAGES for address-full-policy");
    }

    /*
    * provide invalid type to server-id to in-vm-acceptor - put string to int
    */
    @Test
    @ServerConfig(configuration = "domain.xml", profileName = "full-ha")
    public void testInvalidTypeForServerIdInAcceptor() throws Exception {
        container().tryStartAndWaitForFail(
                (OfflineCommand) ctx -> ctx.client.apply(GroovyXmlTransform.of(DoNothing.class,
                        "messaging/InvalidTypeForServerIdInAcceptor.groovy")
                        .subtree("messaging", Subtree.subsystemInProfile("full-ha", "messaging-activemq"))
                        .parameter("parameter", "not-int-value")
                        .build()));
        String errorLog = container().getErrorMessageFromServerStart();
        assertContains(errorLog, "OPVDX001: Validation error in domain.xml ---------------------------");
        assertContains(errorLog, "<in-vm-acceptor name=\"in-vm\" server-id=\"not-int-value\"/>");
        assertContains(errorLog, "^^^^ Wrong type for 'server-id'. Expected [INT] but was STRING. Couldn't");
        assertContains(errorLog, "convert \\\"not-int-value\\\" to [INT]");
    }

    /*
    * provide invalid type to server-id to in-vm-acceptor - put long to int
    */
    @Test
    @ServerConfig(configuration = "domain.xml", profileName = "full-ha")
    public void testLongInIntServerIdInAcceptor() throws Exception {
        container().tryStartAndWaitForFail(
                (OfflineCommand) ctx -> ctx.client.apply(GroovyXmlTransform.of(DoNothing.class,
                        "messaging/InvalidTypeForServerIdInAcceptor.groovy")
                        .subtree("messaging", Subtree.subsystemInProfile("full-ha", "messaging-activemq"))
                        .parameter("parameter", "214748364700")
                        .build()));
        String errorLog = container().getErrorMessageFromServerStart();
        assertContains(errorLog, "OPVDX001: Validation error in domain.xml ---------------------------");
        assertContains(errorLog, "<in-vm-acceptor name=\"in-vm\" server-id=\"214748364700\"/>");
        assertContains(errorLog, "^^^^ Wrong type for 'server-id'. Expected [INT] but was STRING. Couldn't");
        assertContains(errorLog, "convert \\\"214748364700\\\" to [INT]");
    }

    /*
    * provide invalid type - too long value to long type
    */
    @Test
    @ServerConfig(configuration = "domain.xml", profileName = "full-ha")
    public void testTooLongValueForLongTypeInMaxSizeBytes() throws Exception {
        container().tryStartAndWaitForFail(
                (OfflineCommand) ctx -> ctx.client.apply(GroovyXmlTransform.of(DoNothing.class,
                        "messaging/InvalidValueForMaxSizeBytesInAddressSettings.groovy")
                        .subtree("messaging", Subtree.subsystemInProfile("full-ha", "messaging-activemq"))
                        .parameter("parameter", "1048576000000000000000000000000000000000")
                        .build()));
        String errorLog = container().getErrorMessageFromServerStart();
        assertContains(errorLog, "OPVDX001: Validation error in domain.xml");
        assertContains(errorLog, "<address-setting name=\"#\" dead-letter-address=\"jms.queue.DLQ\" " +
                "expiry-address=\"jms.queue.ExpiryQueue\" max-size-bytes=\"1048576000000000000000000000000000000000\" " +
                "page-size-bytes=\"2097152\" message-counter-history-day-limit=\"10\" redistribution-delay=\"1000\" " +
                "address-full-policy=\"PAGE\"/>");
        assertContains(errorLog, " ^^^^ Wrong type for 'max-size-bytes'. Expected [LONG] but was STRING.");
        assertContains(errorLog, "Couldn't convert \\\"1048576000000000000000000000000000000000\\\" to");
        assertContains(errorLog, "[LONG]");
    }

    /*
    * provide invalid type - test double in long
    */
    @Test
    @ServerConfig(configuration = "domain.xml")
    public void testDoubleInLongTypeInMaxSizeBytes() throws Exception {
        container().tryStartAndWaitForFail(
                (OfflineCommand) ctx -> ctx.client.apply(GroovyXmlTransform.of(DoNothing.class,
                        "messaging/InvalidValueForMaxSizeBytesInAddressSettings.groovy")
                        .subtree("messaging", Subtree.subsystemInProfile("full-ha", "messaging-activemq")).parameter("parameter", "0.12345678")
                        .build()));
        String errorLog = container().getErrorMessageFromServerStart();
        assertContains(errorLog, "OPVDX001: Validation error in domain.xml ---------------------------");
        assertContains(errorLog, "<address-setting name=\"#\" dead-letter-address=\"jms.queue.DLQ\" " +
                "expiry-address=\"jms.queue.ExpiryQueue\" max-size-bytes=\"0.12345678\" page-size-bytes=\"2097152\" " +
                "message-counter-history-day-limit=\"10\" redistribution-delay=\"1000\" address-full-policy=\"PAGE\"/>");
        assertContains(errorLog, "^^^^ Wrong type for 'max-size-bytes'. Expected [LONG] but was STRING.");
        assertContains(errorLog, "Couldn't convert \\\"0.12345678\\\" to [LONG]");
    }
}
