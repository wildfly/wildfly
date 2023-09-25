/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.client.api;

import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;

/**
 * User: jpai
 */
@Stateless
@Remote (ExceptionThrowingRemote.class)
public class ExceptionThrowingBean implements ExceptionThrowingRemote {

    @Override
    public void alwaysThrowApplicationException(String state) throws StatefulApplicationException {
        throw new StatefulApplicationException(state);
    }

    @Override
    public void alwaysThrowSystemException(String state) {
        throw new RuntimeException(state);
    }
}
