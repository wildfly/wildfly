/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.manualmode.ejb.ssl.beans;

import jakarta.annotation.security.PermitAll;
import jakarta.ejb.AsyncResult;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.Stateless;

import java.util.concurrent.Future;

/**
 * @author Ondrej Chaloupka
 * @author Jan Martiska
 */
@Stateless
@PermitAll
public class StatelessBean implements StatelessBeanRemote {
    @Override
    public String sayHello() {
        return ANSWER;
    }

    @Override
    @Asynchronous
    public Future<String> sayHelloAsync() {
        return new AsyncResult<String>(ANSWER);
    }

}
