/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.servlet;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * A CDI-incompatible {@link javax.servletAsyncListener} may be bundled with a deployment.
 * This is OK as long as the application does it pass the listener class to
 * {@link javax.servletAsyncContext#createListener(Class)}. This test verifies that the deployment
 * of an application with the listener does not fail.
 *
 * @author Jozef Hartinger
 *
 * @see https://issues.jboss.org/browse/WFLY-2165
 *
 */
@RunWith(Arquillian.class)
public class NonCdiCompliantAsyncListenerTestCase {

    @Deployment
    public static Archive<?> getDeployment() {
        return ShrinkWrap.create(WebArchive.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addClass(NonCdiCompliantAsyncListener.class);
    }

    @Test
    public void test() {
        // noop, just test that the app deploys
    }
}
