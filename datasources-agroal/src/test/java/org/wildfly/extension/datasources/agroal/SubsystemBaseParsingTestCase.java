/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.datasources.agroal;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;

import java.io.IOException;

/**
 * This is the bare bones test example that tests subsystem
 * It does same things that {@link SubsystemParsingTestCase} does but most of internals are already done in AbstractSubsystemBaseTest
 * If you need more control over what happens in tests look at {@link SubsystemParsingTestCase}
 *
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 */
public class SubsystemBaseParsingTestCase extends AbstractSubsystemBaseTest {

    public SubsystemBaseParsingTestCase() {
        super(AgroalExtension.SUBSYSTEM_NAME, new AgroalExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return "<subsystem xmlns=\"" + AgroalNamespace.CURRENT.getUriString() + "\"/>";
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/wildfly-agroal_2_0.xsd";
    }
}
