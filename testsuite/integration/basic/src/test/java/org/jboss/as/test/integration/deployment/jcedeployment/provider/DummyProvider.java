/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.deployment.jcedeployment.provider;

import java.security.Provider;

/**
 * Testing JCE provider which provides one dummy cipher only.
 *
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
public final class DummyProvider extends Provider {

    public static String PROVIDER_NAME = "DP";
    public static String DUMMY_CIPHER = "dummycipher";

    public DummyProvider() {
        super(PROVIDER_NAME, 0.1, "Dummy Provider v0.1");

        put("Cipher.DummyAlg/DummyMode/DummyPadding", DummyCipherSpi.class.getName());
    }

}
