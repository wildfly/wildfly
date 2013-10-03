package org.wildfly.jberet.services;

import org.jboss.as.naming.context.NamespaceContextSelector;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class NamespaceContextHandle implements ContextHandle {

    private final NamespaceContextSelector namespaceContextSelector;

    NamespaceContextHandle() {
        this.namespaceContextSelector = NamespaceContextSelector.getCurrentSelector();
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
