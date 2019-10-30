/*
 * Copyright 2017 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  *
 * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
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
 * Tests for messaging subsystem in standalone mode
 *
 * Created by rsvoboda on 12/13/16.
 */

@RunAsClient
@RunWith(Arquillian.class)
@Category(StandaloneTests.class)
public class MessagingTestCase extends TestBase {

    /*
     * append invalid element to subsystem definition
     * check that all elements are listed
     */
    @Test
    @ServerConfig(configuration = "standalone-full-ha.xml", xmlTransformationGroovy = "messaging/AddFooBar.groovy",
            subtreeName = "messaging", subsystemName = "messaging-activemq")
    public void modifyWsdlAddressElementWithNoValue() throws Exception {
        container().tryStartAndWaitForFail();

        String errorLog = container().getErrorMessageFromServerStart();
        assertDoesNotContain(errorLog, "more)");  // something like '(and 24 more)' shouldn't be in the log
        assertContains(errorLog, "<foo>bar</foo>");
        assertContains(errorLog, "^^^^ 'foo' isn't an allowed element here");
        assertContains(errorLog, "Elements allowed here are: ");
        assertContains(errorLog, "acceptor");
        assertContains(errorLog, "address-setting");
        assertContains(errorLog, "bindings-directory");
        assertContains(errorLog, "bridge");
        assertContains(errorLog, "broadcast-group");
        assertContains(errorLog, "cluster-connection");
        assertContains(errorLog, "connection-factory");
        assertContains(errorLog, "connector");
        assertContains(errorLog, "connector-service");
        assertContains(errorLog, "discovery-group");
        assertContains(errorLog, "divert");
        assertContains(errorLog, "grouping-handler");
        assertContains(errorLog, "http-acceptor");
        assertContains(errorLog, "http-connector");
        assertContains(errorLog, "in-vm-acceptor");
        assertContains(errorLog, "in-vm-connector");
        assertContains(errorLog, "jms-queue");
        assertContains(errorLog, "jms-topic");
        assertContains(errorLog, "journal-directory");
        assertContains(errorLog, "large-messages-directory");
        assertContains(errorLog, "legacy-connection-factory");
        assertContains(errorLog, "live-only");
        assertContains(errorLog, "paging-directory");
        assertContains(errorLog, "pooled-connection-factory");
        assertContains(errorLog, "queue");
        assertContains(errorLog, "remote-acceptor");
        assertContains(errorLog, "remote-connector");
        assertContains(errorLog, "replication-colocated");
        assertContains(errorLog, "replication-master");
        assertContains(errorLog, "replication-slave");
        assertContains(errorLog, "security-setting");
        assertContains(errorLog, "shared-store-colocated");
        assertContains(errorLog, "shared-store-master");
        assertContains(errorLog, "shared-store-slave");
    }

    /*
    * provide invalid value to address-full-policy which is enum - only allowed are PAGE,BLOCK,FAIL,DROP, try to use "PAGES"
    */
    @Test
    @ServerConfig(configuration = "standalone-full-ha.xml", xmlTransformationGroovy = "messaging/InvalidAddressSettingFullPolicy.groovy",
            subtreeName = "messaging", subsystemName = "messaging-activemq")
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
    @ServerConfig(configuration = "standalone-full-ha.xml")
    public void testInvalidTypeForServerIdInAcceptor() throws Exception {
        container().tryStartAndWaitForFail(
                (OfflineCommand) ctx -> ctx.client.apply(GroovyXmlTransform.of(DoNothing.class, "messaging/InvalidTypeForServerIdInAcceptor.groovy")
                        .subtree("messaging", Subtree.subsystem("messaging-activemq")).parameter("parameter", "not-int-value")
                        .build()));
        String errorLog = container().getErrorMessageFromServerStart();
        assertContains(errorLog, "OPVDX001: Validation error in standalone-full-ha.xml ---------------------------");
        assertContains(errorLog, "<in-vm-acceptor name=\"in-vm\" server-id=\"not-int-value\"/>");
        assertContains(errorLog, "^^^^ Wrong type for 'server-id'. Expected [INT] but was STRING. Couldn't");
        assertContains(errorLog, "convert \\\"not-int-value\\\" to [INT]");
    }

    /*
    * provide invalid type to server-id to in-vm-acceptor - put long to int
    */
    @Test
    @ServerConfig(configuration = "standalone-full-ha.xml")
    public void testLongInIntServerIdInAcceptor() throws Exception {
        container().tryStartAndWaitForFail(
                (OfflineCommand) ctx -> ctx.client.apply(GroovyXmlTransform.of(DoNothing.class, "messaging/InvalidTypeForServerIdInAcceptor.groovy")
                        .subtree("messaging", Subtree.subsystem("messaging-activemq")).parameter("parameter", "214748364700")
                        .build()));
        String errorLog = container().getErrorMessageFromServerStart();
        assertContains(errorLog, "OPVDX001: Validation error in standalone-full-ha.xml ---------------------------");
        assertContains(errorLog, "<in-vm-acceptor name=\"in-vm\" server-id=\"214748364700\"/>");
        assertContains(errorLog, "^^^^ Wrong type for 'server-id'. Expected [INT] but was STRING. Couldn't");
        assertContains(errorLog, "convert \\\"214748364700\\\" to [INT]");
    }

    /*
    * provide invalid type - too long value to long type
    */
    @Test
    @ServerConfig(configuration = "standalone-full-ha.xml")
    public void testTooLongValueForLongTypeInMaxSizeBytes() throws Exception {
        container().tryStartAndWaitForFail(
                (OfflineCommand) ctx -> ctx.client.apply(GroovyXmlTransform.of(DoNothing.class,
                        "messaging/InvalidValueForMaxSizeBytesInAddressSettings.groovy")
                        .subtree("messaging", Subtree.subsystem("messaging-activemq"))
                        .parameter("parameter", "1048576000000000000000000000000000000000")
                        .build()));
        String errorLog = container().getErrorMessageFromServerStart();
        assertContains(errorLog, "OPVDX001: Validation error in standalone-full-ha.xml");
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
    @ServerConfig(configuration = "standalone-full-ha.xml")
    public void testDoubleInLongTypeInMaxSizeBytes() throws Exception {
        container().tryStartAndWaitForFail(
                (OfflineCommand) ctx -> ctx.client.apply(GroovyXmlTransform.of(DoNothing.class,
                        "messaging/InvalidValueForMaxSizeBytesInAddressSettings.groovy")
                        .subtree("messaging", Subtree.subsystem("messaging-activemq")).parameter("parameter", "0.12345678")
                        .build()));
        String errorLog = container().getErrorMessageFromServerStart();
        assertContains(errorLog, "OPVDX001: Validation error in standalone-full-ha.xml ---------------------------");
        assertContains(errorLog, "<address-setting name=\"#\" dead-letter-address=\"jms.queue.DLQ\" " +
                "expiry-address=\"jms.queue.ExpiryQueue\" max-size-bytes=\"0.12345678\" page-size-bytes=\"2097152\" " +
                "message-counter-history-day-limit=\"10\" redistribution-delay=\"1000\" address-full-policy=\"PAGE\"/>");
        assertContains(errorLog, "^^^^ Wrong type for 'max-size-bytes'. Expected [LONG] but was STRING.");
        assertContains(errorLog, "Couldn't convert \\\"0.12345678\\\" to [LONG]");
    }

    /*
    * invalid order of elements - append security element to end of messaging-activemq subsystem
    */
    @Test
    @ServerConfig(configuration = "standalone-full-ha.xml", xmlTransformationGroovy = "messaging/AddSecurityElementToEndOfSubsystem.groovy",
            subtreeName = "messaging", subsystemName = "messaging-activemq")
    public void testWrongOrderOfElements() throws Exception {
        container().tryStartAndWaitForFail();
        String errorLog = container().getErrorMessageFromServerStart();
        assertContains(errorLog, "OPVDX001: Validation error in standalone-full-ha.xml ---------------------------");
        assertContains(errorLog, "<security enabled=\"false\"/>");
        assertContains(errorLog, "^^^^ 'security' isn't an allowed element here");
        assertContains(errorLog, "Elements allowed here are:");
        assertContains(errorLog, "acceptor                   jms-topic");
        assertContains(errorLog, "address-setting            journal-directory");
        assertContains(errorLog, "bindings-directory         large-messages-directory");
        assertContains(errorLog, "bridge                     legacy-connection-factory");
        assertContains(errorLog, "broadcast-group            live-only");
        assertContains(errorLog, "cluster-connection         paging-directory");
        assertContains(errorLog, "connection-factory         pooled-connection-factory");
        assertContains(errorLog, "connector                  queue");
        assertContains(errorLog, "connector-service          remote-acceptor");
        assertContains(errorLog, "discovery-group            remote-connector");
        assertContains(errorLog, "divert                     replication-colocated");
        assertContains(errorLog, "grouping-handler           replication-master");
        assertContains(errorLog, "http-acceptor              replication-slave");
        assertContains(errorLog, "http-connector             security-setting");
        assertContains(errorLog, "in-vm-acceptor             shared-store-colocated");
        assertContains(errorLog, "in-vm-connector            shared-store-master");
        assertContains(errorLog, "jms-queue                  shared-store-slave");
        assertContains(errorLog, "'security' is allowed in elements:");
        assertContains(errorLog, "- server > profile > {urn:jboss:domain:messaging-activemq:");
        assertContains(errorLog, "subsystem > server");
    }

    /*
    * missing required attribute in element - missing name in connector
    * Reported Issue: https://issues.jboss.org/browse/JBEAP-8437
    */
    @Test
    @ServerConfig(configuration = "standalone-full-ha.xml", xmlTransformationGroovy = "messaging/AddConnectorWithoutName.groovy",
            subtreeName = "messaging", subsystemName = "messaging-activemq")
    public void testFirstMissingRequiredAttributeInElement() throws Exception {
        container().tryStartAndWaitForFail();
        String errorLog = container().getErrorMessageFromServerStart();
        assertContains(errorLog, "OPVDX001: Validation error in standalone-full-ha.xml ---------------------------");
        assertContains(errorLog, "<http-connector socket-binding=\"http\" endpoint=\"http-acceptor\"/>");
        assertContains(errorLog, "^^^^ Missing required attribute(s): name");
        assertContains(errorLog, "WFLYCTL0133: Missing required attribute(s): name");
    }
}
