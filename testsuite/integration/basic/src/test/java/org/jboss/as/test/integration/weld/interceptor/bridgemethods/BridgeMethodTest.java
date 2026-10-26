/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.weld.interceptor.bridgemethods;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 *
 */
@ExtendWith(ArquillianExtension.class)
public class BridgeMethodTest {

    @Deployment
    public static Archive<?> deploy() {
        return ShrinkWrap.create(JavaArchive.class, "testBridgeMethods.jar")
                .addAsManifestResource(new StringAsset("<beans bean-discovery-mode=\"all\"><interceptors><class>" + SomeInterceptor.class.getName() + "</class></interceptors></beans>"), "beans.xml")
                .addPackage(BridgeMethodTest.class.getPackage());
    }

    @Inject
    private SpecialService specialService;

    @BeforeEach
    public void setUp() {
        SomeInterceptor.invocationCount = 0;
    }

    @Test
    public void testBridgeMethodInterceptor() {
        specialService.doSomething("foo");
        assertEquals(1, SomeInterceptor.invocationCount);
    }

}
