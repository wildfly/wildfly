/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.timerservice.gettimers;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Timer;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

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
