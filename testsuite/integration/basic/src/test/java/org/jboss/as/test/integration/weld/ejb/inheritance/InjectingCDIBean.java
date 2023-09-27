/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.ejb.inheritance;

import jakarta.inject.Inject;

/**
 * @author Stuart Douglas
 */
public class InjectingCDIBean {

    @Inject
    private AbstractBaseClass inheritingBean;

    @Inject
    private ParentInterface parentInterface;

    public String sayHello() {
        return inheritingBean.sayHello();
    }

    public String sayGoodbye() {
        return inheritingBean.sayGoodbye();
    }

    public String callInterfaceMethod() {
        return parentInterface.interfaceMethod();
    }

}
