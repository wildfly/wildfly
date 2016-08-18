/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.test.integration.jsf.managedbean.managedproperty;

import static org.junit.Assert.assertTrue;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test that JSF managed properties are injected before @PostConstruct methods
 * are called.
 *
 * @author Farah Juma
 */
@RunWith(Arquillian.class)
public class ManagedPropertyManagedBeanTestCase {

    @Deployment
    public static Archive<?> deploy() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "jsfmanagedbean.war");
        war.addPackage(ManagedPropertyManagedBeanTestCase.class.getPackage());
        return war;
    }

    @Test
    public void testGreetingBeanInjected() {
        assertTrue(SimpleJsfManagedBean.isGreetingBeanInjected());
    }

    @Test
    public void testPostConstructCalled() {
        assertTrue(SimpleJsfManagedBean.isPostConstructCalled());
    }
}
