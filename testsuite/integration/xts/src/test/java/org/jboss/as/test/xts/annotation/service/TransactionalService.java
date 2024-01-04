/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.xts.annotation.service;

import jakarta.jws.WebMethod;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public interface TransactionalService {

    @WebMethod
    boolean isTransactionActive();

}
