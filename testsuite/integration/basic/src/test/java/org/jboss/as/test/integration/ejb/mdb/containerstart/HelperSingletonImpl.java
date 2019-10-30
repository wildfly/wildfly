/*
 * JBoss, Home of Professional Open Source
 * Copyright (c) 2011, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @authors tag. See the copyright.txt in the
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
package org.jboss.as.test.integration.ejb.mdb.containerstart;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.ejb.Remote;
import javax.ejb.Singleton;
import javax.ejb.Startup;

/**
 * @author <a href="cdewolf@redhat.com">Carlo de Wolf</a>
 */
@Singleton
@Startup
@Remote(HelperSingleton.class)
public class HelperSingletonImpl implements HelperSingleton {
    public static CyclicBarrier barrier = new CyclicBarrier(2);

    public int await(String where, long timeout, TimeUnit unit) throws BrokenBarrierException, TimeoutException, InterruptedException {
        return barrier.await(timeout, unit);
    }

    public void reset() {
        barrier.reset();
    }
}
