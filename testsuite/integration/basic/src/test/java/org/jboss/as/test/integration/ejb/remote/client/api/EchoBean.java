/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.client.api;

import java.util.concurrent.Future;

import jakarta.annotation.Resource;
import jakarta.ejb.AsyncResult;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.Remote;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

/**
 * User: jpai
 */
@Stateless
@Remote(EchoRemote.class)
public class EchoBean implements EchoRemote {

    @Resource
    private SessionContext sessionContext;

    @Inject
    private RequestScopedBean requestScopedBean;

    private static final Logger logger = Logger.getLogger(EchoBean.class);

    @Override
    public String echo(String message) {
        logger.trace(this.getClass().getSimpleName() + " echoing message " + message);
        return message;
    }

    @Asynchronous
    @Override
    public Future<String> asyncEcho(final String message, final long delayInMilliSec) {
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
        return sessionContext.getBusinessObject(EchoRemote.class);
    }

    @Override
    public boolean testRequestScopeActive() {
        requestScopedBean.setState(10);
        requestScopedBean.setState(requestScopedBean.getState() + 10);
        return requestScopedBean.getState() == 20;
    }

    @Override
    public ValueWrapper getValue() {
        return new ValueWrapper();
    }
}
