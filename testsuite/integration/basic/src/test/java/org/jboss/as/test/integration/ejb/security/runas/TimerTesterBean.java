/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ejb.security.runas;

import java.security.Principal;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;
import javax.annotation.security.RunAs;
import javax.ejb.Remote;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerService;
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
        timerService.createTimer(new Date(new Date().getTime() + pPeriod), null);

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
