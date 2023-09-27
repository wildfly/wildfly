/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.xts.annotation.compensationscoped;

import org.jboss.narayana.compensations.api.CompensationScoped;

import java.io.Serializable;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
@CompensationScoped
public class CompensationScopedData implements Serializable {

    private String value;

    public String get() {
        return value;
    }

    public void set(final String value) {
        this.value = value;
    }
}