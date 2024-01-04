/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.elytron.application;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.CredentialReference;
import org.wildfly.test.security.common.elytron.Path;
import org.wildfly.test.security.common.elytron.SimpleCredentialStore;

import static org.junit.Assert.assertEquals;

/**
 * Tests credential store (CS) implementation in Elytron. This testcase uses several scenarios:
 * <ul>
 * <li>CS created on the top of existing keystore</li>
 * <li>CS with keystore created from scratch</li>
 * <li>keystore password as credential-reference (entry in another CS)</li>
 * <li>keystore file survives removing CS from domain model</li>
 * <li>several keystore types used for backing the credential store</li>
 * </ul>
 * The configuration for this test case is partly in module configuration (check keytool maven plugin used in {@code pom.xml}
 * and also credential store CLI commands in {@code modify-elytron.config.cli} file
 *
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(CredentialStoreI18NTestCase.ElytronSetup.class)
public class CredentialStoreI18NTestCase extends AbstractCredentialStoreTestCase {

    private static final String NAME = CredentialStoreI18NTestCase.class.getSimpleName();

    private static final String LOWER = "lower";

    private static final String STR_SYMBOLS = "@!#?$%^&*()%+-{}";
    private static final String STR_CHINESE = "用戶名";
    private static final String STR_ARABIC = "اسمالمستخدم";
    private static final String STR_EURO_LOWER = "žščřžďťňäáéěëíýóůúü";
    private static final String STR_EURO_UPPER = "ŽŠČŘŽĎŤŇÄÁÉĚËÍÝÓŮÚÜ";
    private static final String STR_ALL = STR_SYMBOLS + STR_CHINESE + STR_ARABIC + STR_EURO_LOWER + STR_EURO_UPPER;

    /**
     * Tests using localized strings as secrets.
     */
    @Test
    public void testI18NSecret() throws Exception {
        assertAliasAndSecretSupported(NAME, "i18nsecret", STR_ALL);
    }

    /**
     * Tests using localized strings as alias.
     */
    @Test
    public void testI18NAlias() throws Exception {
        assertAliasAndSecretSupported(NAME, STR_CHINESE, "test");
        assertAliasAndSecretSupported(NAME, STR_ARABIC, "test");
        assertAliasAndSecretSupported(NAME, STR_EURO_LOWER, "test");
    }

    /**
     * Tests for CS aliases case-sensitiveness
     */
    @Test
    public void testAliasesCaseSensitive() throws Exception {
        assertCredentialValue(NAME, LOWER, LOWER);
        try (CLIWrapper cli = new CLIWrapper(true)) {
            Assert.assertFalse(cli.sendLine("/subsystem=elytron/credential-store=CredentialStoreI18NTestCase:add-alias(alias=LOWER, secret-value=password)", true));
            ModelNode result = ModelNode.fromString(cli.readOutput());
            assertEquals("result " + result, result.get(ModelDescriptionConstants.FAILURE_DESCRIPTION).asString(),
                    String.format("WFLYELY00913: Credential alias '%s' of credential type '%s' already exists in the store", "LOWER", PasswordCredential.class.getName()));
        }
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

            return new ConfigurableElement[] { SimpleCredentialStore.builder().withName(NAME).withKeyStorePath(jceksPath)
                    .withKeyStoreType("JCEKS").withCreate(false).withModifiable(true).withCredential(credRefPwd)
                    .withAlias(LOWER, LOWER)
                    .build() };
        }
    }
}
