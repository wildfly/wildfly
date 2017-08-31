/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.jboss.as.test.compat.jpa.eclipselink.wildfly8954;

import static org.junit.Assume.assumeTrue;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.compat.jpa.eclipselink.EclipseLinkSharedModuleProviderTestCase;
import org.jboss.as.test.compat.jpa.eclipselink.Employee;
import org.jboss.as.test.compat.jpa.eclipselink.SFSB1;
import org.jboss.as.test.compat.util.BasicJndiUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * This base class is based on the {@link EclipseLinkSharedModuleProviderTestCase}
 *
 */
@RunWith(Arquillian.class)
public class WFLY8954BaseTest {

    private static final String ARCHIVE_NAME = "toplink_module_test";

    @ArquillianResource
    private static InitialContext iniCtx;

    @BeforeClass
    public static void beforeClass() throws NamingException {
        iniCtx = new InitialContext();
    }

    @Deployment
    public static Archive<?> deploy() throws Exception {

        // (a) Create an .ear file
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, ARCHIVE_NAME + ".ear");

        JavaArchive lib = ShrinkWrap.create(JavaArchive.class, "beans.jar");
        // NOTE: PersistenceXmlHelper - should not beed be deployed - but it is a test class dependency for test
        // preparation
        lib.addClass(PersistenceXmlHelper.class);
        lib.addClasses(BasicJndiUtil.class);

        // Add ejbs:
        // 1. An ejb that allows us to create employees
        lib.addClasses(SFSB1.class);
        // 2. An ejb that will modify and fire an event
        lib.addClass(EjbThatModifiesEntityAndFiresEventLocalFacade.class);
        // 3. Add the event to be fired
        lib.addClass(SomeEntityChangeEvent.class);
        // 4. Add the observer of the event that will get a stale entity
        lib.addClass(SomeEntityChangeEventObserverFacade.class);
        // Pump the jar file into the EAR/lib
        ear.addAsModule(lib);

        lib = ShrinkWrap.create(JavaArchive.class, "entities.jar");
        lib.addClasses(Employee.class);

        // lib.addAsManifestResource(new StringAsset(persistence_xml), "persistence.xml");
        lib.addAsManifestResource(new StringAsset(PersistenceXmlHelper.SINGLETON.getWFLY8954BaseTestPersistenceXml()),
                "persistence.xml");
        ear.addAsLibraries(lib);

        final WebArchive main = ShrinkWrap.create(WebArchive.class, "main.war");
        main.addClasses(WFLY8954BaseTest.class);
        ear.addAsModule(main);

        return ear;
    }

    @Test
    public void testSimpleCreateAndLoadEntities() throws Exception {
        assumeTrue(System.getSecurityManager() == null); // ignore test if System.getSecurityManager() returns non-null

        // (a) Start by creating an employeed entity
        SFSB1 sfsb1 = jndiLookupSFB1();
        final Integer employeePrimaryKey = 10;
        sfsb1.createEmployee("Kelly Smith", "Initial Address Value. ", employeePrimaryKey);

        // (b) Execute the bug step
        // This consists on modifying an entity, making it be observed, and verify if the enityt on th observes reflects
        // the
        // committed state
        EjbThatModifiesEntityAndFiresEventLocalFacade ejbThatWillModifyAndFireEvent = jndiLookupEjbThatModifiesEntityAndFiresEventLocalFacade();
        ejbThatWillModifyAndFireEvent.modifyEmployeedAddressAndFireAChangeEvent(employeePrimaryKey);

        // (c) Now we check the results of the test
        // either the observer get a stale entity or an entity that reflects the db changes
        SomeEntityChangeEventObserverFacade singletonObserver = jndiLookupSomeEntityChangeEventObserverFacade();
        String lastProcessedEntityAddressBeforeExecutingRefresh = singletonObserver
                .getLastProcessedEntityAddressBeforeExecutingRefresh();
        String lastProcessedEntityAddressAfterExecutingRefresh = singletonObserver
                .getLastProcessedEntityAddressAfterExecutingRefresh();
        SomeEntityChangeEvent changeEvent = singletonObserver.getLastProcessedSomeEntityChangeEvent();
        boolean isBugDetected = singletonObserver.isBugDetected();
        if (isBugDetected) {
            String arrsetionError = String.format(
                    "The observer bug has been detected.%n" + "The context is: %n " + "EVENT OBSERVED: %1$s %n"
                            + "lastProcessedEntityAddressBeforeExecutingRefresh: %2$s %n"
                            + "lastProcessedEntityAddressAfterExecutingRefresh: %3$s %n"
                            + " If the bug was not present, we would expect that refreshing the entity would have no effect. %n"
                            + " When the bug is present, refreshing the entity has an effect because the unit of work cache has not yet been merged to the session cache"
                            + " but the changes have already been committed to the database. ",
                    changeEvent, lastProcessedEntityAddressBeforeExecutingRefresh,
                    lastProcessedEntityAddressAfterExecutingRefresh);
            Assert.fail(arrsetionError);
        }
    }

    /**
     *
     * @return A stateless ejb that can help us create entities
     */
    protected SFSB1 jndiLookupSFB1() {
        return BasicJndiUtil.SINGLETON.lookupEjbWithinEar(iniCtx, ARCHIVE_NAME, SFSB1.class.getSimpleName(),
                SFSB1.class);
    }

    /**
     *
     * @return A stateless ejb that can help us reproduce the issue of we are trying to validate
     */
    protected EjbThatModifiesEntityAndFiresEventLocalFacade jndiLookupEjbThatModifiesEntityAndFiresEventLocalFacade() {
        return BasicJndiUtil.SINGLETON.lookupEjbWithinEar(iniCtx, ARCHIVE_NAME,
                EjbThatModifiesEntityAndFiresEventLocalFacade.class.getSimpleName(),
                EjbThatModifiesEntityAndFiresEventLocalFacade.class);
    }

    protected SomeEntityChangeEventObserverFacade jndiLookupSomeEntityChangeEventObserverFacade() {
        return BasicJndiUtil.SINGLETON.lookupEjbWithinEar(iniCtx, ARCHIVE_NAME,
                SomeEntityChangeEventObserverFacade.class.getSimpleName(), SomeEntityChangeEventObserverFacade.class);
    }

}
