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

package org.jboss.as.test.integration.web.security.cert;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.security.common.SecurityTestConstants;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.elytron.ClientCertUndertowDomainMapper;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.CredentialReference;
import org.wildfly.test.security.common.elytron.KeyStoreRealm;
import org.wildfly.test.security.common.elytron.Path;
import org.wildfly.test.security.common.elytron.PropertyFileAuthzBasedDomain;
import org.wildfly.test.security.common.elytron.SimpleKeyManager;
import org.wildfly.test.security.common.elytron.SimpleKeyStore;
import org.wildfly.test.security.common.elytron.SimpleServerSslContext;
import org.wildfly.test.security.common.elytron.SimpleTrustManager;
import org.wildfly.test.security.common.elytron.UserWithRoles;
import org.wildfly.test.security.common.elytron.X500AttributePrincipalDecoder;
import org.wildfly.test.security.common.other.SimpleSocketBinding;
import org.wildfly.test.undertow.common.elytron.SimpleHttpsListener;

/**
 * {@code AbstractElytronSetupTask} Creates Elytron server-ssl-context and key/trust stores and configures
 * CLIENT-CERT authentication.
 *
 * @author Jan Stourac
 */
public class WebCERTTestsElytronSetup extends AbstractElytronSetupTask {

    private static final String NAME = WebCERTTestsElytronSetup.class.getSimpleName();
    private static final String SECURITY_DOMAIN_NAME = "cert-test";
    private static final String HTTPS_LISTENER_NAME = "https-test";

    @Override
    protected void setup(final ModelControllerClient modelControllerClient) throws Exception {
        super.setup(modelControllerClient);
    }

    @Override
    protected ConfigurableElement[] getConfigurableElements() {
        return new ConfigurableElement[]{
                SimpleKeyStore.builder().withName(NAME + SecurityTestConstants.SERVER_KEYSTORE)
                        .withPath(Path.builder().withPath(WebCERTTestsSetup.getServerKeystoreFile().getAbsolutePath()
                        ).build())
                        .withCredentialReference(CredentialReference.builder().withClearText(WebCERTTestsSetup
                                .getPassword()).build())
                        .build(),
                SimpleKeyStore.builder().withName(NAME + SecurityTestConstants.SERVER_TRUSTSTORE)
                        .withPath(Path.builder().withPath(WebCERTTestsSetup.getServerTruststoreFile().getAbsolutePath
                                ()).build())
                        .withCredentialReference(CredentialReference.builder().withClearText(WebCERTTestsSetup
                                .getPassword()).build())
                        .build(),
                SimpleKeyManager.builder().withName(NAME)
                        .withKeyStore(NAME + SecurityTestConstants.SERVER_KEYSTORE)
                        .withCredentialReference(CredentialReference.builder().withClearText(WebCERTTestsSetup
                                .getPassword()).build())
                        .build(),
                SimpleTrustManager.builder().withName(NAME)
                        .withKeyStore(NAME + SecurityTestConstants.SERVER_TRUSTSTORE)
                        .build(),
                KeyStoreRealm.builder().withName(NAME)
                        .withKeyStore(NAME + SecurityTestConstants.SERVER_TRUSTSTORE)
                        .build(),
                X500AttributePrincipalDecoder.builder().withName(NAME)
                        .withOid("2.5.4.3")
                        .withMaximumSegments(1)
                        .build(),
                PropertyFileAuthzBasedDomain.builder().withName(SECURITY_DOMAIN_NAME)
                        .withAuthnRealm(NAME)
                        .withPrincipalDecoder(NAME)
                        .withUser(UserWithRoles.builder().withName("test client").withRoles("gooduser").build())
                        .withUser(UserWithRoles.builder().withName("test client 2").withRoles("superuser").build())
                        .build(),
                ClientCertUndertowDomainMapper.builder().withName(SECURITY_DOMAIN_NAME).withSecurityDomain
                        (SECURITY_DOMAIN_NAME).build(),
                SimpleServerSslContext.builder().withName(NAME)
                        .withKeyManagers(NAME)
                        .withTrustManagers(NAME)
                        .withSecurityDomain(NAME)
                        .withProtocols("TLSv1.2")
                        .withNeedClientAuth(true)
                        .withAuthenticationOptional(true)
                        .withSecurityDomain(SECURITY_DOMAIN_NAME)
                        .build(),

                SimpleSocketBinding.builder().withName(HTTPS_LISTENER_NAME).withPort(WebCERTTestsSetup.HTTPS_PORT)
                        .build(),
                SimpleHttpsListener.builder().withName(HTTPS_LISTENER_NAME).withSocketBinding(HTTPS_LISTENER_NAME).
                        withSslContext(NAME).build()
        };
    }

    @Override
    protected void tearDown(ModelControllerClient modelControllerClient) throws Exception {
        super.tearDown(modelControllerClient);
    }
}
