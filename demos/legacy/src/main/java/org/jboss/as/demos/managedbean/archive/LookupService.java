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

package org.jboss.as.demos.managedbean.archive;

import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import javax.naming.Context;
import javax.naming.NamingException;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author John Bailey
 */
public class LookupService implements Service<Void> {
    private final InjectedValue<Context> lookupContext = new InjectedValue<Context>();

    private final String lookupName;

    private static volatile BeanWithSimpleInjected bean;

    public LookupService(String lookupName) {
        this.lookupName = lookupName;
    }

    public void start(StartContext context) throws StartException {
        try {
            bean = (BeanWithSimpleInjected) lookupContext.getValue().lookup(lookupName);
            synchronized (LookupService.class) {
                LookupService.class.notifyAll();
            }
        } catch (NamingException e) {
            throw new StartException(e);
        }
    }

    public void stop(StopContext context) {
    }

    public Void getValue() throws IllegalStateException, IllegalArgumentException {
        return null;
    }

    public Injector<Context> getLookupContextInjector() {
        return lookupContext;
    }

    public static synchronized  BeanWithSimpleInjected getBean() {
        if(bean == null) {
            try {
                LookupService.class.wait(SECONDS.toMillis(10));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return bean;
    }
}
