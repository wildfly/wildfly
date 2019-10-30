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
package org.jboss.as.test.integration.weld.ejb.injection;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.transaction.UserTransaction;

/**
 * AS7-1269
 *
 * Tests the @EJB injection into CDI beans works correctly
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class EjbInjectionIntoCdiBeanTestCase {

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class);
        war.addPackage(EjbInjectionIntoCdiBeanTestCase.class.getPackage());
        war.addAsWebInfResource(new StringAsset(""), "beans.xml");
        return war;
    }

    @Inject
    private BusStation bean;

    @Resource(lookup="java:jboss/UserTransaction")
    private UserTransaction userTransaction;

    @Test
    public void testEjbInjection() {
        Assert.assertNotNull(bean.getBus());
        Assert.assertNotNull(bean.getLookupBus());
        Assert.assertNotNull(bean.getLookupBus2());
    }

    @Test
    public void testUserTransactionInjection() {
        Assert.assertNotNull(userTransaction);
    }
}
