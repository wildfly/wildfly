/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.security.elytron;

import java.security.KeyStore;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;

import org.jboss.as.controller.capability.RuntimeCapability;
import org.wildfly.security.auth.server.SecurityRealm;

/**
 * Capabilies for the elytron integration section of the legacy security subsystem.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
class Capabilities {

    private static final String CAPABILITY_BASE = "org.wildfly.security.";

    static final String KEY_STORE_CAPABILITY = CAPABILITY_BASE + "key-store";

    static final RuntimeCapability<Void> KEY_STORE_RUNTIME_CAPABILITY =  RuntimeCapability
            .Builder.of(KEY_STORE_CAPABILITY, true, KeyStore.class)
            .build();

    static final String KEY_MANAGER_CAPABILITY = CAPABILITY_BASE + "key-manager";

    static final RuntimeCapability<Void> KEY_MANAGER_RUNTIME_CAPABILITY =  RuntimeCapability
            .Builder.of(KEY_MANAGER_CAPABILITY, true, KeyManager.class)
            .build();

    static final String SECURITY_REALM_CAPABILITY =  CAPABILITY_BASE + "security-realm";

    static final RuntimeCapability<Void> SECURITY_REALM_RUNTIME_CAPABILITY = RuntimeCapability
            .Builder.of(SECURITY_REALM_CAPABILITY, true, SecurityRealm.class)
            .build();

    static final String TRUST_MANAGER_CAPABILITY = CAPABILITY_BASE + "trust-manager";

    static final RuntimeCapability<Void> TRUST_MANAGER_RUNTIME_CAPABILITY =  RuntimeCapability
            .Builder.of(TRUST_MANAGER_CAPABILITY, true, TrustManager.class)
            .build();

}
