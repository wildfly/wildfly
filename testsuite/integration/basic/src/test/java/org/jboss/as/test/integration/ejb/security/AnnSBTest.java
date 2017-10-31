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

import java.util.HashMap;
import java.util.Map;

import javax.ejb.EJBAccessException;
import javax.naming.Context;
import javax.naming.NamingException;

import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.as.test.integration.ejb.security.authorization.AnnOnlyCheckSFSBForInjection;
import org.jboss.as.test.integration.ejb.security.authorization.AnnOnlyCheckSLSBForInjection;
import org.jboss.as.test.integration.ejb.security.authorization.ParentAnnOnlyCheck;
import org.jboss.as.test.integration.ejb.security.authorization.SimpleAuthorizationRemote;
import org.jboss.as.test.shared.integration.ejb.security.Util;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.wildfly.security.auth.client.AuthenticationConfiguration;
import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.auth.client.MatchRule;
import org.wildfly.security.auth.principal.AnonymousPrincipal;
import org.wildfly.security.sasl.SaslMechanismSelector;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Property;
import org.xnio.Sequence;

/**
 * This is a common parent for test cases to check whether basic EJB authorization works from an EJB client to a remote EJB.
 *
 * @author <a href="mailto:jan.lanik@redhat.com">Jan Lanik</a>
 */
public abstract class AnnSBTest {

    @ContainerResource
    private ManagementClient managementClient;

    public static Archive<JavaArchive> testAppDeployment(final Logger LOG, final String MODULE, final Class SB_TO_TEST) {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE + ".jar")
           .addClass(SB_TO_TEST)
           .addClass(SimpleAuthorizationRemote.class)
           .addClass(ParentAnnOnlyCheck.class)
           .addClass(AnnOnlyCheckSLSBForInjection.class)
           .addClass(AnnOnlyCheckSFSBForInjection.class);
        jar.addAsManifestResource(AnnSBTest.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
        jar.addPackage(CommonCriteria.class.getPackage());
        return jar;
    }

    private SimpleAuthorizationRemote getBean(final String MODULE, final Logger log, final Class SB_CLASS, Context ctx) throws NamingException {
        String myContext = Util.createRemoteEjbJndiContext(
           "",
           MODULE,
           "",
           SB_CLASS.getSimpleName(),
           SimpleAuthorizationRemote.class.getName(),
           isBeanClassStatefull(SB_CLASS));

        log.trace("JNDI name=" + myContext);

        return (SimpleAuthorizationRemote) ctx.lookup(myContext);
    }


    /**
     * Test objective:
     * Check if default, @RolesAllowed, @PermitAll, @DenyAll and @RolesAllowed with multiple roles
     * works on method level without user logged in as described in EJB 3.1 spec.
     * The target session bean is given as parameter
     * Expected results:
     * Test has to finish without any exception or error.
     *
     * @throws Exception
     */
    public void testSingleMethodAnnotationsNoUserTemplate(final String MODULE, final Logger log, final Class SB_CLASS) throws Exception {
        final Context ctx = Util.createNamingContext();
        final AuthenticationContext authenticationContext = AuthenticationContext.empty().with(MatchRule.ALL, AuthenticationConfiguration.EMPTY.useAuthorizationPrincipal(AnonymousPrincipal.getInstance()));
        authenticationContext.runCallable(() -> {
            String echoValue = getBean(MODULE, log, SB_CLASS, ctx).defaultAccess("alohomora");
            Assert.assertEquals(echoValue, "alohomora");

            try {
                echoValue = getBean(MODULE, log, SB_CLASS, ctx).roleBasedAccessOne("alohomora");
                Assert.fail("Method cannot be successfully called without logged in user");
            } catch (Exception e) {
                // expected
                Assert.assertTrue("Thrown exception must be EJBAccessException, but was " + e.getClass().getSimpleName(), e instanceof EJBAccessException);
            }

            try {
                echoValue = getBean(MODULE, log, SB_CLASS, ctx).roleBasedAccessMore("alohomora");
                Assert.fail("Method cannot be successfully called without logged in user");
            } catch (EJBAccessException e) {
                // expected
            }

            try {
                echoValue = getBean(MODULE, log, SB_CLASS, ctx).permitAll("alohomora");
                Assert.assertEquals(echoValue, "alohomora");
            } catch (Exception e) {
                Assert.fail("@PermitAll annotation must allow all users and no users to call the method");
            }

            try {
                echoValue = getBean(MODULE, log, SB_CLASS, ctx).denyAll("alohomora");
                Assert.fail("@DenyAll annotation must allow all users and no users to call the method");
            } catch (Exception e) {
                // expected
                Assert.assertTrue("Thrown exception must be EJBAccessException, but was " + e.getClass().getSimpleName(), e instanceof EJBAccessException);
            }
            return null;
        });
    }

    /**
     * Test objective:
     * Check if default, @RolesAllowed, @PermitAll, @DenyAll and @RolesAllowed with multiple roles
     * works on method level with user1 logged in as described in EJB 3.1 spec.
     * user1 has "Users,Role1" roles.
     * The target session bean is given as parameter.
     * Expected results:
     * Test has to finish without any exception or error.
     * <p/>
     *
     * @throws Exception
     */
    public void testSingleMethodAnnotationsUser1Template(final String MODULE, final Logger log, final Class SB_CLASS) throws Exception {
        final Context ctx = Util.createNamingContext();
        final AuthenticationContext authenticationContext = setupAuthenticationContext("user1", "password1");
        authenticationContext.runCallable(() -> {
            try {
                String echoValue = getBean(MODULE, log, SB_CLASS, ctx).defaultAccess("alohomora");
                Assert.assertEquals(echoValue, "alohomora");
            } catch (EJBAccessException e) {
                Assert.fail("EJBAccessException not expected");
            }


            try {
                String echoValue = getBean(MODULE, log, SB_CLASS, ctx).roleBasedAccessOne("alohomora");
                Assert.assertEquals(echoValue, "alohomora");
            } catch (EJBAccessException e) {
                Assert.fail("EJBAccessException not expected");
            }

            try {
                String echoValue = getBean(MODULE, log, SB_CLASS, ctx).roleBasedAccessMore("alohomora");
                Assert.fail("Method cannot be successfully called with logged in principal.");
            } catch (Exception e) {
                // expected
                Assert.assertTrue("Thrown exception must be EJBAccessException, but was different", e instanceof EJBAccessException);
            }

            try {
                String echoValue = getBean(MODULE, log, SB_CLASS, ctx).permitAll("alohomora");
                Assert.assertEquals(echoValue, "alohomora");
            } catch (Exception e) {
                Assert.fail("@PermitAll annotation must allow all users and no users to call the method - principal.");
            }

            try {
                String echoValue = getBean(MODULE, log, SB_CLASS, ctx).denyAll("alohomora");
                Assert.fail("@DenyAll annotation must allow all users and no users to call the method");
            } catch (Exception e) {
                // expected
                Assert.assertTrue("Thrown exception must be EJBAccessException, but was different", e instanceof EJBAccessException);
            }

            try {
                String echoValue = getBean(MODULE, log, SB_CLASS, ctx).starRoleAllowed("alohomora");
                Assert.assertEquals(echoValue, "alohomora");
            } catch (Exception e) {
                Assert.fail(
                        "@RolesAllowed(\"**\") annotation must allow all authenticated users to the method.");
            }
            return null;
        });
    }

    /**
     * Test objective:
     * Check if default, @RolesAllowed, @PermitAll, @DenyAll and @RolesAllowed with multiple roles
     * works on method level with user1 logged in as described in EJB 3.1 spec.
     * user2 has "Users,Role2" roles.
     * The target session bean is given as parameter.
     * Expected results:
     * Test has to finish without any exception or error.
     * <p/>
     *
     * @throws Exception
     */
    public void testSingleMethodAnnotationsUser2Template(final String MODULE, final Logger log, final Class SB_CLASS) throws Exception {
        final Context ctx = Util.createNamingContext();
        final AuthenticationContext authenticationContext = setupAuthenticationContext("user2", "password2");
        authenticationContext.runCallable(() -> {
            try {
                String echoValue = getBean(MODULE, log, SB_CLASS, ctx).defaultAccess("alohomora");
                Assert.assertEquals(echoValue, "alohomora");
            } catch (EJBAccessException e) {
                Assert.fail("EJBAccessException not expected");
            }

            try {
                String echoValue = getBean(MODULE, log, SB_CLASS, ctx).roleBasedAccessOne("alohomora");
                Assert.fail("Method cannot be successfully called with logged in user2");
            } catch (Exception e) {
                // expected
                Assert.assertTrue("Thrown exception must be EJBAccessException, but was different", e instanceof EJBAccessException);
            }


            try {
                String echoValue = getBean(MODULE, log, SB_CLASS, ctx).roleBasedAccessMore("alohomora");
                Assert.assertEquals(echoValue, "alohomora");
            } catch (EJBAccessException e) {
                Assert.fail("EJBAccessException not expected");
            }

            try {
                String echoValue = getBean(MODULE, log, SB_CLASS, ctx).permitAll("alohomora");
                Assert.assertEquals(echoValue, "alohomora");
            } catch (Exception e) {
                Assert.fail("@PermitAll annotation must allow all users and no users to call the method - principal.");
            }

            try {
                String echoValue = getBean(MODULE, log, SB_CLASS, ctx).denyAll("alohomora");
                Assert.fail("@DenyAll annotation must allow all users and no users to call the method");
            } catch (Exception e) {
                // expected
                Assert.assertTrue("Thrown exception must be EJBAccessException, but was different", e instanceof EJBAccessException);
            }
            return null;
        });
    }


    protected AuthenticationContext setupAuthenticationContext(String username, String password) {
        OptionMap.Builder builder = OptionMap.builder().set(Options.SASL_POLICY_NOANONYMOUS, true);
        builder.set(Options.SASL_POLICY_NOPLAINTEXT, false);
        if (password != null) {
            builder.set(Options.SASL_DISALLOWED_MECHANISMS, Sequence.of("JBOSS-LOCAL-USER"));
        } else {
            builder.set(Options.SASL_MECHANISMS, Sequence.of("JBOSS-LOCAL-USER"));
        }

        final AuthenticationContext authenticationContext = AuthenticationContext.empty()
                .with(
                        MatchRule.ALL,
                        AuthenticationConfiguration.EMPTY
                                .useName(username == null ? "$local" : username)
                                .usePassword(password)
                                .useRealm(null)
                                .setSaslMechanismSelector(SaslMechanismSelector.fromString(password != null ? "DIGEST-MD5" : "JBOSS-LOCAL-USER"))
                                .useMechanismProperties(getSaslProperties(builder.getMap()))
                                .useProvidersFromClassLoader(AnnSBTest.class.getClassLoader()));
        return authenticationContext;
    }

    private Map<String, String> getSaslProperties(final OptionMap connectionCreationOptions) {
        Map<String, String> saslProperties = null;
        Sequence<Property> value = connectionCreationOptions.get(Options.SASL_PROPERTIES);
        if (value != null) {
            saslProperties = new HashMap<>(value.size());
            for (Property property : value) {
                saslProperties.put(property.getKey(), (String) property.getValue());
            }
        }
        return saslProperties;
    }


    protected static boolean isBeanClassStatefull(Class bean) {
        if (bean.getName().contains("toSLSB")) {
            return false;
        } else if (bean.getName().contains("toSFSB")) {
            return true;
        } else if (bean.getName().contains("SFSB")) {
            return true;
        } else if (bean.getName().contains("SLSB")) {
            return false;
        } else {
            throw new AssertionError("Session bean class has a wrong name!:  " + bean.getCanonicalName());
        }
    }

}


