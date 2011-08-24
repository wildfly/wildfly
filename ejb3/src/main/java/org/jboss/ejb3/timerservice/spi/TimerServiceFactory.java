/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.ejb3.timerservice.spi;

import javax.ejb.TimerService;
import java.util.List;

/**
 * Creates an EJB TimerService for TimedObjectInvokers.
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 * @version $Revision: $
 */
public interface TimerServiceFactory {
    /**
     * Create an EJB TimerService for the given TimedObjectInvoker.
     *
     * @param invoker the TimerObjectInvoker for which a TimerService must be created
     * @return the EJB TimerService for the TimerObjectInvoker
     * @throws NullPointerException if invoker is null
     */
    TimerService createTimerService(TimedObjectInvoker invoker);

    /**
     * Restores the timers held by the specified timer service, and creates any new auto timers
     * that are needed.
     * <p/>
     * Once a TimerObjectInvoker is ready to receive callbacks, it should call this function.
     *
     * @param timerService the timerService that should restore its timers
     * @param autoTimers   the auto timers that were found in the deployment.
     * @throws NullPointerException if timerService is null
     */
    void restoreTimerService(TimerService timerService, List<ScheduleTimer> autoTimers);

    /**
     * Suspends the timers held by the specified timer service.
     * Note that after this method there should be reference anymore to the TimerObjectInvoker.
     *
     * @param timerService
     * @throws NullPointerException if timerService is null
     */
    void suspendTimerService(TimerService timerService);
}
