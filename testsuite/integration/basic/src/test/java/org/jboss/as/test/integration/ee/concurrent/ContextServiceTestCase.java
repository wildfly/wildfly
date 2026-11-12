/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.concurrent;

import jakarta.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests for EE's context service
 * @author emartins
 */
@ExtendWith(ArquillianExtension.class)
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
