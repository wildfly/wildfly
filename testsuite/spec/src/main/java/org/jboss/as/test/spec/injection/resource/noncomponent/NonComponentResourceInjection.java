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
package org.jboss.as.test.spec.injection.resource.noncomponent;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.transaction.UserTransaction;
import javax.xml.ws.WebServiceRef;

/**
 * This class is not a component, and not used as an interceptor, and as such any resources
 * it defines should not be able to be looked up in JNDI.
 *
 * @author Stuart Douglas
 */
public class NonComponentResourceInjection {

    @Resource
    @SuppressWarnings("unused")
    private UserTransaction userTransaction;

    /**
     * This should not fail the deployment, even though it is completely bogus
     */
    @Resource
    @SuppressWarnings("unused")
    private BogusInjectionTarget randomInjection;

    @PersistenceContext(unitName = "bogus")
    @SuppressWarnings("unused")
    private EntityManager entityManager;

    @PersistenceContext
    @SuppressWarnings("unused")
    private EntityManager entityManagerDefault;

    @PersistenceUnit(unitName = "bogus")
    @SuppressWarnings("unused")
    private EntityManagerFactory entityManagerFactory;

    @PersistenceUnit
    @SuppressWarnings("unused")
    private EntityManagerFactory entityManagerFactoryDefault;

    @EJB
    @SuppressWarnings("unused")
    private NonComponentResourceInjection notReal;

    @WebServiceRef
    @SuppressWarnings("unused")
    private NonComponentResourceInjection nonExitantWebService;
}
