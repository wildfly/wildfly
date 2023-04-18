/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
