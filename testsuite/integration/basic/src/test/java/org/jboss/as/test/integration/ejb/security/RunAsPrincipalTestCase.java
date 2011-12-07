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
package org.jboss.as.test.integration.ejb.security;

import org.junit.Ignore;
import org.junit.Test;

import org.jboss.logging.Logger;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.ejb.security.runasprincipal.Caller;
import org.jboss.as.test.integration.ejb.security.runasprincipal.CallerWithIdentity;
import org.jboss.as.test.integration.ejb.security.runasprincipal.WhoAmI;
import org.jboss.security.client.SecurityClient;
import org.jboss.security.client.SecurityClientFactory;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.util.Base64;
import org.junit.Assert;
import org.junit.runner.RunWith;

import javax.ejb.EJBAccessException;
import javax.naming.InitialContext;

/**
 * Migration of test from EJB3 testsuite [JBQA-5451] Testing calling with runasprincipal annotation (ejbthree1945)
 *
 * @author Carlo de Wolf, Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
public class RunAsPrincipalTestCase extends SecurityTest {

    private static final Logger log = Logger.getLogger(RunAsPrincipalTestCase.class);

    @Deployment
    public static Archive<?> runAsDeployment() {
        // FIXME hack to get things prepared before the deployment happens
        try {
            // create required security domains
            createSecurityDomain();
        } catch (Exception e) {
            // ignore
        }

        // using JavaArchive doesn't work, because of a bug in Arquillian, it only deploys wars properly
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "runasprincipal-test.war")
                .addPackage(WhoAmI.class.getPackage())
                .addClass(Util.class)
                .addClass(Entry.class)
                .addClass(RunAsPrincipalTestCase.class)
                .addClass(Base64.class)
                .addClass(SecurityTest.class)
                .addAsResource(CallerWithIdentity.class.getPackage(),"users.properties", "users.properties")
                .addAsResource(CallerWithIdentity.class.getPackage(),"roles.properties", "roles.properties")
                .addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client,org.jboss.dmr\n"),"MANIFEST.MF");
        log.info(war.toString(true));
        return war;
    }

    private WhoAmI lookupCallerWithIdentity() throws Exception {
        return (WhoAmI)new InitialContext().lookup("java:module/" + CallerWithIdentity.class.getSimpleName() + "!" + WhoAmI.class.getName());
    }

    private WhoAmI lookupCaller() throws Exception {
        return (WhoAmI)new InitialContext().lookup("java:module/" + Caller.class.getSimpleName() + "!" + WhoAmI.class.getName());
    }

    @Test
    public void testJackInABox() throws Exception {
        SecurityClient client = SecurityClientFactory.getSecurityClient();
        client.setSimple("thomas", "valid");
        client.login();
        try {
            WhoAmI bean =  lookupCallerWithIdentity();
            String actual = bean.getCallerPrincipal();
            Assert.assertEquals("jackinabox", actual);
        } finally {
            client.logout();
        }
    }

    @Ignore("AS7-2852")
    @Test
    public void testRunAsPrincipal() throws Exception {
        WhoAmI bean = lookupCaller();
        try {
            String actual = bean.getCallerPrincipal();
            Assert.fail("Expected EJBAccessException and it was get identity: " + actual);
        } catch (EJBAccessException e) {
            // good
        }
    }

    @Test
    public void testAnonymous() throws Exception {
        SecurityClient client = SecurityClientFactory.getSecurityClient();
        client.setSimple("thomas", "valid");
        client.login();
        try {
            WhoAmI bean = lookupCaller();
            String actual = bean.getCallerPrincipal();
            Assert.assertEquals("anonymous", actual);
        } finally {
            client.logout();
        }
    }
}
