/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.deployment.jcedeployment.provider;

/**
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
public class DummyCipherSpi extends CipherAdapter {
    public DummyCipherSpi() {
        super(DummyProvider.DUMMY_CIPHER);
    }
}
