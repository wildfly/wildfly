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

package org.jboss.as.deployment.managedbean;

import javax.annotation.ManagedBean;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.annotation.Resources;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptors;
import javax.interceptor.InvocationContext;

import java.io.Serializable;

/**
 * @author John E. Bailey
 */
@ManagedBean("TestBeanWithInjection")
@Resources({
    @Resource(name="bar", type=TestManagedBean.class, mappedName="TestBean")
})
@Resource(name="foo", type=TestManagedBean.class, mappedName="TestBean")
@Interceptors({TestInterceptor.class})
public class TestManagedBeanWithInjection implements Serializable {
    private static final long serialVersionUID = -4560040427085159721L;
    @Resource private TestManagedBean other;

    static boolean invoked;
    @PostConstruct
    private void printMessage() {
        System.out.println("Test manage bean started: " + other);
    }

    public TestManagedBean getOther() {
        return other;
    }
    

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        invoked = true;
        return context.proceed();
    }
    
}
