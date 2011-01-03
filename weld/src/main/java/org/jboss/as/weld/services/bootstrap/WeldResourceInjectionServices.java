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
package org.jboss.as.weld.services.bootstrap;

import javax.enterprise.inject.spi.InjectionPoint;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.weld.injection.spi.ResourceInjectionServices;
import org.jboss.weld.injection.spi.helpers.AbstractResourceServices;

public class WeldResourceInjectionServices extends AbstractResourceServices implements Service<WeldResourceInjectionServices>,
        ResourceInjectionServices {

    public static final ServiceName SERVICE_NAME = ServiceName.of("WeldResourceInjectionServices");

    private static final Logger log = Logger.getLogger("org.jboss.weld");

    private static final String USER_TRANSACTION_LOCATION = "java:comp/UserTransaction";
    private static final String USER_TRANSACTION_CLASS_NAME = "javax.transaction.UserTransaction";
    private static final String HANDLE_DELEGATE_CLASS_NAME = "javax.ejb.spi.HandleDelegate";
    private static final String TIMER_SERVICE_CLASS_NAME = "javax.ejb.TimerService";
    private static final String ORB_CLASS_NAME = "org.omg.CORBA.ORB";

    private final Context context;

    @Override
    public void start(StartContext context) throws StartException {
    }

    @Override
    public void stop(StopContext context) {
    }

    @Override
    public WeldResourceInjectionServices getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    protected static String getEJBResourceName(InjectionPoint injectionPoint, String proposedName) {
        if (injectionPoint.getType() instanceof Class<?>) {
            Class<?> type = (Class<?>) injectionPoint.getType();
            if (USER_TRANSACTION_CLASS_NAME.equals(type.getName())) {
                return USER_TRANSACTION_LOCATION;
            } else if (HANDLE_DELEGATE_CLASS_NAME.equals(type.getName())) {
                log.warn("Injection of @Resource HandleDelegate not supported in managed beans. Injection Point: "
                        + injectionPoint);
                return proposedName;
            } else if (ORB_CLASS_NAME.equals(type.getName())) {
                log.warn("Injection of @Resource ORB not supported in managed beans. Injection Point: " + injectionPoint);
                return proposedName;
            } else if (TIMER_SERVICE_CLASS_NAME.equals(type.getName())) {
                log.warn("Injection of @Resource TimerService not supported in managed beans. Injection Point: "
                        + injectionPoint);
                return proposedName;
            }
        }
        return proposedName;
    }


    public WeldResourceInjectionServices() {
        try {
            this.context = new InitialContext();
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected Context getContext() {
        return context;
    }

    @Override
    protected String getResourceName(InjectionPoint injectionPoint) {
        return getEJBResourceName(injectionPoint, super.getResourceName(injectionPoint));
    }

    @Override
    public void cleanup() {
    }

}
