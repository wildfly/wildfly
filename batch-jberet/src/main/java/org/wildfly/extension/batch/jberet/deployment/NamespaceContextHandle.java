/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.batch.jberet.deployment;

import org.jboss.as.naming.context.NamespaceContextSelector;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class NamespaceContextHandle implements ContextHandle {

    private final NamespaceContextSelector namespaceContextSelector;

    NamespaceContextHandle(NamespaceContextSelector namespaceContextSelector) {
        this.namespaceContextSelector = namespaceContextSelector;
    }

    @Override
    public Handle setup() {
        NamespaceContextSelector.pushCurrentSelector(namespaceContextSelector);
        return new Handle() {
            @Override
            public void tearDown() {
                NamespaceContextSelector.popCurrentSelector();
            }
        };
    }
}
