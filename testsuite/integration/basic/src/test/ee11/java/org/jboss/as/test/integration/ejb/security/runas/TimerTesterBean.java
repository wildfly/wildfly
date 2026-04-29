/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.security.runas;

import java.security.Principal;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.Resource;
import jakarta.annotation.security.RunAs;
import jakarta.ejb.Remote;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.Timeout;
import jakarta.ejb.Timer;
import jakarta.ejb.TimerService;
import javax.naming.InitialContext;

import org.jboss.ejb3.annotation.SecurityDomain;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 */
@Stateless
@Remote(TimerTester.class)
@RunAs("user2")
@SecurityDomain("ejb3-tests")
public class TimerTesterBean implements TimerTester {
    private static final Logger log = Logger.getLogger(TimerTesterBean.class);

    private static final CountDownLatch latch = new CountDownLatch(1);
    public static boolean timerCalled = false;
    public static Principal callerPrincipal = null;
    public static Set<Principal> calleeCallerPrincipal = null;


    @Resource
    private TimerService timerService;


    @Resource
    private SessionContext ctx;

    public void startTimer(long pPeriod) {
        timerCalled = false;
        timerService.createTimer(new Date(System.currentTimeMillis() + pPeriod), null);

    }

    public static boolean awaitTimerCall() {
        try {
            latch.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return timerCalled;
    }

    @Timeout
    public void timeoutHandler(Timer timer) {
        callerPrincipal = ctx.getCallerPrincipal();
        log.trace("timeoutHanlder() - getCallerPrincipal: " + callerPrincipal);

        try {
            UncheckedStatelessBean tester = (UncheckedStatelessBean) new InitialContext().lookup("java:module/"
                    + UncheckedStatelessBean.class.getSimpleName());
            calleeCallerPrincipal = tester.unchecked();
            log.trace("callee: " + calleeCallerPrincipal);
            timerCalled = true;
        } catch (Exception e) {
            log.error(e);
        }
    }
}
