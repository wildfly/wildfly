/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.async;

import java.util.concurrent.Future;
import jakarta.ejb.Remote;

/**
 * Remote interface for testing cancel method asynchronously.
 */
@Remote
public interface AsyncBeanCancelRemoteInterface {
    Future<String> asyncRemoteCancelMethod() throws InterruptedException;
}
