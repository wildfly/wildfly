/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.container.interceptor.regex;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.ejb.interceptor.regex.RegexServerSetup;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@ServerSetup(RegexServerSetup.class)
public class EJBNameRegexContainerInterceptorTestCase {

    @ArquillianResource
    private Deployer deployer;

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "testregex.war");
        war.addPackage(EJBNameRegexContainerInterceptorTestCase.class.getPackage());
        war.addAsWebInfResource(EJBNameRegexContainerInterceptorTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
        return war;
    }

    @Deployment(name = "invalid-regex", managed = false)
    public static Archive<?> deployInvalidRegex() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "testregex-invalid.war");
        war.addPackage(EJBNameRegexContainerInterceptorTestCase.class.getPackage());
        war.addAsWebInfResource(EJBNameRegexContainerInterceptorTestCase.class.getPackage(), "jboss-ejb3-invalid.xml", "jboss-ejb3.xml");
        return war;
    }

    @Test
    public void testExactNameAndRegexMatch() throws NamingException {
        resetInterceptors();
        final InitialContext ctx = new InitialContext();
        SLSBRemote bean = (SLSBRemote) ctx.lookup("java:module/TestRemote");
        bean.foo();
        Assert.assertTrue("Exact-name binding should have applied", TestInterceptorFullName.invoked);
        Assert.assertTrue("Regex binding should have matched TestRemote", TestInterceptorRegex.invoked);
        Assert.assertFalse("Non-matching regex binding should not have applied", TestInterceptorWrongRegex.invoked);
    }

    @Test
    public void testInvalidRegexCausesDeploymentFailure() {
        try {
            deployer.deploy("invalid-regex");
            Assert.fail("Deployment with invalid regex ejb-name should have failed");
        } catch (Exception e) {
            String message = e.toString();
            Assert.assertTrue("Exception should mention the offending pattern [unclosed",
                    message.contains("[unclosed") || e.getCause() != null && e.getCause().toString().contains("[unclosed"));
        } finally {
            try {
                deployer.undeploy("invalid-regex");
            } catch (Exception ignored) {
            }
        }
    }

    private void resetInterceptors() {
        TestInterceptorFullName.invoked = false;
        TestInterceptorRegex.invoked = false;
        TestInterceptorWrongRegex.invoked = false;
    }
}
