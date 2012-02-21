/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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

import java.util.Map;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.security.auth.login.LoginContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.ejb.security.lifecycle.BaseBean;
import org.jboss.as.test.integration.ejb.security.lifecycle.EntryBean;
import org.jboss.as.test.shared.integration.ejb.security.Util;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.fail;

/**
 * EJB 3.1 Section 17.2.5 - This test case is to test the programmatic access to the callers's security context for the various
 * bean methods.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@RunWith(Arquillian.class)
public class LifecycleTestCase extends SecurityTest {

    private static final Logger log = Logger.getLogger(LifecycleTestCase.class.getName());

    private static final String USER1 = "user1";

    private static final String TRUE = "true";

    private static final String UNSUPPORTED_OPERATION = "UnsupportedOperationException";

    private static final String ILLEGAL_STATE = "IllegalStateException";

    @EJB(mappedName = "java:global/ejb3security/EntryBean")
    private EntryBean entryBean;

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
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "ejb3security.war")
                .addPackage(EntryBean.class.getPackage())
                .addClass(SecurityTest.class)
                .addClass(Util.class) // TODO - Should not need to exclude the interfaces.
                .addAsResource("ejb3/security/users.properties", "users.properties")
                .addAsResource("ejb3/security/roles.properties", "roles.properties")
                .addAsWebInfResource("ejb3/security/jboss-web.xml", "jboss-web.xml")
                .addAsManifestResource(new StringAsset("Manifest-Version: 1.0\nDependencies: org.jboss.as.controller-client,org.jboss.dmr\n"), "MANIFEST.MF");
        log.info(war.toString(true));
        return war;
    }

    @Test
    public void testStatefulBean() throws Exception {
        StringBuilder failureMessages = new StringBuilder();
        LoginContext lc = Util.getCLMLoginContext("user1", "password1");
        lc.login();
        try {
            Map<String, String> result = entryBean.testStatefulBean();
            verifyResult(result, BaseBean.LIFECYCLE_CALLBACK, USER1, UNSUPPORTED_OPERATION, TRUE, ILLEGAL_STATE, failureMessages);
            verifyResult(result, BaseBean.BUSINESS, USER1, UNSUPPORTED_OPERATION, TRUE, ILLEGAL_STATE, failureMessages);
            verifyResult(result, BaseBean.AFTER_BEGIN, USER1, UNSUPPORTED_OPERATION, TRUE, ILLEGAL_STATE, failureMessages);
        } finally {
            lc.logout();
        }

        if (failureMessages.length() > 0) {
            fail(failureMessages.toString());
        }
    }

    @Test
    @Ignore("AS7-1064")
    public void testStatefulBeanDependencyInjection() throws Exception {
        StringBuilder failureMessages = new StringBuilder();
        LoginContext lc = Util.getCLMLoginContext("user1", "password1");
        lc.login();
        try {
            Map<String, String> result = entryBean.testStatefulBean();
            verifyResult(result, BaseBean.DEPENDENCY_INJECTION, ILLEGAL_STATE, UNSUPPORTED_OPERATION, ILLEGAL_STATE, ILLEGAL_STATE,
                    failureMessages);
        } finally {
            lc.logout();
        }

        if (failureMessages.length() > 0) {
            fail(failureMessages.toString());
        }
    }

    @Test
    public void testStatelessBean() throws Exception {
        StringBuilder failureMessages = new StringBuilder();
        LoginContext lc = Util.getCLMLoginContext("user1", "password1");
        lc.login();
        try {
            Map<String, String> result = entryBean.testStatlessBean();
            for (String current : result.keySet()) {
                log.info(current + " = " + result.get(current));
            }
            verifyResult(result, BaseBean.BUSINESS, USER1, UNSUPPORTED_OPERATION, TRUE, ILLEGAL_STATE, failureMessages);
        } finally {
            lc.logout();
        }

        if (failureMessages.length() > 0) {
            fail(failureMessages.toString());
        }
    }

    @Test
    @Ignore("AS7-1064")
    public void testStatelessBeanDependencyInjection() throws Exception {
        StringBuilder failureMessages = new StringBuilder();
        LoginContext lc = Util.getCLMLoginContext("user1", "password1");
        lc.login();
        try {
            Map<String, String> result = entryBean.testStatlessBean();
            for (String current : result.keySet()) {
                log.info(current + " = " + result.get(current));
            }
            verifyResult(result, BaseBean.DEPENDENCY_INJECTION, ILLEGAL_STATE, UNSUPPORTED_OPERATION, ILLEGAL_STATE, ILLEGAL_STATE,
                    failureMessages);
        } finally {
            lc.logout();
        }

        if (failureMessages.length() > 0) {
            fail(failureMessages.toString());
        }
    }

    @Test
    @Ignore("AS7-1064")
    public void testStatelessBeanLifecyleCallback() throws Exception {
        StringBuilder failureMessages = new StringBuilder();
        LoginContext lc = Util.getCLMLoginContext("user1", "password1");
        lc.login();
        try {
            Map<String, String> result = entryBean.testStatlessBean();
            for (String current : result.keySet()) {
                log.info(current + " = " + result.get(current));
            }
            verifyResult(result, BaseBean.LIFECYCLE_CALLBACK, ILLEGAL_STATE, UNSUPPORTED_OPERATION, ILLEGAL_STATE, ILLEGAL_STATE,
                    failureMessages);
        } finally {
            lc.logout();
        }

        if (failureMessages.length() > 0) {
            fail(failureMessages.toString());
        }
    }

    // TODO - Add test for Message Driven Bean

    private void verifyResult(Map<String, String> result, String beanMethod, String getCallerPrincipalResponse,
            String getCallerIdentityResponse, String isCallerInRoleResponse, String isCallerInRoleIdentityResponse,
            StringBuilder errors) {
        verify(beanMethod, BaseBean.GET_CALLER_PRINCIPAL, getCallerPrincipalResponse,
                result.get(beanMethod + ":" + BaseBean.GET_CALLER_PRINCIPAL), errors);
        verify(beanMethod, BaseBean.GET_CALLER_IDENTITY, getCallerIdentityResponse, result.get(beanMethod + ":" + BaseBean.GET_CALLER_IDENTITY),
                errors);
        verify(beanMethod, BaseBean.IS_CALLER_IN_ROLE, isCallerInRoleResponse, result.get(beanMethod + ":" + BaseBean.IS_CALLER_IN_ROLE), errors);
        verify(beanMethod, BaseBean.IS_CALLER_IN_ROLE_IDENITY, isCallerInRoleIdentityResponse,
                result.get(beanMethod + ":" + BaseBean.IS_CALLER_IN_ROLE_IDENITY), errors);
    }

    private void verify(String beanMethod, String ejbContextMethod, String expected, String actual, StringBuilder errors) {
        if (expected.equals(actual) == false) {
            errors.append('{');
            errors.append(ejbContextMethod).append(" for ").append(beanMethod);
            errors.append(" returned '").append(actual).append("' but we expected '");
            errors.append(expected).append("'}");
        }
    }

}
