/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat Middleware LLC, and individual contributors
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
package org.wildfly.test.manual.elytron.seccontext;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * Authorization forwarding (credential less forwarding) for security context propagation test.
 *
 * @author Josef Cacek
 */
public abstract class AbstractAuthorizationForwardingTestCase extends AbstractSecurityContextPropagationTestBase {

    /**
     * Test the authorization forwarding (credential less propagation) works for EJB calls when {@link RunAsPrincipalPermission}
     * is assigned to caller server identity.
     *
     * <pre>
     * When: EJB client calls EntryBean as admin user and Elytron AuthenticationContext API is used to
     *       authorization forwarding to WhoAmIBean call with "server" user used as caller server identity
     * Then: WhoAmIBean call is possible and returns "admin" username
     * </pre>
     */
    @Test
    public void testForwardedAuthorizationPasses() throws Exception {
        String[] doubleWhoAmI = SeccontextUtil.switchIdentity("admin", "admin",
                getDoubleWhoAmICallable(ReAuthnType.FORWARDED_AUTHORIZATION, "server", "server"),
                ReAuthnType.AC_AUTHENTICATION);
        assertNotNull("The entryBean.doubleWhoAmI() should return not-null instance", doubleWhoAmI);
        assertArrayEquals("Unexpected principal names returned from doubleWhoAmI", new String[]{"admin", "admin"},
                doubleWhoAmI);
    }

    /**
     * Test the authorization forwarding works for EJB calls when {@link RunAsPrincipalPermission} is not assigned to the caller
     * identity, but the authentication identity == authorization identity (which has sufficient roles to call the EJB).
     *
     * <pre>
     * When: EJB client calls EntryBean as admin user and Elytron AuthenticationContext API is used to
     *       authorization forwarding to WhoAmIBean call with "admin" user used as caller server identity.
     * Then: WhoAmIBean call is possible and returns "admin" username
     * </pre>
     */
    @Test
    public void testSameAuthorizationIdentityPasses() throws Exception {
        String[] doubleWhoAmI = SeccontextUtil.switchIdentity("admin", "admin",
                getDoubleWhoAmICallable(ReAuthnType.FORWARDED_AUTHORIZATION, "admin", "admin"), ReAuthnType.AC_AUTHENTICATION);
        assertNotNull("The entryBean.doubleWhoAmI() should return not-null instance", doubleWhoAmI);
        assertArrayEquals("Unexpected principal names returned from doubleWhoAmI", new String[]{"admin", "admin"},
                doubleWhoAmI);
    }

    /**
     * Test the authorization forwarding fails for EJB calls when {@link RunAsPrincipalPermission} is not assigned to the caller
     * identity.
     *
     * <pre>
     * When: EJB client calls EntryBean as admin user and Elytron AuthenticationContext API is used to
     *       authorization forwarding to WhoAmIBean call with either "server-norunas" or "whoami" users
     *       used as caller server identity.
     * Then: WhoAmIBean call fails in both cases as the server identity don't have RunAsPrincipalPermission
     * </pre>
     */
    @Test
    public void testForwardedAuthorizationIdentityWithoutRunAsFails() throws Exception {
        String[] doubleWhoAmI = SeccontextUtil.switchIdentity("admin", "admin",
                getDoubleWhoAmICallable(ReAuthnType.FORWARDED_AUTHORIZATION, "server-norunas", "server-norunas"),
                ReAuthnType.AC_AUTHENTICATION);
        assertNotNull("The entryBean.doubleWhoAmI() should return not-null instance", doubleWhoAmI);
        assertThat(doubleWhoAmI[1], isEjbAuthenticationError());

        doubleWhoAmI = SeccontextUtil.switchIdentity("admin", "admin",
                getDoubleWhoAmICallable(ReAuthnType.FORWARDED_AUTHORIZATION, "whoami", "whoami"),
                ReAuthnType.AC_AUTHENTICATION);
        assertNotNull("The entryBean.doubleWhoAmI() should return not-null instance", doubleWhoAmI);
        assertThat(doubleWhoAmI[1], isEjbAuthenticationError());
    }

    /**
     * Test propagation of RuntimeException back to server1 during a call using the authorization forwarding.
     *
     * <pre>
     * When: EJB client calls EntryBean as admin user and Elytron AuthenticationContext API is used to
     *       authorization forwarding to WhoAmIBean call with "server" user used as caller server identity
     * Then: WhoAmIBean.throwIllegalStateException call should result in expected IllegalStateException.
     * </pre>
     */
    @Test
    public void testIllegalStateExceptionFromForwardedAuthz() throws Exception {
        String[] doubleWhoAmI = SeccontextUtil.switchIdentity("admin", "admin",
                getWhoAmIAndIllegalStateExceptionCallable(ReAuthnType.FORWARDED_AUTHORIZATION, "server", "server"),
                ReAuthnType.AC_AUTHENTICATION);
        assertNotNull("The entryBean.whoAmIAndIllegalStateException() should return not-null instance", doubleWhoAmI);
        assertEquals("admin", doubleWhoAmI[0]);
        assertThat(doubleWhoAmI[1], isExpectedIllegalStateException());
    }

    /**
     * Test propagation of Server2Exception (unknown on server1) back to server1 during a call using the authorization
     * forwarding.
     *
     * <pre>
     * When: EJB client calls EntryBean as admin user and Elytron AuthenticationContext API is used to
     *       authorization forwarding to WhoAmIBean call with "server" user used as caller server identity
     * Then: WhoAmIBean.throwServer2Exception call should result in expected ClassNotFoundException.
     * </pre>
     */
    @Test
    public void testServer2ExceptionFromForwardedAuthz() throws Exception {
        String[] doubleWhoAmI = SeccontextUtil.switchIdentity("admin", "admin",
                getWhoAmIAndServer2ExceptionCallable(ReAuthnType.FORWARDED_AUTHORIZATION, "server", "server"),
                ReAuthnType.AC_AUTHENTICATION);
        assertNotNull("The entryBean.whoAmIAndServer2Exception() should return not-null instance", doubleWhoAmI);
        assertEquals("admin", doubleWhoAmI[0]);
        assertThat(doubleWhoAmI[1], isClassNotFoundException_Server2Exception());
    }
}
