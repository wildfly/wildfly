/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
        return readResource("subsystem_1_0.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws IOException {
        return "schema/wildfly-elytron-oidc_1_0.xsd";
    }

    @Override
    protected String[] getSubsystemTemplatePaths() throws IOException {
        return new String[]{
                "/subsystem-templates/elytron-oidc.xml",
        };
    }

    @Override
    public void testSchemaOfSubsystemTemplates() throws Exception {
        //
    }

    @Override
    protected void compareXml(String configId, String original, String marshalled) throws Exception {
        //
    }

    protected Properties getResolvedProperties() {
        return System.getProperties();
    }
}
