/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.manualmode.ejb.ssl.beans;


import jakarta.ejb.Stateful;
import java.util.concurrent.Future;
import jakarta.ejb.AsyncResult;
import jakarta.ejb.Asynchronous;

/**
 * @author Jan Martiska
 */
@Stateful
public class StatefulBean implements  StatefulBeanRemote {

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
