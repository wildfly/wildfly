/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component.interceptors;

import java.util.Map;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class StoredLogDiagnosticContext {

    static final Object KEY = new Object();
    final Map<String, Object> mdc;
    final String ndc;

    StoredLogDiagnosticContext(final Map<String, Object> mdc, final String ndc) {
        this.mdc = mdc;
        this.ndc = ndc;
    }

    Map<String, Object> getMdc() {
        return mdc;
    }

    String getNdc() {
        return ndc;
    }
}
