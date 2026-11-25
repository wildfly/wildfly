/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.concurrent;

import jakarta.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for EE's context service
 * @author emartins
 */
@RunWith(Arquillian.class)
public class ContextServiceTestCase {

    @Deployment
    public static Archive<?> getDeployment() {
        return ShrinkWrap.create(JavaArchive.class, ContextServiceTestCase.class.getSimpleName() + ".jar")
                .addClasses(ContextServiceTestCase.class, ContextualProxyExceptionUnwrapBean.class, ContextualProxy.class);
    }

    @Inject
    ContextualProxyExceptionUnwrapBean bean;

    @Test
    public void testContextualProxyDeclaredException() throws Exception {
        bean.test();
    }
}
