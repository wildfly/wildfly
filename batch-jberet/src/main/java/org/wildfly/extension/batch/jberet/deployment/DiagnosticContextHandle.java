/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.batch.jberet.deployment;

import java.util.Map;

import org.jboss.logging.MDC;
import org.jboss.logging.NDC;

/**
 * A {@code ContextHandle} to propagate and clear mapped diagnostic context (MDC)
 * and nested diagnostic context (NDC) data used in logging.
 */
class DiagnosticContextHandle implements ContextHandle {
    private final Map<String, Object> mdcMap;
    private final String ndcTop;

    DiagnosticContextHandle(final Map<String, Object> mdcMap, final String ndcTop) {
        this.mdcMap = mdcMap;
        this.ndcTop = ndcTop;
    }

    @Override
    public Handle setup() {
        for (Map.Entry<String, Object> e : mdcMap.entrySet()) {
            MDC.put(e.getKey(), e.getValue());
        }

        if (ndcTop != null) {
            NDC.clear();
            NDC.push(ndcTop);
        }

        return new Handle() {
            @Override
            public void tearDown() {
                MDC.clear();
                NDC.clear();
            }
        };
    }
}
