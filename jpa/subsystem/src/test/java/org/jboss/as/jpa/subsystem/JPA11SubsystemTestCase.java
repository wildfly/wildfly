/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jpa.subsystem;

import java.io.IOException;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.junit.Test;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */

public class JPA11SubsystemTestCase extends AbstractSubsystemBaseTest {

    public JPA11SubsystemTestCase() {
        super(JPAExtension.SUBSYSTEM_NAME, new JPAExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem-1.1.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/jboss-as-jpa_1_1.xsd";
    }

    @Test
    public void testEmptySubsystem() throws Exception {
        standardSubsystemTest("subsystem-1.1-empty.xml");
    }
}
