/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.integration.elytron.sasl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.List;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.FileUtils;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.AnnotationUtils;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.factory.DSAnnotationProcessor;
import org.apache.directory.server.factory.ServerAnnotationProcessor;
import org.apache.directory.server.ldap.LdapServer;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.network.NetworkUtils;
import org.jboss.as.test.integration.ldap.InMemoryDirectoryServiceFactory;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.ManagedCreateLdapServer;
import org.jboss.as.test.integration.security.common.Utils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.security.auth.client.AuthenticationConfiguration;
import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.auth.client.MatchRule;
import org.wildfly.security.auth.permission.LoginPermission;
import org.wildfly.security.sasl.SaslMechanismSelector;
import org.wildfly.test.integration.elytron.sasl.AbstractSaslTestBase.JmsSetup;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.ConstantPermissionMapper;
import org.wildfly.test.security.common.elytron.MechanismConfiguration;
import org.wildfly.test.security.common.elytron.PermissionRef;
import org.wildfly.test.security.common.elytron.SimpleSaslAuthenticationFactory;
import org.wildfly.test.security.common.elytron.SimpleSecurityDomain;
import org.wildfly.test.security.common.elytron.SimpleSecurityDomain.SecurityDomainRealm;
import org.wildfly.test.security.common.other.MessagingElytronDomainConfigurator;
import org.wildfly.test.security.common.other.SimpleRemotingConnector;
import org.wildfly.test.security.common.other.SimpleSocketBinding;

/**
 * Elytron OTP SASL mechanism tests which use Naming + JMS client. The server setup adds for each tested SASL configuration a
 * new native remoting port and client tests functionality against it.
 *
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({ JmsSetup.class, OtpSaslTestCase.ServerSetup.class })
@Ignore("WFLY-8667")
public class OtpSaslTestCase extends AbstractSaslTestBase {

    private static final String OTP = "OTP";
    private static final String OTP_ALGORITHM = "otp-sha1";
    private static final String OTP_PASSPHRASE = "This is a test.";

    // https://www.ocf.berkeley.edu/~jjlin/jsotp/
    // http://tomeko.net/online_tools/hex_to_base64.php?lang=en
    private static final byte[] OTP_HASH_99 = DatatypeConverter.parseHexBinary("87FEC7768B73CCF9");
    private static final byte[] OTP_HASH_98 = DatatypeConverter.parseHexBinary("33D865A2BF9E5E76");
    private static final int PORT_OTP = 10569;

    private static final int LDAP_PORT = 10389;

    private static final String HOST = Utils.getDefaultHost(false);
    private static final String HOST_FMT = NetworkUtils.formatPossibleIpv6Address(HOST);
    private static final String LDAP_URL = "ldap://" + HOST_FMT + ":" + LDAP_PORT;

    /**
     * Tests that client is able to use OTP SASL mechanism when server allows it.
     */
    @Test
    public void testOtpAccess() throws Exception {
        assertSequenceAndHash(99, OTP_HASH_99);
        Runnable runAndExpectFail = () -> sendAndReceiveMsg(PORT_OTP, true);
        AuthenticationContext.empty()
                .with(MatchRule.ALL,
                        AuthenticationConfiguration.empty()
                                .setSaslMechanismSelector(SaslMechanismSelector.fromString(OTP)))
                .run(runAndExpectFail);
        assertSequenceAndHash(99, OTP_HASH_99);
        AuthenticationContext.empty().with(MatchRule.ALL,
                AuthenticationConfiguration.empty()
                        .setSaslMechanismSelector(SaslMechanismSelector.fromString(OTP)).useName("jduke").usePassword("TeSt"))
                .run(runAndExpectFail);
        assertSequenceAndHash(99, OTP_HASH_99);
        AuthenticationContext.empty()
                .with(MatchRule.ALL,
                        AuthenticationConfiguration.empty()
                                .setSaslMechanismSelector(SaslMechanismSelector.fromString(OTP)).useName("jduke")
                                .usePassword(OTP_PASSPHRASE))
                .run(() -> sendAndReceiveMsg(PORT_OTP, false));
        assertSequenceAndHash(98, OTP_HASH_98);
    }

    /**
     * Check correct user attribute values in the LDAP when using OTP algorithm.
     */
    private void assertSequenceAndHash(Integer expectedSequence, byte[] expectedHash) throws NamingException {
        final Properties env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, LDAP_URL);
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, "uid=admin,ou=system");
        env.put(Context.SECURITY_CREDENTIALS, "secret");
        final LdapContext ctx = new InitialLdapContext(env, null);
        NamingEnumeration<?> namingEnum = ctx.search("dc=wildfly,dc=org", new BasicAttributes("cn", "jduke"));
        if (namingEnum.hasMore()) {
            SearchResult sr = (SearchResult) namingEnum.next();
            Attributes attrs = sr.getAttributes();
            assertEquals("Unexpected sequence number in LDAP attribute", expectedSequence,
                    new Integer(attrs.get("telephoneNumber").get().toString()));
            assertEquals("Unexpected hash value in LDAP attribute", Base64.getEncoder().encodeToString(expectedHash),
                    attrs.get("title").get().toString());
        } else {
            fail("User not found in LDAP");
        }

        namingEnum.close();
        ctx.close();
    }

    /**
     * Setup task which configures Elytron security domains and remoting connectors for this test.
     */
    public static class ServerSetup extends AbstractElytronSetupTask {

        @Override
        protected ConfigurableElement[] getConfigurableElements() {
            List<ConfigurableElement> elements = new ArrayList<>();

            elements.add(ConstantPermissionMapper.builder().withName(NAME)
                    .withPermissions(PermissionRef.fromPermission(new LoginPermission())).build());

            elements.add(new OtpLdapConf());
            elements.add(SimpleSecurityDomain.builder().withName(OTP).withDefaultRealm(OTP).withPermissionMapper(NAME)
                    .withRealms(SecurityDomainRealm.builder().withRealm(OTP).withRoleDecoder("groups-to-roles").build())
                    .build());

            elements.add(MessagingElytronDomainConfigurator.builder().withElytronDomain(OTP).build());

            elements.add(SimpleSaslAuthenticationFactory.builder().withName(OTP).withSaslServerFactory("elytron")
                    .withSecurityDomain(OTP)
                    .addMechanismConfiguration(MechanismConfiguration.builder().withMechanismName(OTP).build()).build());

            elements.add(SimpleSocketBinding.builder().withName(OTP).withPort(PORT_OTP).build());
            elements.add(SimpleRemotingConnector.builder().withName(OTP).withSocketBinding(OTP)
                    .withSaslAuthenticationFactory(OTP).build());

            return elements.toArray(new ConfigurableElement[elements.size()]);
        }

        /**
         * LDAP configurable element - set's and starts LDAP instance and configures ldap-realm in the Elytron. The LDAP realm
         * is used as modifiable realm for testing OTP SASL mechanism.
         */
        //@formatter:off
        @CreateDS(
                name = "WildFlyDS",
                factory = InMemoryDirectoryServiceFactory.class,
                partitions = @CreatePartition(name = "wildfly", suffix = "dc=wildfly,dc=org"),
                allowAnonAccess = true
            )
            @CreateLdapServer(
                transports = @CreateTransport(protocol = "LDAP", address = "localhost", port = LDAP_PORT),
                allowAnonymousAccess = true
            )
        //@formatter:on
        private static class OtpLdapConf implements ConfigurableElement {

            private static DirectoryService directoryService;
            private static LdapServer ldapServer;

            @Override
            public void create(CLIWrapper cli) throws Exception {
                Encoder b64e = Base64.getEncoder();
                directoryService = DSAnnotationProcessor.getDirectoryService();
                DSAnnotationProcessor.injectEntries(directoryService,
                        "dn: dc=wildfly,dc=org\n" //
                                + "dc: jboss\n" //
                                + "objectClass: top\n" //
                                + "objectClass: domain\n" //
                                + "\n" //
                                + "dn: cn=jduke,dc=wildfly,dc=org\n" //
                                + "objectclass: top\n" //
                                + "objectclass: person\n" //
                                + "objectclass: organizationalPerson\n" //
                                + "cn: jduke\n" //
                                + "sn: guest\n" // role ;)
                                + "street: " + OTP_ALGORITHM + "\n" // algorithm
                                + "title: " + b64e.encodeToString(OTP_HASH_99) + "\n" // stored hash
                                + "description: TeSt\n" // seed
                                + "telephoneNumber: 99\n" // sequence
                );
                final ManagedCreateLdapServer createLdapServer = new ManagedCreateLdapServer(
                        (CreateLdapServer) AnnotationUtils.getInstance(CreateLdapServer.class));
                Utils.fixApacheDSTransportAddress(createLdapServer, Utils.getSecondaryTestAddress(null, false));
                ldapServer = ServerAnnotationProcessor.instantiateLdapServer(createLdapServer, directoryService);
                ldapServer.start();

                cli.sendLine(String.format(
                        "/subsystem=elytron/dir-context=%s:add(url=\"%s\",principal=\"uid=admin,ou=system\",credential-reference={clear-text=secret})",
                        OTP, LDAP_URL));
                cli.sendLine(String
                        .format("/subsystem=elytron/ldap-realm=%s:add(dir-context=%s,identity-mapping={rdn-identifier=cn,search-base-dn=\"dc=wildfly,dc=org\","
                                + "otp-credential-mapper={algorithm-from=street, hash-from=title, seed-from=description, sequence-from=telephoneNumber},"
                                + "attribute-mapping=[{from=sn,to=groups}]})", OTP, OTP));
            }

            @Override
            public void remove(CLIWrapper cli) throws Exception {
                // cli.sendLine(String.format("/subsystem=elytron/security-domain=%s:remove()", OTP));
                cli.sendLine(String.format("/subsystem=elytron/ldap-realm=%s:remove()", OTP));
                cli.sendLine(String.format("/subsystem=elytron/dir-context=%s:remove()", OTP));

                ldapServer.stop();
                directoryService.shutdown();
                FileUtils.deleteDirectory(directoryService.getInstanceLayout().getInstanceDirectory());
            }

            @Override
            public String getName() {
                return "ldap-configuration";
            }
        }

    }
}
