/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.resource.basic;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests that the Resource injection as specified by Jakarta EE spec works as expected
 * <p/>
 * User: Jaikiran Pai
 */
@ExtendWith(ArquillianExtension.class)
public class ResourceInjectionTestCase {

    private static final Logger logger = Logger.getLogger(ResourceInjectionTestCase.class.getName());

    private SimpleSLSB slsb;

    @BeforeEach
    public void beforeTest() throws Exception {
        Context ctx = new InitialContext();
        this.slsb = (SimpleSLSB) ctx.lookup("java:module/" + SimpleSLSB.class.getSimpleName() + "!"
                + SimpleSLSB.class.getName());
    }

    @Deployment
    public static WebArchive createWebDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "resource-injection-test.war");
        war.addPackage(SimpleSLSB.class.getPackage());
        war.addAsWebInfResource(ResourceInjectionTestCase.class.getPackage(), "web.xml", "web.xml");
        return war;
    }

    /**
     * Tests simple resource injection in EJB
     */
    @Test
    public void testResourceInjectionInEJB() {
        final String user = "Charlie Sheen";
        final String greeting = this.slsb.sayHello(user);
        Assertions.assertEquals(CommonBean.HELLO_GREETING_PREFIX + user, greeting, "Unepxected greeting received from bean");

        Class<?> invokedBusinessInterface = this.slsb.getInvokedBusinessInterface();
        Assertions.assertEquals(SimpleSLSB.class, invokedBusinessInterface,
                "Unexpected invoked business interface returned by bean");
    }

    /**
     * Test env-entry injection in EJB
     */
    @Test
    public void testEnvEntryInjection() {
        final String envEntryString = this.slsb.getInjectedString();
        Assertions.assertEquals("It's Friday!!!", envEntryString, "Unexpected value injected for env-entry of type String");
    }

    /**
     * Test scenario:
     * <p/>
     * A @Resource backed by an env-entry should be injected only if the corresponding env-entry has an env-entry-value specified.
     * If the env-entry-value is missing, then the injection of @Resource should not happen.
     */
    @Test
    public void testOptionalEnvEntryInjection() {
        int defaultInt = this.slsb.getUnInjectedInt();
        Assertions.assertEquals(defaultInt, SimpleSLSB.DEFAULT_UNINJECTED_INT_VAL,
                "env-entry of type int without a value was *not* expected to be injected");

        String defaultString = this.slsb.getUnInjectedString();
        Assertions.assertEquals(defaultString, SimpleSLSB.DEFAULT_UNINJECTED_STRING_VAL,
                "env-entry of type String without a value was *not* expected to be injected");
    }

    /**
     * Test scenario:
     * <p/>
     * A @Resource backed by an env-entry should be made available in ENC only if the corresponding env-entry has an
     * env-entry-value specified. If the env-entry-value is missing, then there should be no corresponding ENC entry for that
     * env-entry
     */
    @Test
    public void testOptionalEnvEntryEncAvailability() {
        boolean intEnvEntryAvailableInEnc = this.slsb.isUnInjectedIntEnvEntryPresentInEnc();
        Assertions.assertFalse(intEnvEntryAvailableInEnc,
                "env-entry of type int, without an env-entry-value was *not* expected to be available in ENC");

        boolean stringEnvEntryAvailableInEnc = this.slsb.isUnInjectedStringEnvEntryPresentInEnc();
        Assertions.assertFalse(stringEnvEntryAvailableInEnc,
                "env-entry of type String, without an env-entry-value was *not* expected to be available in ENC");

    }

    /**
     * Tests that an EJB with a @Resource of type {@link jakarta.ejb.TimerService} deploys fine and the
     * {@link jakarta.ejb.TimerService} is injected in the bean
     */
    @Test
    public void testTimerServiceInjection() {
        Assertions.assertTrue(this.slsb.isTimerServiceInjected(), "Timerservice was not injected in bean");
    }

    /**
     * Test if an ORB can be properly located (EJB3 16.13). Part migration of tests from EJB testsuite [JIRA JBQA-5483].
     * Ondrej Chaloupka
     *
     * @throws Exception
     */
    @Test
    public void testOrbEnvironment() throws Exception {
        Context ctx = new InitialContext();
        CheckORBRemote bean = (CheckORBRemote) ctx.lookup("java:module/" + CheckORBBean.class.getSimpleName() + "!"
                + CheckORBRemote.class.getName());
        bean.checkForORBInEnvironment();
    }

    /**
     * Test if an ORB can be properly located (EJB3 16.13). Part migration of tests from EJB testsuite [JIRA JBQA-5483].
     *
     * @throws Exception
     */
    @Test
    public void testInjection() throws Exception {
        Context ctx = new InitialContext();
        CheckORBRemote bean = (CheckORBRemote) ctx.lookup("java:module/" + CheckORBBean.class.getSimpleName() + "!"
                + CheckORBRemote.class.getName());
        bean.checkForInjectedORB();
    }

    /**
     * Tests that a EJB with a URL Connection Factory @Resource deploys fine and is injected as expected
     */
    @Test
    public void testURLInjection() {
        Assertions.assertTrue(this.slsb.validURLInjections(), "URL injection not valid");
    }

    /**
     * Tests that a EJB with several Context (and sub classes) @Resource deploys fine and is injected as expected
     */
    @Test
    public void testContextInjection() {
        Assertions.assertTrue(this.slsb.validContextInjections(), "Context injection not valid");
    }
}
