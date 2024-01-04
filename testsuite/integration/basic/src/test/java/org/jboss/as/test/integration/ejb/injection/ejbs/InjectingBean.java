/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.injection.ejbs;

import jakarta.ejb.EJB;
import jakarta.ejb.EJBs;
import jakarta.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * @author Stuart Douglas
 */
@Stateless
@EJBs({@EJB(name = "bean1", beanName = "../b1.jar#bean", beanInterface = BeanInterface.class), @EJB(name = "bean2", beanName = "../b2.jar#bean", beanInterface = BeanInterface.class)})
public class InjectingBean {


    public String getBean1Name() {
        try {
            return ((BeanInterface) new InitialContext().lookup("java:comp/env/bean1")).name();
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    public String getBean2Name() {
        try {
            return ((BeanInterface) new InitialContext().lookup("java:comp/env/bean2")).name();
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

}
