/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.demos.ejb3.mbean;

import org.jboss.as.demos.ejb3.archive.SimpleSingletonLocal;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

/**
 * @author Jaikiran Pai
 */
public class SingletonBeanLookupThread implements Callable<Object> {


    private String jndiName;

    private int numTimes;

    private CountDownLatch latch;

    public SingletonBeanLookupThread(CountDownLatch latch, String jndiName, int numTimes) {
        this.jndiName = jndiName;
        this.numTimes = numTimes;
        this.latch = latch;
    }

    @Override
    public Object call() throws Exception {
        try {

            for (int i = 0; i < numTimes; i++) {
                Context ctx = new InitialContext();
                SimpleSingletonLocal bean = (SimpleSingletonLocal) ctx.lookup(jndiName);
                // invoke a no-op since the singleton bean instance gets created on invocation
                bean.doNothing();
            }
        } finally {
            latch.countDown();
        }
        return null;
    }
}
