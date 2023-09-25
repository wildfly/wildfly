/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.async;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import jakarta.ejb.Remote;

/**
 * @author Ondrej Chaloupka
 */
@Remote
public interface AsyncBeanRemoteInterface {
    void asyncMethod() throws InterruptedException;
    Future<Boolean> futureMethod() throws InterruptedException, ExecutionException;
}
