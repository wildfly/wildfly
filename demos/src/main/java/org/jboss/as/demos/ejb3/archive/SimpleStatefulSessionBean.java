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
package org.jboss.as.demos.ejb3.archive;

import javax.annotation.Resource;
import javax.ejb.Local;
import javax.ejb.SessionContext;
import javax.ejb.Stateful;
import javax.interceptor.ExcludeClassInterceptors;
import javax.interceptor.Interceptors;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@Stateful
@Interceptors(SimpleInterceptor.class)
@Local({SimpleStatefulSessionLocal.class, EchoService.class})
public class SimpleStatefulSessionBean implements SimpleStatefulSessionLocal {
    @Resource
    private SessionContext context;

    private String state;

    public String echo(String msg) {
        System.out.println("Called echo on " + this);
        return "Echo " + msg + ":" + state;
    }

    @ExcludeClassInterceptors
    @Override
    public EchoService getEchoService() {
        return context.getBusinessObject(EchoService.class);
    }

    public void setState(String s) {
        System.out.println("Called setState on " + this);
        this.state = s;
    }

    @Override
    public String getState() {
        return this.state;
    }
}
