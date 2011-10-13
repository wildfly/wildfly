/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.integration.ee.injection.resource.basic;

import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests that the Resource injection as specified by Java EE spec works as expected
 * <p/>
 * User: Jaikiran Pai
 */
@RunWith(Arquillian.class)
public class ResourceInjectionTestCase {

    private static final Logger logger = Logger.getLogger(ResourceInjectionTestCase.class.getName());

    private SimpleSLSB slsb;

    @Before
    public void beforeTest() throws Exception {
        Context ctx = new InitialContext();
        this.slsb = (SimpleSLSB) ctx.lookup("java:module/" + SimpleSLSB.class.getSimpleName() + "!" + SimpleSLSB.class.getName());
    }

    @Deployment
    public static WebArchive createWebDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "resource-injection-test.war");
        war.addPackage(SimpleSLSB.class.getPackage());
        war.addAsWebInfResource("injection/web.xml");
        logger.info("War created: " + war.toString(true));
        return war;
    }

    /**
     * Tests simple resource injection in EJB
     */
    @Test
    public void testResourceInjectionInEJB() {
        final String user = "Charlie Sheen";
        final String greeting = this.slsb.sayHello(user);
        Assert.assertEquals("Unepxected greeting received from bean", CommonBean.HELLO_GREETING_PREFIX + user, greeting);

        Class<?> invokedBusinessInterface = this.slsb.getInvokedBusinessInterface();
        Assert.assertEquals("Unexpected invoked business interface returned by bean", SimpleSLSB.class, invokedBusinessInterface);
    }

    /**
     * Test env-entry injection in EJB
     */
    @Test
    public void testEnvEntryInjection() {
        final String envEntryString = this.slsb.getInjectedString();
        Assert.assertEquals("Unexpected value injected for env-entry of type String", "It's Friday!!!", envEntryString);
    }

    /**
     * Test scenario:
     * <p/>
     * A @Resource backed by a env-entry should be injected only if the corresponding env-entry has a env-entry-value specified.
     * If the env-entry-value is missing, then the injection of @Resource should not happen.
     */
    @Test
    public void testOptionalEnvEntryInjection() {
        int defaultInt = this.slsb.getUnInjectedInt();
        Assert.assertEquals("env-entry of type int without an value was *not* expected to be injected", defaultInt, SimpleSLSB.DEFAULT_UNINJECTED_INT_VAL);

        String defaultString = this.slsb.getUnInjectedString();
        Assert.assertEquals("env-entry of type String without an value was *not* expected to be injected", defaultString, SimpleSLSB.DEFAULT_UNINJECTED_STRING_VAL);
    }

    /**
     * Test scenario:
     * <p/>
     * A @Resource backed by a env-entry should be made available in ENC only if the corresponding env-entry has a env-entry-value specified.
     * If the env-entry-value is missing, then there should be no corresponding ENC entry for that env-entry
     */
    @Test
    public void testOptionalEnvEntryEncAvailability() {
        boolean intEnvEntryAvailableInEnc = this.slsb.isUnInjectedIntEnvEntryPresentInEnc();
        Assert.assertFalse("env-entry of type int, without a env-entry-value was *not* expected to be available in ENC", intEnvEntryAvailableInEnc);

        boolean stringEnvEntryAvailableInEnc = this.slsb.isUnInjectedStringEnvEntryPresentInEnc();
        Assert.assertFalse("env-entry of type String, without a env-entry-value was *not* expected to be available in ENC", stringEnvEntryAvailableInEnc);

    }

    /**
     * Tests that a EJB with a @Resource of type {@link javax.ejb.TimerService} deploys fine and the {@link javax.ejb.TimerService}
     * is injected in the bean
     */
    @Test
    public void testTimerServiceInjection() {
        Assert.assertTrue("Timerservice was not injected in bean", this.slsb.isTimerServiceInjected());
    }
}
