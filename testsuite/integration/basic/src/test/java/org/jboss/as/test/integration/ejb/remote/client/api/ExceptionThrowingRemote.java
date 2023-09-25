/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.client.api;

/**
 * User: jpai
 */
public interface ExceptionThrowingRemote {

    void alwaysThrowApplicationException(final String state) throws StatefulApplicationException;

    void alwaysThrowSystemException(final String state);
}
