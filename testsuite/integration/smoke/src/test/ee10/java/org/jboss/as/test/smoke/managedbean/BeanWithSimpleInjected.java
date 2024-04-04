/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.smoke.managedbean;

import jakarta.annotation.ManagedBean;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptors;
import jakarta.interceptor.InvocationContext;

import org.jboss.logging.Logger;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author Thomas.Diesler@jboss.com
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

    @Inject int number;

    @Inject String value;

    @Inject Instance<String> valueInstance;

    @PostConstruct
    public void start() {
        if(bean2 == null) {
            throw new RuntimeException("PostConstruct called before @Inject method");
        }

        log.trace("-----> Constructed BeanWithSimpleInjected, simple=" + simple);
    }

    @Interceptors(OtherInterceptorBean.class)
    @CDIBinding
    public String echo(String msg) {
        return msg + bean.getValue() + bean2.getValue();
    }

    public SimpleManagedBean getSimple() {
        return simple;
    }

    public SimpleManagedBean getSimple2() {
        return simple2;
    }

    public int getNumber() {
        return number;
    }

    public String getValue() {
        return value;
    }

    public String getValue2() {
        return valueInstance.get();
    }

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        if (!context.getMethod().getName().equals("echo")) {
            return context.proceed();
        }
        log.trace("-----> Intercepting call to " + context.getMethod());
        return "#BeanWithSimpleInjected#" + context.proceed();
    }
}
