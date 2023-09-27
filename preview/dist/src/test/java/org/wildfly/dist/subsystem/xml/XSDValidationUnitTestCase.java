/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.dist.subsystem.xml;

import org.junit.Test;
import org.wildfly.test.distribution.validation.AbstractValidationUnitTest;

/**
 * A XSDValidationUnitTestCase.
 *
 * @author <a href="alex@jboss.com">Alexey Loubyansky</a>
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class XSDValidationUnitTestCase extends AbstractValidationUnitTest {
    @Test
    public void testJBossXsds() throws Exception {
        jbossXsdsTest();
    }
}
