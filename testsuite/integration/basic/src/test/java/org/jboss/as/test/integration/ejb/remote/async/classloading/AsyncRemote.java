/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.async.classloading;

import java.util.concurrent.Future;

import jakarta.ejb.Asynchronous;
import jakarta.ejb.Remote;

/**
 * @author baranowb
 *
 */
@Remote
public interface AsyncRemote {
    @Asynchronous
    Future<ReturnObject> testAsync(String x);

    @Asynchronous
    Future<ReturnObject> testAsyncNull(String x);
}
