/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.integration.ejb.remote.client.api;

import java.util.concurrent.Future;

import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;

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
        logger.info(this.getClass().getSimpleName() + " echoing message " + message);
        return message;
    }

    @Asynchronous
    @Override
    public Future<String> asyncEcho(String message, long delayInMilliSec) {
        logger.info("Going to delay the echo of \"" + message + "\" for " + delayInMilliSec + " milli seconds");
        try {
            Thread.sleep(delayInMilliSec);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        logger.info(this.getClass().getSimpleName() + " echoing message: " + message);
        return new AsyncResult<String>(message);
    }

    @Override
    public EchoRemote getBusinessObject() {
        return null;
    }
}
