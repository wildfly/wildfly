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

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.wildfly.test.manual.elytron.seccontext.AbstractSecurityContextPropagationTestBase.server1;
import static org.wildfly.test.manual.elytron.seccontext.SeccontextUtil.JAR_ENTRY_EJB;
import static org.wildfly.test.manual.elytron.seccontext.SeccontextUtil.SERVER1;
import static org.wildfly.test.manual.elytron.seccontext.SeccontextUtil.SERVER2;
import static org.wildfly.test.manual.elytron.seccontext.SeccontextUtil.SERVER3;
import static org.wildfly.test.manual.elytron.seccontext.SeccontextUtil.WAR_WHOAMI;

import java.io.IOException;
import java.net.SocketPermission;
import java.util.concurrent.Callable;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.wildfly.security.permission.ElytronPermission;

/**
 * Test case for authentication and authorization forwarding for 3 servers. Test scenarios use following configuration.
 * Description of used users and beans WhoAmIBean and EntryBean is in superclass.
 *
 * <h3>Given</h3>
 *
 * <pre>
 * Another EJBs used for testing:
 * - FirstServerChainBean - protected (entry, admin and no-server2-identity roles are allowed), stateless,
 * configures identity propagation and calls a remote EntryBean
 *
 * Deployments used for testing:
 * - first-server-ejb.jar (FirstServerChainBean)
 * - entry-ejb-server-chain.jar (EntryBean)
 * - whoami-server-chain.jar (WhoAmIBean)
 *
 * Servers started and configured for context propagation scenarios:
 * - seccontext-server1 (standalone-ha.xml)
 *   * first-server-ejb.jar
 * - seccontext-server2 (standalone.xml)
 *   * entry-ejb-server-chain.jar
 * - seccontext-server3
 *   * whoami-server-chain.jar
 * </pre>
 *
 * @author olukas
 */
public class ServerChainSecurityContextPropagationTestCase extends AbstractSecurityContextPropagationTestBase {

    public static final String FIRST_SERVER_CHAIN_EJB = "first-server-chain";
    public static final String JAR_ENTRY_EJB_SERVER_CHAIN = JAR_ENTRY_EJB + "-server-chain";
    public static final String WAR_WHOAMI_SERVER_CHAIN = WAR_WHOAMI + "-server-chain";

    private static final ServerHolder server3 = new ServerHolder(SERVER3, TestSuiteEnvironment.getServerAddressNode1(), 250);

    /**
     * Creates deployment with FirstServerChain bean - to be placed on the first server.
     */
    @Deployment(name = FIRST_SERVER_CHAIN_EJB, managed = false, testable = false)
    @TargetsContainer(SERVER1)
    public static Archive<?> createServerChain1Deployment() {
        return ShrinkWrap.create(JavaArchive.class, FIRST_SERVER_CHAIN_EJB + ".jar")
                .addClasses(FirstServerChainBean.class, FirstServerChain.class, Entry.class, ReAuthnType.class,
                        SeccontextUtil.class, CallAnotherBeanInfo.class)
                .addAsManifestResource(createPermissionsXmlAsset(new ElytronPermission("authenticate"),
                                new ElytronPermission("getPrivateCredentials"), new ElytronPermission("getSecurityDomain"),
                                new SocketPermission(TestSuiteEnvironment.getServerAddressNode1() + ":8180", "connect,resolve")),
                        "permissions.xml")
                .addAsManifestResource(Utils.getJBossEjb3XmlAsset("seccontext-entry"), "jboss-ejb3.xml");
    }

    /**
     * Creates deployment with Entry bean - to be placed on the second server.
     */
    @Deployment(name = JAR_ENTRY_EJB_SERVER_CHAIN, managed = false, testable = false)
    @TargetsContainer(SERVER2)
    public static Archive<?> createServerChain2Deployment() {
        return ShrinkWrap.create(JavaArchive.class, JAR_ENTRY_EJB_SERVER_CHAIN + ".jar")
                .addClasses(EntryBean.class, Entry.class, WhoAmI.class, ReAuthnType.class, SeccontextUtil.class,
                        CallAnotherBeanInfo.class)
                .addAsManifestResource(createPermissionsXmlAsset(new ElytronPermission("authenticate"),
                                new ElytronPermission("getPrivateCredentials"), new ElytronPermission("getSecurityDomain"),
                                new SocketPermission(TestSuiteEnvironment.getServerAddressNode1() + ":8330", "connect,resolve")),
                        "permissions.xml")
                .addAsManifestResource(Utils.getJBossEjb3XmlAsset("seccontext-entry"), "jboss-ejb3.xml");
    }

    /**
     * Creates deployment with WhoAmI bean - to be placed on the third server.
     */
    @Deployment(name = WAR_WHOAMI_SERVER_CHAIN, managed = false, testable = false)
    @TargetsContainer(SERVER3)
    public static Archive<?> createServerChain3Deployment() {
        return ShrinkWrap.create(JavaArchive.class, WAR_WHOAMI_SERVER_CHAIN + ".jar")
                .addClasses(WhoAmIBean.class, WhoAmI.class, Server2Exception.class)
                .addAsManifestResource(Utils.getJBossEjb3XmlAsset("seccontext-whoami"), "jboss-ejb3.xml");
    }

    @Before
    public void startServer3() throws CommandLineException, IOException, MgmtOperationException {
        server3.resetContainerConfiguration(new ServerConfigurationBuilder()
                .withDeployments(WAR_WHOAMI_SERVER_CHAIN)
                .withAdditionalUsers("another-server", "no-server2-identity")
                .withCliCommands("/subsystem=elytron/simple-permission-mapper=seccontext-server-permissions"
                        + ":write-attribute(name=permission-mappings[1],value={principals=[another-server],"
                        + "permissions=[{class-name=org.wildfly.security.auth.permission.LoginPermission},"
                        + "{class-name=org.wildfly.security.auth.permission.RunAsPrincipalPermission, target-name=admin},"
                        + "{class-name=org.wildfly.security.auth.permission.RunAsPrincipalPermission, target-name="
                        + "no-server2-identity}]})")
                .build());
    }

    /**
     * Shut down servers.
     */
    @AfterClass
    public static void shutdownServer3() throws IOException {
        server3.shutDown();
    }

    /**
     * Setup seccontext-server1.
     */
    @Override
    protected void setupServer1() throws CommandLineException, IOException, MgmtOperationException {
        server1.resetContainerConfiguration(new ServerConfigurationBuilder()
                .withAdditionalUsers("no-server2-identity")
                .withDeployments(FIRST_SERVER_CHAIN_EJB)
                .build());
    }

    /**
     * Setup seccontext-server2.
     */
    @Override
    protected void setupServer2() throws CommandLineException, IOException, MgmtOperationException {
        server2.resetContainerConfiguration(new ServerConfigurationBuilder()
                .withDeployments(JAR_ENTRY_EJB_SERVER_CHAIN)
                .build());
    }

    /**
     * Test forwarding authentication (credential forwarding) works for EJB calls after another authentication forwarding.
     *
     * <pre>
     * When: EJB client calls FirstServerChainBean as admin user and Elytron AuthenticationContext API is used to
     *       authentication forwarding to EntryBean call and Elytron AuthenticationContext API is used to
     *       authentication forwarding to WhoAmIBean call.
     * Then: credentials are reused for EntryBean as well as WhoAmIBean call and it correctly returns "admin" username for both
     *       beans.
     * </pre>
     */
    @Test
    public void testForwardedAuthenticationPropagationChain() throws Exception {
        String[] tripleWhoAmI = SeccontextUtil.switchIdentity("admin", "admin",
                getTripleWhoAmICallable(ReAuthnType.FORWARDED_AUTHENTICATION, null, null, ReAuthnType.FORWARDED_AUTHENTICATION,
                        null, null), ReAuthnType.AC_AUTHENTICATION);
        assertNotNull("The firstServerChainBean.tripleWhoAmI() should return not-null instance", tripleWhoAmI);
        assertArrayEquals("Unexpected principal names returned from tripleWhoAmI", new String[]{"admin", "admin", "admin"},
                tripleWhoAmI);
    }

    /**
     * Test forwarding authentication (credential forwarding) for EJB calls after another authentication forwarding is not
     * possible when given identity does not exist in intermediate server.
     *
     * <pre>
     * When: EJB client calls FirstServerChainBean as no-server2-identity user and Elytron AuthenticationContext API is used to
     *       authentication forwarding to EntryBean call and Elytron AuthenticationContext API is used to
     *       authentication forwarding to WhoAmIBean call.
     * Then: authentication for EntryBean should fail because no-server2-identity does not exist on seccontext-server2.
     * </pre>
     */
    @Test
    public void testForwardedAuthenticationIdentityDoesNotExistOnIntermediateServer() throws Exception {
        String[] tripleWhoAmI = SeccontextUtil.switchIdentity("no-server2-identity", "no-server2-identity",
                getTripleWhoAmICallable(ReAuthnType.FORWARDED_AUTHENTICATION, null, null,
                        ReAuthnType.FORWARDED_AUTHENTICATION, null, null), ReAuthnType.AC_AUTHENTICATION);
        assertNotNull("The firstServerChainBean.tripleWhoAmI() should return not-null instance", tripleWhoAmI);
        assertEquals("Unexpected principal names returned from first call in tripleWhoAmI", "no-server2-identity",
                tripleWhoAmI[0]);
        assertThat("Access should be denied for second call in tripleWhoAmI when identity does not exist on second server",
                tripleWhoAmI[1], isEjbAuthenticationError());
        assertNull("Third call in tripleWhoAmI should not exist", tripleWhoAmI[2]);
    }

    /**
     * Test forwarding authorization (credential less forwarding) works for EJB calls after another authorization forwarding.
     * {@link RunAsPrincipalPermission} is assigned to caller server and another-server identity.
     *
     * <pre>
     * When: EJB client calls FirstServerChainBean as admin user and Elytron AuthenticationContext API is used to
     *       authorization forwarding to EntryBean call with "server" user used as caller server identity and
     *       Elytron AuthenticationContext API is used to
     *       authorization forwarding to WhoAmIBean call with "another-server" user used as caller server identity.
     * Then: EntryBean call is possible and returns "admin" username
     *       and WhoAmIBean call is possible and returns "admin" username.
     * </pre>
     */
    @Test
    public void testForwardedAuthorizationPropagationChain() throws Exception {
        String[] tripleWhoAmI = SeccontextUtil.switchIdentity("admin", "admin",
                getTripleWhoAmICallable(ReAuthnType.FORWARDED_AUTHORIZATION, "server", "server",
                        ReAuthnType.FORWARDED_AUTHORIZATION, "another-server", "another-server"), ReAuthnType.AC_AUTHENTICATION);
        assertNotNull("The firstServerChainBean.tripleWhoAmI() should return not-null instance", tripleWhoAmI);
        assertArrayEquals("Unexpected principal names returned from tripleWhoAmI", new String[]{"admin", "admin", "admin"},
                tripleWhoAmI);
    }

    /**
     * Test forwarding authorization (credential less forwarding) for EJB calls after another authorization forwarding is not
     * possible when given authorization identity does not exist in intermediate server. {@link RunAsPrincipalPermission} is
     * assigned to caller server and another-server identity.
     *
     * <pre>
     * When: EJB client calls FirstServerChainBean as no-server2-identity user and Elytron AuthenticationContext API is used to
     *       authorization forwarding to EntryBean call with "server" user used as caller server identity
     *       and Elytron AuthenticationContext API is used to
     *       authorization forwarding to WhoAmIBean call with "another-server" user used as caller server identity.
     * Then: authorization for EntryBean should fail because no-server2-identity does not exist on seccontext-server2.
     * </pre>
     */
    @Test
    public void testForwardedAuthorizationIdentityDoesNotExistOnIntermediateServer() throws Exception {
        String[] tripleWhoAmI = SeccontextUtil.switchIdentity("no-server2-identity", "no-server2-identity",
                getTripleWhoAmICallable(ReAuthnType.FORWARDED_AUTHORIZATION, "server", "server",
                        ReAuthnType.FORWARDED_AUTHORIZATION, "another-server", "another-server"), ReAuthnType.AC_AUTHENTICATION);
        assertNotNull("The firstServerChainBean.tripleWhoAmI() should return not-null instance", tripleWhoAmI);
        assertEquals("Unexpected principal names returned from first call in tripleWhoAmI", "no-server2-identity",
                tripleWhoAmI[0]);
        assertThat("Access should be denied for second call in tripleWhoAmI when identity does not exist on second server",
                tripleWhoAmI[1], isEjbAuthenticationError());
        assertNull("Third call in tripleWhoAmI should not exist", tripleWhoAmI[2]);
    }

    /**
     * Test forwarding authentication (credential forwarding) for EJB calls is not possible after authorization forwarding.
     * {@link RunAsPrincipalPermission} is assigned to caller server and another-server identity.
     *
     * <pre>
     * When: EJB client calls FirstServerChainBean as admin user and Elytron AuthenticationContext API is used to
     *       authorization forwarding to EntryBean call with "server" user used as caller server identity and
     *       Elytron AuthenticationContext API is used to authentication forwarding to WhoAmIBean.
     * Then: WhoAmIBean call is fails because credentails should not be available on seccontext-server2 after authorization
     *       forwarding.
     * </pre>
     */
    @Test
    public void testForwardingAuthenticationIsNotPossibleAfterForwardingAuthorization() throws Exception {
        String[] tripleWhoAmI = SeccontextUtil.switchIdentity("admin", "admin",
                getTripleWhoAmICallable(ReAuthnType.FORWARDED_AUTHORIZATION, "server", "server",
                        ReAuthnType.FORWARDED_AUTHENTICATION, null, null), ReAuthnType.AC_AUTHENTICATION);
        assertNotNull("The firstServerChainBean.tripleWhoAmI() should return not-null instance", tripleWhoAmI);
        assertEquals("Unexpected principal names returned from first call in tripleWhoAmI", "admin", tripleWhoAmI[0]);
        assertEquals("Unexpected principal names returned from second call in tripleWhoAmI", "admin", tripleWhoAmI[1]);
        assertThat("Access should be denied for third call in tripleWhoAmI", tripleWhoAmI[2], isEjbAuthenticationError());
    }

    protected Callable<String[]> getTripleWhoAmICallable(final ReAuthnType firstType, final String firstUsername,
            final String firstPassword, final ReAuthnType secondType, final String secondUsername, final String secondPassword) {
        return () -> {
            final FirstServerChain bean = SeccontextUtil.lookup(
                    SeccontextUtil.getRemoteEjbName(FIRST_SERVER_CHAIN_EJB, "FirstServerChainBean",
                            FirstServerChain.class.getName(), isEntryStateful()), server1.getApplicationRemotingUrl());
            final String server2Url = server2.getApplicationRemotingUrl();
            final String server3Url = server3.getApplicationRemotingUrl();
            return bean.tripleWhoAmI(new CallAnotherBeanInfo.Builder()
                    .username(firstUsername)
                    .password(firstPassword)
                    .type(firstType)
                    .providerUrl(server2Url)
                    .statefullWhoAmI(isWhoAmIStateful())
                    .build(),
                    new CallAnotherBeanInfo.Builder()
                    .username(secondUsername)
                    .password(secondPassword)
                    .type(secondType)
                    .providerUrl(server3Url)
                    .statefullWhoAmI(isWhoAmIStateful())
                    .lookupEjbAppName(WAR_WHOAMI_SERVER_CHAIN)
                    .build());
        };
    }

    @Override
    protected boolean isEntryStateful() {
        return false;
    }

    @Override
    protected boolean isWhoAmIStateful() {
        return false;
    }

}
