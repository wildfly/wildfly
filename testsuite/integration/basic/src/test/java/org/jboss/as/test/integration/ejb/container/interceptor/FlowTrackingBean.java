/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.container.interceptor;

import javax.ejb.LocalBean;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptors;
import javax.interceptor.InvocationContext;

import org.jboss.logging.Logger;

/**
 * A {@link FlowTracker} implementation. There are 2 EE interceptors used - {@link NonContainerInterceptor} and the class itself
 * ({@link #aroundInvoke(InvocationContext)}).
 *
 * @author Jaikiran Pai
 */
@Stateless
@Interceptors(NonContainerInterceptor.class)
@Remote(FlowTracker.class)
@LocalBean
public class FlowTrackingBean implements FlowTracker {

    private static final Logger logger = Logger.getLogger(FlowTrackingBean.class);

    public static final String CONTEXT_DATA_KEY = "foo-bar";

    @AroundInvoke
    protected Object aroundInvoke(InvocationContext invocationContext) throws Exception {
        logger.trace("@AroundInvoke on bean invoked");
        final String skipInterceptor = (String) invocationContext.getContextData().get(FlowTrackingBean.CONTEXT_DATA_KEY);
        if (skipInterceptor != null && this.getClass().getName().equals(skipInterceptor)) {
            return invocationContext.proceed();
        }
        return this.getClass().getName() + " " + invocationContext.proceed();
    }

    public String echo(final String msg) {
        logger.trace("EJB invoked!!!");
        return msg;
    }

}
