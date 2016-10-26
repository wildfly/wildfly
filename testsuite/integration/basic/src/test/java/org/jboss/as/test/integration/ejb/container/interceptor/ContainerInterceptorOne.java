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

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

import org.jboss.logging.Logger;

/**
 * Simple interceptor, which adds its classname in front of the result of {@link InvocationContext#proceed()}. Result of the
 * proceed() stays untouched in case the {@link InvocationContext#getContextData()} contains classname of this interceptor under
 * the {@link FlowTrackingBean#CONTEXT_DATA_KEY} key.
 *
 * @author Jaikiran Pai
 */
public class ContainerInterceptorOne {

    private static final Logger logger = Logger.getLogger(ContainerInterceptorOne.class);

    @AroundInvoke
    public Object aroundInvoke(final InvocationContext invocationContext) throws Exception {
        logger.trace("Container interceptor invoked!!!");
        final String skipInterceptor = (String) invocationContext.getContextData().get(FlowTrackingBean.CONTEXT_DATA_KEY);
        if (skipInterceptor != null && this.getClass().getName().equals(skipInterceptor)) {
            return invocationContext.proceed();
        }
        return this.getClass().getName() + " " + invocationContext.proceed();
    }
}
