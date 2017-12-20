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

package org.jboss.as.test.integration.ejb.remote.async;

import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 *
 */
@Stateless
public class StatelessRemoteBean implements RemoteInterface, LocalInterface {

    public static volatile CountDownLatch doneLatch = new CountDownLatch(1);
    public static volatile CountDownLatch startLatch = new CountDownLatch(1);

    public static void reset() {
        doneLatch = new CountDownLatch(1);
        startLatch = new CountDownLatch(1);
    }

    @Asynchronous
    public void modifyArray(final String[] array) {
        try {
            if(!startLatch.await(5, TimeUnit.SECONDS)) {
                throw new RuntimeException("Invocation was not asynchronous");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        array[0] = "goodbye";
        doneLatch.countDown();
    }


    @Asynchronous
    public Future<String> hello() {
        return new AsyncResult<String>("hello");
    }

    @Override
    @Asynchronous
    public Future<Void> alwaysFail() throws AppException {
        throw new AppException("Intentionally thrown");
    }

    @Override
    @Asynchronous
    public void passByReference(final String[] array) {
        try {
            if(!startLatch.await(5, TimeUnit.SECONDS)) {
                throw new RuntimeException("Invocation was not asynchronous");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        array[0] = "goodbye";
        doneLatch.countDown();
    }
}
