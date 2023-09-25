/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.resource.noncomponent;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.mail.Session;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnit;
import jakarta.transaction.UserTransaction;
import jakarta.xml.ws.WebServiceRef;

/**
 * This class is not a component, and not used as an interceptor, and as such any resources
 * it defines should not be able to be looked up in JNDI.
 *
 * @author Stuart Douglas
 */
public class NonComponentResourceInjection {

    @Resource
    private UserTransaction userTransaction;

    /**
     * This should not fail the deployment, even though it is completely bogus
     */
    @Resource
    private NonComponentResourceInjectionTestCase randomInjection;

    @PersistenceContext(unitName = "bogus")
    private EntityManager entityManager;

    @PersistenceContext
    private EntityManager entityManagerDefault;

    @PersistenceUnit(unitName = "bogus")
    private EntityManagerFactory entityManagerFactory;

    @PersistenceUnit
    private EntityManagerFactory entityManagerFactoryDefault;

    @EJB
    private NonComponentResourceInjection notReal;

    @WebServiceRef
    private NonComponentResourceInjection nonExitantWebService;

    @Resource(mappedName = "java:/Mail")
    public Session mailSessionJndiDefault;


    @Resource(mappedName = "java:jboss/mail/foo/MyMailServer1")
    public Session mailSessionJndiCustom;

    @Resource
    public Session mailSession;

}
