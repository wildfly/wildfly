/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019 Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.microprofile.jwt.smallrye;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Properties;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Subsystem parsing test case.
 *
 * <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@RunWith(Parameterized.class)
public class MicroProfileJWTSubsystemTestCase extends AbstractSubsystemBaseTest {
    @Parameters
    public static Iterable<MicroProfileJWTSubsystemSchema> parameters() {
        return EnumSet.allOf(MicroProfileJWTSubsystemSchema.class);
    }

    private final MicroProfileJWTSubsystemSchema schema;

    public MicroProfileJWTSubsystemTestCase(MicroProfileJWTSubsystemSchema schema) {
        super(MicroProfileJWTExtension.SUBSYSTEM_NAME, new MicroProfileJWTExtension());
        this.schema = schema;
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource(String.format(Locale.ROOT, "subsystem_%d_%d.xml", this.schema.getVersion().major(), this.schema.getVersion().minor()));
    }

    @Override
    protected String getSubsystemXsdPath() throws IOException {
        return String.format(Locale.ROOT, "schema/wildfly-microprofile-jwt-smallrye_%d_%d.xsd", this.schema.getVersion().major(), this.schema.getVersion().minor());
    }

    @Override
    protected Properties getResolvedProperties() {
        return System.getProperties();
    }
}
