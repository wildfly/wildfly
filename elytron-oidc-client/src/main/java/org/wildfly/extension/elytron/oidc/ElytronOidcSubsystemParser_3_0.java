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

/**
 * Subsystem parser for the Elytron OpenID Connect subsystem.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
public class ElytronOidcSubsystemParser_3_0 extends ElytronOidcSubsystemParser_2_0 {

    /**
     * The name space used for the {@code subsystem} element
     */
    public static final String NAMESPACE_3_0 = "urn:wildfly:elytron-oidc-client:3.0";

    @Override
    String getNameSpace() {
        return NAMESPACE_3_0;
    }
}
