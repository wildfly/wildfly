/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb.timer.servlet;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.servlet.annotation.WebServlet;

import org.jboss.as.test.clustering.cluster.ejb.timer.beans.AutoPersistentTimerBean;
import org.jboss.as.test.clustering.cluster.ejb.timer.beans.AutoTransientTimerBean;
import org.jboss.as.test.clustering.cluster.ejb.timer.beans.CalendarPersistentTimerBean;
import org.jboss.as.test.clustering.cluster.ejb.timer.beans.CalendarTransientTimerBean;
import org.jboss.as.test.clustering.cluster.ejb.timer.beans.IntervalPersistentTimerBean;
import org.jboss.as.test.clustering.cluster.ejb.timer.beans.IntervalTransientTimerBean;
import org.jboss.as.test.clustering.cluster.ejb.timer.beans.ManualTimerBean;
import org.jboss.as.test.clustering.cluster.ejb.timer.beans.SingleActionPersistentTimerBean;
import org.jboss.as.test.clustering.cluster.ejb.timer.beans.SingleActionTransientTimerBean;
import org.jboss.as.test.clustering.cluster.ejb.timer.beans.TimerBean;

/**
 * @author Paul Ferraro
 */
@WebServlet(urlPatterns = { TimerServlet.SERVLET_PATH })
public class TimerServlet extends AbstractTimerServlet {
    private static final long serialVersionUID = 5393197778939188763L;
    private static final String SERVLET_NAME = "timer";
    static final String SERVLET_PATH = "/" + SERVLET_NAME;

    public static URI createURI(URL baseURL, String module) throws URISyntaxException {
        return baseURL.toURI().resolve(new StringBuilder(SERVLET_NAME).append('?').append(MODULE).append('=').append(module).toString());
    }

    public static final Set<Class<? extends TimerBean>> PERSISTENT_TIMER_CLASSES = Set.of(AutoPersistentTimerBean.class, CalendarPersistentTimerBean.class, IntervalPersistentTimerBean.class, SingleActionPersistentTimerBean.class);
    public static final Set<Class<? extends TimerBean>> TRANSIENT_TIMER_CLASSES = Set.of(AutoTransientTimerBean.class, CalendarTransientTimerBean.class, IntervalTransientTimerBean.class, SingleActionTransientTimerBean.class);
    public static final Set<Class<? extends TimerBean>> TIMER_CLASSES = Stream.of(PERSISTENT_TIMER_CLASSES, TRANSIENT_TIMER_CLASSES).flatMap(Set::stream).collect(Collectors.toSet());
    public static final Set<Class<? extends ManualTimerBean>> MANUAL_PERSISTENT_TIMER_CLASSES = Set.of(CalendarPersistentTimerBean.class, IntervalPersistentTimerBean.class, SingleActionPersistentTimerBean.class);
    public static final Set<Class<? extends ManualTimerBean>> MANUAL_TRANSIENT_TIMER_CLASSES = Set.of(CalendarTransientTimerBean.class, IntervalTransientTimerBean.class, SingleActionTransientTimerBean.class);
    public static final Set<Class<? extends ManualTimerBean>> MANUAL_TIMER_CLASSES = Stream.of(MANUAL_PERSISTENT_TIMER_CLASSES, MANUAL_TRANSIENT_TIMER_CLASSES).flatMap(Set::stream).collect(Collectors.toSet());
    public static final Set<Class<? extends TimerBean>> AUTO_TIMER_CLASSES = Set.of(AutoPersistentTimerBean.class, AutoTransientTimerBean.class);
    public static final Set<Class<? extends TimerBean>> SINGLE_ACTION_TIMER_CLASSES = Set.of(SingleActionPersistentTimerBean.class, SingleActionTransientTimerBean.class);

    public TimerServlet() {
        super(MANUAL_TIMER_CLASSES, AUTO_TIMER_CLASSES);
    }
}
