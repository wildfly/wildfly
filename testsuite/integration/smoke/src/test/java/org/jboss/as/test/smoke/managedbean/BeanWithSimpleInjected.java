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

import org.jboss.logging.Logger;

import javax.annotation.ManagedBean;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptors;
import javax.interceptor.InvocationContext;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
@ManagedBean("BeanWithSimpleInjected")
@Interceptors(InterceptorBean.class)
public class BeanWithSimpleInjected extends BeanParent {

    private final Logger log = Logger.getLogger(BeanWithSimpleInjected.class);

    @Resource
    private SimpleManagedBean simple;

    @Resource(lookup="java:module/SimpleManagedBean")
    private SimpleManagedBean simple2;

    @Inject
    private CDIManagedBean bean;

    private CDIManagedBean bean2;

    @Inject
    public void initMethod(CDIManagedBean bean) {
        this.bean2 = bean;
    }

    @PostConstruct
    public void start() {
        if(bean2 == null) {
            throw new RuntimeException("PostConstruct called before @Inject method");
        }

        log.info("-----> Constructed BeanWithSimpleInjected, simple=" + simple);
    }

    @Interceptors(OtherInterceptorBean.class)
    public String echo(String msg) {
        return msg + bean.getValue() + bean2.getValue();
    }

    public SimpleManagedBean getSimple() {
        return simple;
    }

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        if (!context.getMethod().getName().equals("echo")) {
            return context.proceed();
        }
        log.info("-----> Intercepting call to " + context.getMethod());
        return "#BeanWithSimpleInjected#" + context.proceed();
    }
}
