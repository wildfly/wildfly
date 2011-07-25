/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.ejb3.timerservice;

import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.component.singleton.SingletonComponent;
import org.jboss.as.ejb3.component.stateless.StatelessSessionComponent;
import org.jboss.ejb3.timerservice.spi.ScheduleTimer;
import org.jboss.ejb3.timerservice.spi.TimedObjectInvoker;
import org.jboss.ejb3.timerservice.spi.TimerServiceFactory;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import javax.ejb.TimerService;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MSC service with a silly name that provides a wrapper around the EJB timer service.
 *
 * @author Stuart Douglas
 */
public class TimerServiceService extends ForwardingTimerService implements Service<TimerService> {

    public static final ServiceName SERVICE_NAME = ServiceName.of("ejb", "timerService");

    private volatile org.jboss.ejb3.timerservice.api.TimerService timerService;
    private final InjectedValue<TimerServiceFactory> timerServiceFactoryInjectedValue = new InjectedValue<TimerServiceFactory>();
    private final InjectedValue<EJBComponent> ejbComponentInjectedValue = new InjectedValue<EJBComponent>();

    private final Map<Method, List<AutoTimer>> autoTimers;
    private final ClassLoader classLoader;

    public TimerServiceService(final Map<Method, List<AutoTimer>> autoTimers, final ClassLoader classLoader) {
        this.autoTimers = autoTimers;
        this.classLoader = classLoader;
    }

    @Override
    public synchronized void start(final StartContext context) throws StartException {
        final TimerServiceFactory timerServiceFactory = timerServiceFactoryInjectedValue.getValue();

        final EJBComponent component = ejbComponentInjectedValue.getValue();
        final TimedObjectInvoker invoker;
        if(component instanceof SingletonComponent) {
            invoker = new SingletonTimedObjectInvokerImpl((SingletonComponent) component, classLoader);
        } else if(component instanceof StatelessSessionComponent) {
            invoker = new StatelessTimedObjectInvokerImpl((StatelessSessionComponent) component, classLoader);
        } else {
            throw new StartException("TimerService can only be used with singleton and stateless components");
        }
        final org.jboss.ejb3.timerservice.api.TimerService timerService = (org.jboss.ejb3.timerservice.api.TimerService) timerServiceFactory.createTimerService(invoker);
        final List<ScheduleTimer> timers = new ArrayList<ScheduleTimer>();

        for(Map.Entry<Method, List<AutoTimer>> entry : autoTimers.entrySet()) {
            for(AutoTimer timer : entry.getValue()) {
                timers.add(new ScheduleTimer(entry.getKey(), timer.getScheduleExpression(), timer.getTimerConfig()));
            }
        }

        timerServiceFactory.restoreTimerService(timerService, timers);

        this.timerService = timerService;
    }

    @Override
    public synchronized void stop(final StopContext context) {
        timerServiceFactoryInjectedValue.getValue().suspendTimerService(timerService);
        timerService = null;
    }

    @Override
    protected TimerService delegate() {
        final TimerService timerService = this.timerService;
        if(timerService == null) {
            throw new IllegalStateException("TimerService is not started");
        }
        return timerService;
    }

    @Override
    public TimerService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public InjectedValue<TimerServiceFactory> getTimerServiceFactoryInjectedValue() {
        return timerServiceFactoryInjectedValue;
    }

    public InjectedValue<EJBComponent> getEjbComponentInjectedValue() {
        return ejbComponentInjectedValue;
    }
}
