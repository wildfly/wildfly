/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.async.classloading;

import java.util.concurrent.Future;

import jakarta.ejb.AsyncResult;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.Stateless;

/**
 * @author baranowb
 *
 */
@Stateless
public class AsyncRemoteEJB implements AsyncRemote {

    @Override
    @Asynchronous
    public Future<ReturnObject> testAsync(final String x) {
        return new AsyncResult<ReturnObject>(new ReturnObject(x));
    }

    @Override
    @Asynchronous
    public Future<ReturnObject> testAsyncNull(String x) {
        return null;
    }

}
