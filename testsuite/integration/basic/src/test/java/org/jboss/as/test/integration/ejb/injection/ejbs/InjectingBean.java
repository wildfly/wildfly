/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.test.integration.ejb.injection.ejbs;

import javax.ejb.EJB;
import javax.ejb.EJBs;
import javax.ejb.Stateless;
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
