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

import java.util.EnumSet;
import java.util.Properties;

import org.jboss.as.subsystem.test.AbstractSubsystemSchemaTest;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Subsystem parsing test case.
 *
 * <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@RunWith(Parameterized.class)
public class MicroProfileJWTSubsystemTestCase extends AbstractSubsystemSchemaTest<MicroProfileJWTSubsystemSchema> {
    @Parameters
    public static Iterable<MicroProfileJWTSubsystemSchema> parameters() {
        return EnumSet.allOf(MicroProfileJWTSubsystemSchema.class);
    }

    public MicroProfileJWTSubsystemTestCase(MicroProfileJWTSubsystemSchema schema) {
        super(MicroProfileJWTExtension.SUBSYSTEM_NAME, new MicroProfileJWTExtension(), schema, MicroProfileJWTSubsystemSchema.CURRENT);
    }

    @Override
    protected String getSubsystemXmlPathPattern() {
        // Exclude subsystem name from pattern
        return "subsystem_%2$d_%3$d.xml";
    }

    @Override
    protected Properties getResolvedProperties() {
        return System.getProperties();
    }
}
