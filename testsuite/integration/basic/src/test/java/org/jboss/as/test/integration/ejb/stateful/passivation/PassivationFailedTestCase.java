/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.stateful.passivation;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

import java.util.PropertyPermission;

import jakarta.ejb.NoSuchEJBException;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Paul Ferraro
 */
@RunWith(Arquillian.class)
@ServerSetup(PassivationTestCaseSetup.class)
public class PassivationFailedTestCase {

    @org.junit.BeforeClass
    public static void init() {
        org.jboss.as.test.shared.IntermittentFailure.thisTestIsFailingIntermittently("WFLY-19293");
    }

    @ArquillianResource
    private InitialContext ctx;

    @Deployment
    public static Archive<?> deploy() throws Exception {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, PassivationFailedTestCase.class.getSimpleName() + ".jar");
        jar.addPackage(PassivationTestCase.class.getPackage());
        jar.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        jar.addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client, org.jboss.dmr \n"), "MANIFEST.MF");
        jar.addAsManifestResource(PassivationTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        jar.addAsManifestResource(PassivationTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        jar.addAsManifestResource(createPermissionsXmlAsset(new PropertyPermission("ts.timeout.factor", "read")), "jboss-permissions.xml");
        return jar;
    }

    /**
     * Tests passivation of bean that throws exception during serialization.
     */
    @Test
    public void testPassivationFailure() throws Exception {
        PassivationInterceptor.reset();

        // create first bean
        try (TestPassivationRemote remote1 = (TestPassivationRemote) ctx.lookup("java:module/" + BeanWithSerializationIssue.class.getSimpleName())) {
            // make an invocation
            Assert.assertEquals("Returned remote1 result was not expected", TestPassivationRemote.EXPECTED_RESULT, remote1.returnTrueString());

            // create second bean, this should force the first bean to passivate
            try (TestPassivationRemote remote2 = (TestPassivationRemote) ctx.lookup("java:module/" + BeanWithSerializationIssue.class.getSimpleName())) {
                // make an invocation
                Assert.assertEquals("Returned remote2 result was not expected", TestPassivationRemote.EXPECTED_RESULT, remote2.returnTrueString());

                // verify that a bean was prePassivated
                TestPassivationBean target = (TestPassivationBean) PassivationInterceptor.getPrePassivateTarget();
                Assert.assertNotNull(target);
                // verify that bean was not postActivated yet
                Assert.assertTrue(target.hasBeenPassivated());

                // From EJB 4.2.1:
                // The container may destroy a session bean instance if the instance does not meet the requirements for serialization after PrePassivate.
                try {
                    // At least one of these invocations should fail
                    remote1.returnTrueString();
                    Assert.fail("Invocation of pre-passivated EJB should not succeed since passivation failed");
                } catch (NoSuchEJBException e) {
                    // Expected
                }
                // No EJBs should have activated, since they were unable to passivate
                Assert.assertTrue(PassivationInterceptor.getPostActivateTarget() == null);
            } catch (NoSuchEJBException e) {
                // Expected
            }
        } catch (NoSuchEJBException e) {
            // Expected
        }
        PassivationInterceptor.reset();
    }
}
