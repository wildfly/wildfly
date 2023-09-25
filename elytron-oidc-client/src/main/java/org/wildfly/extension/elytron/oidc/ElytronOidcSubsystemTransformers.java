/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.extension.elytron.oidc;

import static org.wildfly.extension.elytron.oidc.ElytronOidcDescriptionConstants.AUTHENTICATION_REQUEST_FORMAT;
import static org.wildfly.extension.elytron.oidc.ElytronOidcDescriptionConstants.CLIENT_KEY_ALIAS;
import static org.wildfly.extension.elytron.oidc.ElytronOidcDescriptionConstants.CLIENT_KEY_PASSWORD;
import static org.wildfly.extension.elytron.oidc.ElytronOidcDescriptionConstants.CLIENT_KEYSTORE_FILE;
import static org.wildfly.extension.elytron.oidc.ElytronOidcDescriptionConstants.CLIENT_KEYSTORE_PASSWORD;
import static org.wildfly.extension.elytron.oidc.ElytronOidcDescriptionConstants.CLIENT_KEYSTORE_TYPE;
import static org.wildfly.extension.elytron.oidc.ElytronOidcDescriptionConstants.REQUEST_OBJECT_CONTENT_ENCRYPTION_ALGORITHM;
import static org.wildfly.extension.elytron.oidc.ElytronOidcDescriptionConstants.REQUEST_OBJECT_ENCRYPTION_ALGORITHM;
import static org.wildfly.extension.elytron.oidc.ElytronOidcDescriptionConstants.REQUEST_OBJECT_SIGNING_ALGORITHM;
import static org.wildfly.extension.elytron.oidc.ElytronOidcDescriptionConstants.PROVIDER;
import static org.wildfly.extension.elytron.oidc.ElytronOidcDescriptionConstants.REALM;
import static org.wildfly.extension.elytron.oidc.ElytronOidcDescriptionConstants.SECURE_DEPLOYMENT;
import static org.wildfly.extension.elytron.oidc.ElytronOidcDescriptionConstants.SECURE_SERVER;
import static org.wildfly.extension.elytron.oidc.ElytronOidcExtension.VERSION_1_0_0;
import static org.wildfly.extension.elytron.oidc.ElytronOidcExtension.VERSION_2_0_0;
import static org.wildfly.extension.elytron.oidc.ElytronOidcExtension.VERSION_3_0_0;
import static org.wildfly.security.http.oidc.Oidc.SCOPE;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;

public class ElytronOidcSubsystemTransformers implements ExtensionTransformerRegistration {

    @Override
    public String getSubsystemName() {
        return ElytronOidcExtension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration registration) {

        ChainedTransformationDescriptionBuilder chainedBuilder = TransformationDescriptionBuilder.Factory.createChainedSubystemInstance(registration.getCurrentSubsystemVersion());
        // 3.0.0 (WildFly 30) to 2.0.0 (WildFly 29)
        from3(chainedBuilder);
        // 2.0.0 (WildFly 29) to 1.0.0 (WildFly 28)
        from2(chainedBuilder);

        chainedBuilder.buildAndRegister(registration, new ModelVersion[] { VERSION_2_0_0, VERSION_1_0_0 });
    }

    private static void from3(ChainedTransformationDescriptionBuilder chainedBuilder) {
        ResourceTransformationDescriptionBuilder builder = chainedBuilder.createBuilder(VERSION_3_0_0, VERSION_2_0_0);
        builder.addChildResource(PathElement.pathElement(SECURE_SERVER))
                .getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.DEFINED, AUTHENTICATION_REQUEST_FORMAT)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, AUTHENTICATION_REQUEST_FORMAT)
                .addRejectCheck(RejectAttributeChecker.DEFINED, REQUEST_OBJECT_CONTENT_ENCRYPTION_ALGORITHM)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, REQUEST_OBJECT_CONTENT_ENCRYPTION_ALGORITHM)
                .addRejectCheck(RejectAttributeChecker.DEFINED, REQUEST_OBJECT_ENCRYPTION_ALGORITHM)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, REQUEST_OBJECT_ENCRYPTION_ALGORITHM)
                .addRejectCheck(RejectAttributeChecker.DEFINED, REQUEST_OBJECT_SIGNING_ALGORITHM)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, REQUEST_OBJECT_SIGNING_ALGORITHM)
                .addRejectCheck(RejectAttributeChecker.DEFINED, CLIENT_KEYSTORE_FILE)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, CLIENT_KEYSTORE_FILE)
                .addRejectCheck(RejectAttributeChecker.DEFINED, CLIENT_KEYSTORE_PASSWORD)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, CLIENT_KEYSTORE_PASSWORD)
                .addRejectCheck(RejectAttributeChecker.DEFINED, CLIENT_KEY_PASSWORD)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, CLIENT_KEY_PASSWORD)
                .addRejectCheck(RejectAttributeChecker.DEFINED, CLIENT_KEYSTORE_TYPE)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, CLIENT_KEYSTORE_TYPE)
                .addRejectCheck(RejectAttributeChecker.DEFINED, CLIENT_KEY_ALIAS)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, CLIENT_KEY_ALIAS)
                .addRejectCheck(RejectAttributeChecker.DEFINED, SCOPE)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, SCOPE);

        builder.addChildResource(PathElement.pathElement(SECURE_DEPLOYMENT))
                .getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.DEFINED, AUTHENTICATION_REQUEST_FORMAT)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, AUTHENTICATION_REQUEST_FORMAT)
                .addRejectCheck(RejectAttributeChecker.DEFINED, REQUEST_OBJECT_CONTENT_ENCRYPTION_ALGORITHM)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, REQUEST_OBJECT_CONTENT_ENCRYPTION_ALGORITHM)
                .addRejectCheck(RejectAttributeChecker.DEFINED, REQUEST_OBJECT_ENCRYPTION_ALGORITHM)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, REQUEST_OBJECT_ENCRYPTION_ALGORITHM)
                .addRejectCheck(RejectAttributeChecker.DEFINED, REQUEST_OBJECT_SIGNING_ALGORITHM)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, REQUEST_OBJECT_SIGNING_ALGORITHM)
                .addRejectCheck(RejectAttributeChecker.DEFINED, CLIENT_KEYSTORE_FILE)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, CLIENT_KEYSTORE_FILE)
                .addRejectCheck(RejectAttributeChecker.DEFINED, CLIENT_KEYSTORE_PASSWORD)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, CLIENT_KEYSTORE_PASSWORD)
                .addRejectCheck(RejectAttributeChecker.DEFINED, CLIENT_KEY_PASSWORD)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, CLIENT_KEY_PASSWORD)
                .addRejectCheck(RejectAttributeChecker.DEFINED, CLIENT_KEYSTORE_TYPE)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, CLIENT_KEYSTORE_TYPE)
                .addRejectCheck(RejectAttributeChecker.DEFINED, CLIENT_KEY_ALIAS)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, CLIENT_KEY_ALIAS)
                .addRejectCheck(RejectAttributeChecker.DEFINED, SCOPE)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, SCOPE);

        builder.addChildResource(PathElement.pathElement(REALM))
                .getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.DEFINED, AUTHENTICATION_REQUEST_FORMAT)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, AUTHENTICATION_REQUEST_FORMAT)
                .addRejectCheck(RejectAttributeChecker.DEFINED, REQUEST_OBJECT_CONTENT_ENCRYPTION_ALGORITHM)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, REQUEST_OBJECT_CONTENT_ENCRYPTION_ALGORITHM)
                .addRejectCheck(RejectAttributeChecker.DEFINED, REQUEST_OBJECT_ENCRYPTION_ALGORITHM)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, REQUEST_OBJECT_ENCRYPTION_ALGORITHM)
                .addRejectCheck(RejectAttributeChecker.DEFINED, REQUEST_OBJECT_SIGNING_ALGORITHM)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, REQUEST_OBJECT_SIGNING_ALGORITHM)
                .addRejectCheck(RejectAttributeChecker.DEFINED, CLIENT_KEYSTORE_FILE)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, CLIENT_KEYSTORE_FILE)
                .addRejectCheck(RejectAttributeChecker.DEFINED, CLIENT_KEYSTORE_PASSWORD)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, CLIENT_KEYSTORE_PASSWORD)
                .addRejectCheck(RejectAttributeChecker.DEFINED, CLIENT_KEY_PASSWORD)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, CLIENT_KEY_PASSWORD)
                .addRejectCheck(RejectAttributeChecker.DEFINED, CLIENT_KEYSTORE_TYPE)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, CLIENT_KEYSTORE_TYPE)
                .addRejectCheck(RejectAttributeChecker.DEFINED, CLIENT_KEY_ALIAS)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, CLIENT_KEY_ALIAS);

        builder.addChildResource(PathElement.pathElement(PROVIDER))
                .getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.DEFINED, AUTHENTICATION_REQUEST_FORMAT)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, AUTHENTICATION_REQUEST_FORMAT)
                .addRejectCheck(RejectAttributeChecker.DEFINED, REQUEST_OBJECT_CONTENT_ENCRYPTION_ALGORITHM)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, REQUEST_OBJECT_CONTENT_ENCRYPTION_ALGORITHM)
                .addRejectCheck(RejectAttributeChecker.DEFINED, REQUEST_OBJECT_ENCRYPTION_ALGORITHM)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, REQUEST_OBJECT_ENCRYPTION_ALGORITHM)
                .addRejectCheck(RejectAttributeChecker.DEFINED, REQUEST_OBJECT_SIGNING_ALGORITHM)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, REQUEST_OBJECT_SIGNING_ALGORITHM)
                .addRejectCheck(RejectAttributeChecker.DEFINED, CLIENT_KEYSTORE_FILE)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, CLIENT_KEYSTORE_FILE)
                .addRejectCheck(RejectAttributeChecker.DEFINED, CLIENT_KEYSTORE_PASSWORD)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, CLIENT_KEYSTORE_PASSWORD)
                .addRejectCheck(RejectAttributeChecker.DEFINED, CLIENT_KEY_PASSWORD)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, CLIENT_KEY_PASSWORD)
                .addRejectCheck(RejectAttributeChecker.DEFINED, CLIENT_KEYSTORE_TYPE)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, CLIENT_KEYSTORE_TYPE)
                .addRejectCheck(RejectAttributeChecker.DEFINED, CLIENT_KEY_ALIAS)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, CLIENT_KEY_ALIAS);
    }

    private static void from2(ChainedTransformationDescriptionBuilder chainedBuilder) {
        ResourceTransformationDescriptionBuilder builder = chainedBuilder.createBuilder(VERSION_2_0_0, VERSION_1_0_0);
        builder.rejectChildResource(PathElement.pathElement(SECURE_SERVER));
    }
}
