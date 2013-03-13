/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.single.ejb.remotecall;

import java.util.concurrent.Future;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Stateless;

import org.jboss.ejb3.annotation.Clustered;

/**
 * @author Kabir Khan
 * @author Ondrej Chaloupka
 */
@Stateless
@Clustered
@Remote(RemoteAsyncInterface.class)
public class StatelessAsyncClusteredBean implements RemoteAsyncInterface {
    private static volatile boolean methodCalled = false;

    @EJB
    SynchronizationSingletonInterface synchro;

    @Asynchronous
    public void voidTest() throws InterruptedException {
        synchro.waitForLatchNumber1();
        synchro.countDownLatchNumber2();
        methodCalled = true;
    }

    @Asynchronous
    public Future<Integer> futureGetTest(Integer number) throws InterruptedException {
        synchro.waitForLatchNumber1();
        methodCalled = true;
        return new AsyncResult<Integer>(number);
    }

    public void resetMethodCalled() {
        methodCalled = false;
    }

    public boolean getMethodCalled() {
        return methodCalled;
    }
}
