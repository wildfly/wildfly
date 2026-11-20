/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.stateful.passivation.ejb2;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.ejb.stateful.passivation.PassivationTestCaseSetup;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests that passivation succeeds for ejb2 beans.
 * @see org.jboss.as.test.integration.ejb.stateful.passivation.PassivationTestCase
 *
 * @author Ondrej Chaloupka
 */
@ExtendWith(ArquillianExtension.class)
@ServerSetup(PassivationTestCaseSetup.class)
public class PassivationSucceedsEJB2TestCase {
    private static String jndi;

    @ArquillianResource
    private InitialContext ctx;

    static {
        jndi = "java:module/" + TestPassivationBean.class.getSimpleName() + "!" + TestPassivationRemoteHome.class.getName();
    }

    @Deployment
    public static Archive<?> deploy() throws Exception {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "passivation-ejb2-test.jar");
        jar.addPackage(PassivationSucceedsEJB2TestCase.class.getPackage());
        jar.addClasses(PassivationTestCaseSetup.class);
        jar.addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client, org.jboss.dmr"), "MANIFEST.MF");
        return jar;
    }

    @Test
    public void testPassivationMaxSize() throws Exception {
        TestPassivationRemoteHome home = (TestPassivationRemoteHome) ctx.lookup(jndi);
        TestPassivationRemote remote1 = home.create();
        try {
            Assertions.assertEquals(TestPassivationRemote.EXPECTED_RESULT, remote1.returnTrueString(),
                    "Returned remote1 result was not expected");

            Assertions.assertFalse(remote1.hasBeenPassivated(), "ejbPassivate not called on remote1, check cache configuration and client sleep time");
            Assertions.assertFalse(remote1.hasBeenActivated(), "ejbActivate not called on remote1");

            // create another bean. This should force the other bean to passivate, as only one bean is allowed in the pool at a time
            TestPassivationRemote remote2 = home.create();
            try {
                Assertions.assertEquals(TestPassivationRemote.EXPECTED_RESULT, remote2.returnTrueString(),
                        "Returned remote2 result was not expected");

                Assertions.assertFalse(remote2.hasBeenPassivated(),
                        "ejbPassivate not called on remote2, check cache configuration and client sleep time");
                Assertions.assertFalse(remote2.hasBeenActivated(), "ejbActivate not called on remote2");

                Assertions.assertTrue(remote1.hasBeenPassivated(),
                        "ejbPassivate not called on remote1, check cache configuration and client sleep time");
                Assertions.assertTrue(remote1.hasBeenActivated(), "ejbActivate not called on remote1");

                Assertions.assertTrue(remote2.hasBeenPassivated(),
                        "ejbPassivate not called on remote2, check cache configuration and client sleep time");
                Assertions.assertTrue(remote2.hasBeenActivated(), "ejbActivate not called on remote2");
            } finally {
                remote2.remove();
            }
        } finally {
            remote1.remove();
        }
    }
}
