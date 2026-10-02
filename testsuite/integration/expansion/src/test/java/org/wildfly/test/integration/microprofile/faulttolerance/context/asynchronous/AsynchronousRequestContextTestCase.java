/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.microprofile.faulttolerance.context.asynchronous;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Adapted from Thorntail/SmallRye.
 *
 * @author Martin Kouba
 * @author Radoslav Husar
 */
@ExtendWith(ArquillianExtension.class)
public class AsynchronousRequestContextTestCase {

    @Deployment
    public static WebArchive createTestArchive() {
        return ShrinkWrap.create(WebArchive.class, AsynchronousRequestContextTestCase.class.getSimpleName() + ".war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addPackage(AsynchronousRequestContextTestCase.class.getPackage())
                ;
    }

    @Inject
    AsyncService asyncService;

    @Test
    void requestContextActive() throws Exception {
        RequestFoo.DESTROYED.set(false);
        assertEquals("ok", asyncService.perform().get());
        assertTrue(RequestFoo.DESTROYED.get());
    }

}
