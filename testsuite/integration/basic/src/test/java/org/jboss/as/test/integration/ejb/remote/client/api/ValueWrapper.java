/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.client.api;

import java.io.Serializable;

/**
 * Java serialization will not invoke the class initializer during unmarshalling, resulting in
 * shouldBeNilAfterUnmarshalling being left as null.  This helps test if JBoss marshalling does the same.
 */
public class ValueWrapper implements Serializable {
    public static String INITIALIZER_CONSTANT = "FIVE";

    private transient String shouldBeNilAfterUnmarshalling = initializer();

    private String initializer() {
        return INITIALIZER_CONSTANT;
    }

    public String getShouldBeNilAfterUnmarshalling() {
        return shouldBeNilAfterUnmarshalling;
    }

}
