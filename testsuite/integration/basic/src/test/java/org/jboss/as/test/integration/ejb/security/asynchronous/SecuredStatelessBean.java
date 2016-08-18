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

package org.jboss.as.test.integration.ejb.security.asynchronous;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.ejb3.annotation.SecurityDomain;
import org.junit.Assert;


/**
 * @author <a href="mailto:kabir.khan@jboss.org">Kabir Khan</a>
 */
@Stateless
@SecurityDomain("async-security-test")
@Remote(SecuredStatelessRemote.class)
@Local(SecuredStatelessLocal.class)
@Asynchronous
public class SecuredStatelessBean implements SecuredStatelessRemote, SecuredStatelessLocal {

    public static volatile CountDownLatch startLatch = new CountDownLatch(1);

    public static void reset() {
        startLatch = new CountDownLatch(1);
    }

    @PermitAll
    public Future<Boolean> uncheckedMethod() throws InterruptedException {
        try {
            if (!startLatch.await(5, TimeUnit.SECONDS)) {
                throw new RuntimeException("Invocation was not asynchronous");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return new AsyncResult<Boolean>(true);
    }

    @DenyAll
    public Future<Boolean> excludedMethod() throws InterruptedException {
        try {
            if (!startLatch.await(5, TimeUnit.SECONDS)) {
                throw new RuntimeException("Invocation was not asynchronous");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return new AsyncResult<Boolean>(true);
    }

    @RolesAllowed("allowed")
    public Future<Boolean> method() throws InterruptedException, ExecutionException {
        try {
            if (!startLatch.await(5, TimeUnit.SECONDS)) {
                throw new RuntimeException("Invocation was not asynchronous");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        SecuredStatelessLocal localSearchedBean = null;
        try {
            Context context = new InitialContext();
            localSearchedBean = (SecuredStatelessLocal) context.lookup("java:module/" + SecuredStatelessBean.class.getSimpleName() + "!"
                    + SecuredStatelessLocal.class.getName());
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }

        final CountDownLatch latchLocal = new CountDownLatch(1);
        final Future<Boolean> future = localSearchedBean.localSecured(latchLocal);
        latchLocal.countDown();
        boolean result = future.get();
        Assert.assertTrue(result);

        return new AsyncResult<Boolean>(true);
    }

    @RolesAllowed("allowed")
    public Future<Boolean> localSecured(CountDownLatch latchLocal) throws InterruptedException {
        latchLocal.await(5, TimeUnit.SECONDS);
        return new AsyncResult<Boolean>(true);
    }

}
