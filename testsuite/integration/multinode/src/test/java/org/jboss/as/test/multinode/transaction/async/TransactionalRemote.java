/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.multinode.transaction.async;

import java.util.concurrent.Future;
import jakarta.ejb.Remote;

/**
 * Remote interface for testing beans being called.
 *
 * @author Ondrej Chaloupka
 */
@Remote
public interface TransactionalRemote {
    Future<Integer> transactionStatus();

    Future<Integer> asyncWithRequired();
}
