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

package org.wildfly.test.integration.elytron.application;

import static org.jboss.as.test.shared.CliUtils.asAbsolutePath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.CharBuffer;
import java.security.KeyStore;

import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.logging.ControllerLogger;
import org.jboss.as.test.integration.management.util.CLIOpResult;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.dmr.ModelNode;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.CredentialReference;
import org.wildfly.test.security.common.elytron.Path;
import org.wildfly.test.security.common.elytron.SimpleCredentialStore;

/**
 * Tests credential store (CS) implementation in Elytron. This test case uses existing CS keystore prepared in testsuite module
 * configuration - check keytool maven plugin used in {@code pom.xml} and also credential store CLI commands in
 * {@code modify-elytron.config.cli} file.
 *
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(CredentialStoreTestCase.ElytronSetup.class)
public class CredentialStoreTestCase extends AbstractCredentialStoreTestCase {

    private static final String NAME = CredentialStoreTestCase.class.getSimpleName();
    private static final String CS_NAME_CLEAR = NAME + "-clear";
    private static final String CS_NAME_CRED_REF = NAME + "-cred-ref";
    private static final String CS_NAME_MODIFIABLE = NAME + "-modifiable";

    private static final String ALIAS_SECRET = "alias-secret";
    private static final String ALIAS_PASSWORD = "alias-password";

    /**
     * Tests unmodifiable credential store when backing keystore has password in clear text.
     */
    @Test
    public void testClearPassword() throws Exception {
        testUnmodifiableInternally(CS_NAME_CLEAR);
    }

    /**
     * Test unmodifiable credential store when backing keystore has password provided as credential reference.
     */
    @Test
    public void testCredentialReference() throws Exception {
        testUnmodifiableInternally(CS_NAME_CRED_REF);
    }

    /**
     * Tests credential store with automatically created PKCS12 keystore.
     */
    @Test
    @Ignore
    public void testCredentialStoreCreating() throws Exception {
        String storeName = NAME;
        File tempFolder = Utils.createTemporaryFolder(storeName);
        String fileName = System.currentTimeMillis() + ".p12";
        File ksFile = new File(tempFolder, fileName);
        assertTrue(tempFolder.isDirectory());
        assertFalse(ksFile.exists());
        try {
            try (CLIWrapper cli = new CLIWrapper(true)) {
                cli.sendLine(String.format("/path=%s:add(path=\"%s\")", storeName, asAbsolutePath(tempFolder)));
                SimpleCredentialStore storeConfig = SimpleCredentialStore.builder().withName(storeName)
                        .withKeyStorePath(Path.builder().withPath(fileName).withRelativeTo(storeName).build())
                        .withCredential(CredentialReference.builder().withClearText("pkcs12pass").build())
                        .withKeyStoreType("PKCS12").withModifiable(true).withCreate(true).withAlias("elytron", "rocks!")
                        .build();
                try {
                    storeConfig.create(cli);

                    assertContainsAliases(cli, storeName, "elytron");

                    assertTrue(ksFile.exists());

                    cli.sendLine(String.format(
                            "/subsystem=elytron/credential-store=%s/alias=another-secret:add(secret-value=\"%1$s\")",
                            storeName));

                    assertCredentialValue(storeName, "elytron", "rocks!");
                    assertCredentialValue(storeName, "another-secret", storeName);
                } finally {
                    // this should remove alias "elytron" from KeyStore file and remove credential {@value NAME} from domain
                    // model
                    storeConfig.remove(cli);
                }
                // KeyStore file should not be removed after
                assertTrue(ksFile.exists());
                KeyStore ks = KeyStore.getInstance("PKCS12");
                try (FileInputStream fis = new FileInputStream(ksFile)) {
                    ks.load(fis, "pkcs12pass".toCharArray());
                    assertEquals(1, ks.size());
                    assertTrue(ks.aliases().nextElement().contains("another-secret"));
                }
            }
        } finally {
            FileUtils.deleteQuietly(tempFolder);
        }
    }

    /**
     * Tests reload operation on credential store instance.
     */
    @Test
    public void testReloadCredentialStore() throws Exception {
        final String alias = "cs-reload-test";
        try (CLIWrapper cli = new CLIWrapper(true)) {
            try {
                cli.sendLine(String.format("/subsystem=elytron/credential-store=%s/alias=%s:add(secret-value=\"%s\"",
                        CS_NAME_MODIFIABLE, alias, alias));
                assertCredentialNotFound(CS_NAME_CLEAR, alias);
                assertCredentialNotFound(CS_NAME_CRED_REF, alias);
                cli.sendLine(String.format("/subsystem=elytron/credential-store=%s:reload()", CS_NAME_CRED_REF));
                assertCredentialNotFound(CS_NAME_CLEAR, alias);
                assertCredentialValue(CS_NAME_CRED_REF, alias, alias);
                cli.sendLine(String.format("/subsystem=elytron/credential-store=%s:reload()", CS_NAME_CLEAR));
                assertCredentialValue(CS_NAME_CLEAR, alias, alias);
            } finally {
                cli.sendLine(
                        String.format("/subsystem=elytron/credential-store=%s/alias=%s:remove()", CS_NAME_MODIFIABLE, alias));
            }
        }
    }

    /**
     * Tests change password operation on credential store instance.
     */
    @Test
    public void testUpdatePasswordCredentialStore() throws Exception {
        final String alias = "cs-update-test";
        final String password = "password";
        final String updatedPassword = "passw0rd!";
        try (CLIWrapper cli = new CLIWrapper(true)) {
            try {
                cli.sendLine(String.format("/subsystem=elytron/credential-store=%s/alias=%s:add(secret-value=\"%s\"",
                        CS_NAME_MODIFIABLE, alias, alias));
                assertCredentialValue(CS_NAME_MODIFIABLE, alias, alias);
                cli.sendLine(String.format("/subsystem=elytron/credential-store=%s/alias=%s:write-attribute(name=secret-value, value=\"%s\")",
                        CS_NAME_MODIFIABLE, alias, updatedPassword));
                assertCredentialValue(CS_NAME_MODIFIABLE, alias, updatedPassword);
            } finally {
                cli.sendLine(
                        String.format("/subsystem=elytron/credential-store=%s/alias=%s:remove()", CS_NAME_MODIFIABLE, alias), true);
            }
        }
    }

    /**
     * Tests add-remove-add operations sequence on an alias in credential store.
     */
    @Test
    public void testAddRemoveAddAlias() throws Exception {
        final String alias = "addremoveadd";
        try (CLIWrapper cli = new CLIWrapper(true)) {
            try {
                cli.sendLine(String.format("/subsystem=elytron/credential-store=%s/alias=%s:add(secret-value=\"%s\"",
                        CS_NAME_MODIFIABLE, alias, alias));
                assertCredentialValue(CS_NAME_MODIFIABLE, alias, alias);
                cli.sendLine(
                        String.format("/subsystem=elytron/credential-store=%s/alias=%s:remove()", CS_NAME_MODIFIABLE, alias));
                cli.sendLine(String.format("/subsystem=elytron/credential-store=%s/alias=%s:add(secret-value=\"%s\"",
                        CS_NAME_MODIFIABLE, alias, alias + alias));
                assertCredentialValue(CS_NAME_MODIFIABLE, alias, alias + alias);
                cli.sendLine(String.format("/subsystem=elytron/credential-store=%s/alias=%s:add(secret-value=\"%s\"",
                        CS_NAME_MODIFIABLE, alias, alias), true);
                ModelNode result = ModelNode.fromString(cli.readOutput());
                assertEquals("result " + result, result.get(ModelDescriptionConstants.FAILURE_DESCRIPTION).asString(), ControllerLogger.ROOT_LOGGER.duplicateResourceAddress(PathAddress.parseCLIStyleAddress(String.format("/subsystem=elytron/credential-store=%s/alias=%s",
                        CS_NAME_MODIFIABLE, alias))).getMessage());
            } finally {
                cli.sendLine(
                        String.format("/subsystem=elytron/credential-store=%s/alias=%s:remove()", CS_NAME_MODIFIABLE, alias));
            }
        }
    }

    /**
     * Tests creating credential with long secret.
     */
    @Test
    public void testLongSecret() throws Exception {
        final String secret = generateString(10 * 1024 + 1, 's');
        assertAliasAndSecretSupported(CS_NAME_MODIFIABLE, "longsecret", secret);
    }

    /**
     * Tests creating credential with empty secret.
     */
    @Test
    public void testEmptySecret() throws Exception {
        assertAliasAndSecretSupported(CS_NAME_MODIFIABLE, "emptysecret", "");
        assertAliasAndSecretSupported(CS_NAME_MODIFIABLE, "nullsecret", null);
    }

    /**
     * Tests creating credential with long alias.
     */
    @Test
    public void testLongAlias() throws Exception {
        final String alias = generateString(1024 + 1, 'a');
        assertAliasAndSecretSupported(CS_NAME_MODIFIABLE, alias, "test");
    }

    private void testUnmodifiableInternally(final String csName) throws IOException, Exception {
        try (CLIWrapper cli = new CLIWrapper(true)) {

            assertContainsAliases(cli, csName, ALIAS_PASSWORD, ALIAS_SECRET);

            cli.sendLine(String.format("/subsystem=elytron/credential-store=%s/alias=%1$s:add(secret-value=%1$s)", csName),
                    true);
            final CLIOpResult opResult = cli.readAllAsOpResult();
            assertFalse("Adding alias to non-modifiable credential store should fail.", opResult.isIsOutcomeSuccess());
        }

        assertCredentialValue(csName, ALIAS_PASSWORD, "password");
        assertCredentialValue(csName, ALIAS_SECRET, "secret");

        assertCredentialNotFound(csName, csName);
    }

    private static String generateString(int len, char c) {
        return CharBuffer.allocate(len).toString().replace('\0', c);
    }

    /**
     * Configures 2 unmodifiable credential stores (CS) on the top of one existing JCEKS keystore - One CS uses plain text
     * keystore password, the second uses credential reference (pointing to the first CS). Then configures one modifiable CS.
     */
    static class ElytronSetup extends AbstractElytronSetupTask {

        @Override
        protected ConfigurableElement[] getConfigurableElements() {
            final Path jceksPath = Path.builder().withPath("cred-store.jceks").withRelativeTo("jboss.server.config.dir")
                    .build();
            final CredentialReference credRefPwd = CredentialReference.builder().withClearText("password").build();
            final CredentialReference credRefRef = CredentialReference.builder().withStore(CS_NAME_CLEAR)
                    .withAlias(ALIAS_PASSWORD).build();

            return new ConfigurableElement[] {
                    SimpleCredentialStore.builder().withName(CS_NAME_CLEAR).withKeyStorePath(jceksPath)
                            .withKeyStoreType("JCEKS").withCreate(false).withModifiable(false).withCredential(credRefPwd)
                            .build(),
                    SimpleCredentialStore.builder().withName(CS_NAME_CRED_REF).withKeyStorePath(jceksPath).withCreate(false)
                            .withModifiable(false).withCredential(credRefRef).build(),
                    SimpleCredentialStore.builder().withName(CS_NAME_MODIFIABLE).withKeyStorePath(jceksPath)
                            .withModifiable(true).withCredential(credRefPwd).build() };
        }
    }
}
