/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.dist.subsystem.xml;

import org.junit.Test;
import org.wildfly.test.distribution.validation.AbstractValidationUnitTest;

/**
 * Performs validation against their xsd of the server configuration files in the distribution.
 *
 * @author Brian Stansberry
 */
public class StandardConfigsXMLValidationUnitTestCase extends AbstractValidationUnitTest {

    @Test
    public void testHost() throws Exception {
        parseXml("domain/configuration/host.xml");
    }

    @Test
    public void testHostSecondary() throws Exception {
        parseXml("domain/configuration/host-secondary.xml");
    }

    @Test
    public void testHostPrimary() throws Exception {
        parseXml("domain/configuration/host-primary.xml");
    }

    @Test
    public void testDomain() throws Exception {
        parseXml("domain/configuration/domain.xml");
    }

    @Test
    public void testStandalone() throws Exception {
        parseXml("standalone/configuration/standalone.xml");
    }

    @Test
    public void testStandaloneHA() throws Exception {
        parseXml("standalone/configuration/standalone-ha.xml");
    }

    @Test
    public void testStandaloneFull() throws Exception {
        parseXml("standalone/configuration/standalone-full.xml");
    }

    //TODO Leave commented out until domain-jts.xml is definitely removed from the configuration
//    @Test
//    public void testDomainJTS() throws Exception {
//        parseXml("docs/examples/configs/domain-jts.xml");
//    }
//
    @Test
    public void testStandaloneEC2HA() throws Exception {
        parseXml("docs/examples/configs/standalone-ec2-ha.xml");
    }

    @Test
    public void testStandaloneEC2FullHA() throws Exception {
        parseXml("docs/examples/configs/standalone-ec2-full-ha.xml");

    }
    @Test
    public void testStandaloneGossipHA() throws Exception {
        parseXml("docs/examples/configs/standalone-gossip-ha.xml");
    }

    @Test
    public void testStandaloneGossipFullHA() throws Exception {
        parseXml("docs/examples/configs/standalone-gossip-full-ha.xml");
    }

    @Test
    public void testStandaloneJTS() throws Exception {
        parseXml("docs/examples/configs/standalone-jts.xml");
    }

    @Test
    public void testStandaloneMinimalistic() throws Exception {
        parseXml("docs/examples/configs/standalone-minimalistic.xml");
    }

    @Test
    public void testStandaloneXTS() throws Exception {
        parseXml("docs/examples/configs/standalone-xts.xml");
    }

    @Test
    public void testStandaloneRTS() throws Exception {
        parseXml("docs/examples/configs/standalone-rts.xml");
    }

    @Test
    public void testStandaloneGenericJMS() throws Exception {
        parseXml("docs/examples/configs/standalone-genericjms.xml");
    }
}
