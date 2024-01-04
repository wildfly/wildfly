/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.vdx.domain;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.vdx.TestBase;
import org.wildfly.test.integration.vdx.category.DomainTests;
import org.wildfly.test.integration.vdx.utils.server.ServerConfig;

import java.nio.file.Files;

import static org.wildfly.test.integration.vdx.standalone.SmokeStandaloneTestCase.*;

/**
 *
 * Created by mnovak on 11/28/16.
 */
@RunAsClient
@RunWith(Arquillian.class)
@Category(DomainTests.class)
public class SmokeDomainTestCase extends TestBase {
    @Test
    @ServerConfig(configuration = "duplicate-attribute.xml")
    public void testWithExistingConfigInResources() throws Exception {
        container().tryStartAndWaitForFail();
        ensureDuplicateAttribute(container().getErrorMessageFromServerStart());
    }

    @Test
    @ServerConfig(configuration = "domain-to-damage.xml", xmlTransformationGroovy = "TypoInExtensions.groovy")
    public void typoInExtensionsWithConfigInResources() throws Exception {
        container().tryStartAndWaitForFail();
        ensureTypoInExtensions(container().getErrorMessageFromServerStart());
    }

    @Test
    @ServerConfig(configuration = "domain.xml", xmlTransformationGroovy = "AddNonExistentElementToMessagingSubsystem.groovy",
            subtreeName = "messaging", subsystemName = "messaging-activemq", profileName = "full-ha")
    public void addNonExistingElementToMessagingSubsystem() throws Exception {
        // WildFly Preview doesn't configure a messaging broker
        AssumeTestGroupUtil.assumeNotWildFlyPreview();
        container().tryStartAndWaitForFail();
        ensureNonExistingElementToMessagingSubsystem(container().getErrorMessageFromServerStart());
    }

    @Test
    @ServerConfig(configuration = "empty.xml")
    public void emptyDCConfigFile() throws Exception {
        container().tryStartAndWaitForFail();
        assertContains( String.join("\n", Files.readAllLines(container().getServerLogPath())),
                "OPVDX004: Failed to pretty print validation error: empty.xml has no content");
    }

    @Test
    @ServerConfig(configuration = "domain.xml", hostConfig = "empty.xml")
    public void emptyHCConfigFile() throws Exception {
        container().tryStartAndWaitForFail();
        assertContains( String.join("\n", Files.readAllLines(container().getServerLogPath())),
                "OPVDX004: Failed to pretty print validation error: empty.xml has no content");
    }
}
