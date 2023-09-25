/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.async;

import jakarta.ejb.ApplicationException;

/**
 *
 */
@ApplicationException
public class AppException extends Exception {

    public AppException(final String message) {
        super(message);
    }
}
