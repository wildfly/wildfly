/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.client.api;

import java.util.concurrent.Future;

import jakarta.ejb.AsyncResult;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;

import org.jboss.logging.Logger;

/**
 * User: jpai
 */
@Stateless
@Remote(EchoRemote.class)
@Interceptors(InterceptorOne.class)
public class InterceptedEchoBean implements EchoRemote {

    private static final Logger logger = Logger.getLogger(InterceptedEchoBean.class);

    @Override
    @Interceptors(InterceptorTwo.class)
    public String echo(String message) {
        logger.trace(this.getClass().getSimpleName() + " echoing message " + message);
        return message;
    }

    @Asynchronous
    @Override
    public Future<String> asyncEcho(String message, long delayInMilliSec) {
        logger.trace("Going to delay the echo of \"" + message + "\" for " + delayInMilliSec + " milliseconds");
        try {
            Thread.sleep(delayInMilliSec);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        logger.trace(this.getClass().getSimpleName() + " echoing message: " + message);
        return new AsyncResult<String>(message);
    }

    @Override
    public EchoRemote getBusinessObject() {
        return null;
    }

    @Override
    public boolean testRequestScopeActive() {
        //not used
        return false;
    }

    @Override
    public ValueWrapper getValue() {
        return null;
    }
}
