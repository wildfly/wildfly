/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.injection.ejb;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * @author Stuart Douglas
 */
@Stateless
@EJB(name = "jndiEjb",beanName = "../b1.jar#bean", beanInterface = BeanInterface.class)
public class InjectingBean {

    @EJB(beanName = "../b1.jar#bean")
    public BeanInterface bean1;

    @EJB(beanName = "../b2.jar#bean")
    public BeanInterface bean2;

    public String getBean1Name() {
        return bean1.name();
    }

    public String getBean2Name() {
        return bean2.name();
    }

    public String getJndiEjbName() throws NamingException {
        return ((BeanInterface)new InitialContext().lookup("java:comp/env/jndiEjb")).name();
    }

}
