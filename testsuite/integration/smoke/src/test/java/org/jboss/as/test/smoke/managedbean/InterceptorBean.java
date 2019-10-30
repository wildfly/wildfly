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
package org.jboss.as.test.smoke.managedbean;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

import org.jboss.logging.Logger;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class InterceptorBean {

    private final Logger log = Logger.getLogger(InterceptorBean.class);

    private String name;

    @PostConstruct
    public void initializeInterceptor(InvocationContext context) {
        log.trace("Post constructing it");
        name = "#InterceptorBean#";
    }

    @PreDestroy
    public void destroyInterceptor(InvocationContext context) {
        log.trace("Pre destroying it");
    }

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        if (!context.getMethod().getName().equals("echo")) {
            return context.proceed();
        }
        log.trace("-----> Intercepting call to " + context.getMethod().getDeclaringClass() + "." +  context.getMethod().getName() + "()");
        return name + context.proceed();
    }

}
