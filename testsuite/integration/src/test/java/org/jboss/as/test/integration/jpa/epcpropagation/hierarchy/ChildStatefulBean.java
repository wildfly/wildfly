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
package org.jboss.as.test.integration.jpa.epcpropagation.hierarchy;

import org.junit.Assert;

import javax.ejb.EJB;
import javax.ejb.Stateful;
import javax.naming.InitialContext;

/**
 * @author Stuart Douglas
 */
@Stateful
public class ChildStatefulBean extends BeanParent {

    @EJB
    private SimpleStatefulBean injectedSimpleStatefulBean;

    public void testPropagation() throws Exception {
        SimpleStatefulBean simpleStatefulBean = (SimpleStatefulBean)new InitialContext().lookup("java:module/" + SimpleStatefulBean.class.getSimpleName());
        Bus b = new Bus(1, "My Bus");
        entityManager.persist(b);
        //the XPC should propagate
        simpleStatefulBean.noop();
        injectedSimpleStatefulBean.noop();
        Assert.assertTrue(simpleStatefulBean.contains(b));
        Assert.assertTrue(injectedSimpleStatefulBean.contains(b));
        Assert.assertTrue(entityManager.contains(b));
    }
}
