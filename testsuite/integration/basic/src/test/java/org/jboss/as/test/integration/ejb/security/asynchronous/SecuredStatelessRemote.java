/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.security.asynchronous;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author <a href="mailto:kabir.khan@jboss.org">Kabir Khan</a>
 */
public interface SecuredStatelessRemote {

    Future<Boolean> uncheckedMethod() throws InterruptedException;

    Future<Boolean> excludedMethod() throws InterruptedException;

    Future<Boolean> method() throws InterruptedException, ExecutionException;

}
