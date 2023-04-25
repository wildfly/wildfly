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

import static org.wildfly.extension.elytron.oidc.ElytronOidcDescriptionConstants.SECURE_SERVER;
import static org.wildfly.extension.elytron.oidc.ElytronOidcExtension.VERSION_1_0_0;
import static org.wildfly.extension.elytron.oidc.ElytronOidcExtension.VERSION_2_0_0;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
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
        // 2.0.0 (WildFly 29) to 1.0.0 (WildFly 28)
        from2(chainedBuilder);

        chainedBuilder.buildAndRegister(registration, new ModelVersion[] { VERSION_1_0_0 });
    }

    private static void from2(ChainedTransformationDescriptionBuilder chainedBuilder) {
        ResourceTransformationDescriptionBuilder builder = chainedBuilder.createBuilder(VERSION_2_0_0, VERSION_1_0_0);
        builder.rejectChildResource(PathElement.pathElement(SECURE_SERVER));
    }
}
