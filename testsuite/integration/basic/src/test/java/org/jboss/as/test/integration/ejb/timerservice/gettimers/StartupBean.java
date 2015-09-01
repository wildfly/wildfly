/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat Middleware LLC, and individual contributors
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
 *
 */

package org.jboss.as.test.integration.ejb.timerservice.gettimers;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Timer;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

/**
 * @author Tomas Hofman (thofman@redhat.com)
 */
@Singleton
public class StartupBean {

    @EJB
    TimerBeanOne timerBeanOne;

    @EJB
    TimerBeanTwo timerBeanTwo;

    @TransactionAttribute(value = TransactionAttributeType.REQUIRED)
    public Map<String, Collection<Timer>> startTimers() {
        Map<String, Collection<Timer>> beanTimersMap = new HashMap<String, Collection<Timer>>();
        timerBeanOne.startTimers();
        beanTimersMap.put(TimerBeanOne.class.getSimpleName(), timerBeanOne.getTimers());
        timerBeanTwo.startTimers();
        beanTimersMap.put(TimerBeanTwo.class.getSimpleName(), timerBeanTwo.getTimers());
        return beanTimersMap;
    }

}
