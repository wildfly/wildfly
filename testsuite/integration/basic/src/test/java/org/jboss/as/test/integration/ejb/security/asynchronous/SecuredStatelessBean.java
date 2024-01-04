/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.security.asynchronous;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.AsyncResult;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.Local;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;
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
