/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.connector.subsystems.complextestcases;


import org.jboss.as.connector.subsystems.jca.JcaExtension;
import org.junit.Test;

/**
 *
 * @author <a href="vrastsel@redhat.com">Vladimir Rastseluev</a>
 */

public class ComplexJcaSubsystemTestCase extends AbstractComplexSubsystemTestCase {

    public ComplexJcaSubsystemTestCase() {
        super(JcaExtension.SUBSYSTEM_NAME, new JcaExtension());
    }

    @Test
    public void testModel() throws Exception {

        getModel("jca.xml");
    }

    @Test
    public void testMinModel() throws Exception {

        getModel("minimal-jca.xml");
    }


}
