/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.security;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Security subsystem tests for the version 2.0 of the subsystem schema.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class SecurityDomainModelv20UnitTestCase extends AbstractSubsystemBaseTest {

    public SecurityDomainModelv20UnitTestCase() {
        super(SecurityExtension.SUBSYSTEM_NAME, new SecurityExtension());
    }

    private static String oldConfig;


    @BeforeClass
    public static void beforeClass() {
        try {
            File target = new File(SecurityDomainModelv20UnitTestCase.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
            File config = new File(target, "config");
            config.mkdir();
            oldConfig = System.setProperty("jboss.server.config.dir", config.getAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @AfterClass
    public static void afterClass() {
        if (oldConfig != null) {
            System.setProperty("jboss.server.config.dir", oldConfig);
        } else {
            System.clearProperty("jboss.server.config.dir");
        }
    }


    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("securitysubsystemv20.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/wildfly-security_2_0.xsd";
    }

    @Override
    protected String[] getSubsystemTemplatePaths() throws IOException {
        return new String[] {
                "/subsystem-templates/security.xml"
        };
    }

    @Override
    protected Properties getResolvedProperties() {
        Properties properties = new Properties();
        properties.put("jboss.server.config.dir", System.getProperty("java.io.tmpdir"));
        return properties;
    }

    @Test
    @Override
    public void testSchemaOfSubsystemTemplates() throws Exception {
        super.testSchemaOfSubsystemTemplates();
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return AdditionalInitialization.withCapabilities("org.wildfly.clustering.infinispan.default-cache-configuration.security");
    }
}
