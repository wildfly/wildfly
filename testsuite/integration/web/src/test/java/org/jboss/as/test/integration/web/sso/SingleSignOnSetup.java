/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.sso;

import java.util.Collections;
import java.util.List;

import org.jboss.as.test.integration.web.security.WebTestsSecurityDomainSetup;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.CredentialReference;
import org.wildfly.test.security.common.elytron.Path;
import org.wildfly.test.security.common.elytron.PropertyFileBasedDomain.Builder;
import org.wildfly.test.security.common.elytron.SimpleKeyStore;
import org.wildfly.test.undertow.common.SingleSignOnSetting;
import org.wildfly.test.undertow.common.UndertowApplicationSecurityDomain;

import static org.jboss.as.test.integration.web.sso.SSOTestBase.PASSWORD;
import static org.jboss.as.test.integration.web.sso.SSOTestBase.PASSWORD_2;
import static org.jboss.as.test.integration.web.sso.SSOTestBase.ROLE;
import static org.jboss.as.test.integration.web.sso.SSOTestBase.USERNAME;
import static org.jboss.as.test.integration.web.sso.SSOTestBase.USERNAME_2;

public class SingleSignOnSetup extends WebTestsSecurityDomainSetup {

    @Override
    protected List<ConfigurableElement> getAdditionalElements() {
        return Collections.singletonList(
            SimpleKeyStore.builder()
                .withName("sso")
                .withPath(Path.builder().withPath("sso.keystore").withRelativeTo("jboss.server.config.dir").build())
                .withType("PKCS12")
                .withCredentialReference(CredentialReference.builder().withClearText("password").build())
                .build());
    }

    @Override
    protected Builder withUsers(Builder builder) {
        return builder.withUser(USERNAME, PASSWORD, ROLE)
                .withUser(USERNAME_2, PASSWORD_2, ROLE);
    }

    @Override
    protected ConfigurableElement getApplicationSecurityDomainMapping() {
        return UndertowApplicationSecurityDomain.builder()
            .withName("sso-domain")
            .withSecurityDomain(getSecurityDomainName())
            .withSingleSignOnSettings(
                SingleSignOnSetting.builder()
                    .withKeyStore("sso")
                    .withKeyAlias("localhost")
                    .withCredentialReference(
                        CredentialReference.builder()
                            .withClearText("password")
                            .build()
                    )
                    .build()
                )
            .build();
    }




}
