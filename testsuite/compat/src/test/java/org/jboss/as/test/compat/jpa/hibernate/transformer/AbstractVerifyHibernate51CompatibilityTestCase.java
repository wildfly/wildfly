/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.compat.jpa.hibernate.transformer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.BitSet;
import java.util.Currency;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import javax.inject.Inject;

import org.hibernate.FlushMode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;

/**
 * Verify that an application compiled against Hibernate ORM 5.1.x, will run against Hibernate ORM 5.3.x on WildFly,
 * using a WildFly bytecode transformer that modifies the application bytecode at runtime to map the older Hibernate 5.1
 * calls, to 5.3.
 *
 * @author Scott Marlow
 * @author Gail Badner
 */
public abstract class AbstractVerifyHibernate51CompatibilityTestCase {

    private static final String hibernate_cfg = "<?xml version='1.0' encoding='utf-8'?>"
            + "<!DOCTYPE hibernate-configuration PUBLIC " + "\"//Hibernate/Hibernate Configuration DTD 3.0//EN\" "
            + "\"http://www.hibernate.org/dtd/hibernate-configuration-3.0.dtd\">"
            + "<hibernate-configuration><session-factory>" + "<property name=\"show_sql\">false</property>"
            + "<property name=\"current_session_context_class\">thread</property>"
// only needed for ORM 5.3.0    + "<property name=\"hibernate.allow_update_outside_transaction\">true</property>"
            + "<mapping resource=\"testmapping.hbm.xml\"/>" + "</session-factory></hibernate-configuration>";

    private static final String testmapping = "<?xml version=\"1.0\"?>" + "<!DOCTYPE hibernate-mapping PUBLIC "
            + "\"-//Hibernate/Hibernate Mapping DTD 3.0//EN\" " + "\"http://www.hibernate.org/dtd/hibernate-mapping-3.0.dtd\">"
            + "<hibernate-mapping package=\"org.jboss.as.test.integration.hibernate\">"
            + "<class name=\"org.jboss.as.test.compat.jpa.hibernate.transformer.Student\" table=\"STUDENT\">"
            + "<id name=\"studentId\" column=\"student_id\">" + "<generator class=\"assigned\"/>" + "</id>"
            + "<property name=\"firstName\" column=\"first_name\"/>" + "<property name=\"lastName\" column=\"last_name\"/>"
            + "<property name=\"address\"/>"
            + "</class>"
            + "<class name=\"org.jboss.as.test.compat.jpa.hibernate.transformer.Gene\" table=\"GENE\">"
            + "<id name=\"id\">" + "<generator class=\"assigned\"/>" + "</id>"
            + "<property name=\"state\" type=\"org.jboss.as.test.compat.jpa.hibernate.transformer.StateType\"/>"
            + "</class>"
            + "<class name=\"org.jboss.as.test.compat.jpa.hibernate.transformer.QueueOwner\" table=\"QUEUE_OWNER\">"
            + "<id name=\"id\">" + "<generator class=\"assigned\"/>" + "</id>"
            + "<bag name=\"strings\" collection-type=\"org.jboss.as.test.compat.jpa.hibernate.transformer.QueueType\">"
            + "<key column=\"ownerId\"/>"
            + "<element type=\"string\"/>"
            + "</bag>"
            + "</class>"
            + "<class name=\"org.jboss.as.test.compat.jpa.hibernate.transformer.MutualFund\" table=\"MutualFund\">"
            + "<id name=\"id\">" + "<generator class=\"assigned\"/>" + "</id>"
            + "<property name=\"holdings\" type=\"org.jboss.as.test.compat.jpa.hibernate.transformer.MonetaryAmountUserType\">"
            + "<column name=\"amount_millions\" not-null=\"true\" read=\"amount_millions * 1000000.0\" write=\"? / 1000000.0\"/>"
            + "<column name=\"currency\" not-null=\"true\"/>"
            + "</property>"
            + "</class>"
            + "<class name=\"org.jboss.as.test.compat.jpa.hibernate.transformer.Product\" table=\"Product\">"
            + "<id name=\"id\">" + "<generator class=\"assigned\"/>" + "</id>"
            + "<property name=\"bitSet\" type=\"org.jboss.as.test.compat.jpa.hibernate.transformer.BitSetType\"/>"
            + "</class>"
            + "</hibernate-mapping>";

    @Inject
    private SFSBHibernateSessionFactory sfsb;

    protected static JavaArchive getLib() {
        JavaArchive lib = ShrinkWrap.create(JavaArchive.class, "beans.jar");
        lib = ShrinkWrap.create(JavaArchive.class, "entities.jar");
        lib.addClasses(Student.class);
        lib.addClasses(Gene.class);
        lib.addClasses(State.class);
        lib.addClasses(StateType.class);
        lib.addClasses(QueueOwner.class);
        lib.addClasses(QueueType.class);
        lib.addClasses(PersistentQueue.class);
        lib.addClasses(MonetaryAmount.class);
        lib.addClasses(MutualFund.class);
        lib.addClasses(MonetaryAmountUserType.class);
        lib.addClasses(Product.class);
        lib.addClasses(BitSetType.class);
        lib.addClasses(BitSetTypeDescriptor.class);
        lib.addAsResource(new StringAsset(testmapping), "testmapping.hbm.xml");
        lib.addAsResource(new StringAsset(hibernate_cfg), "hibernate.cfg.xml");
        lib.addAsResource(new StringAsset(hibernate_cfg), "MutualFund.cfg.xml");
        return lib;
    }

    protected static WebArchive getWar() {
        final WebArchive main = ShrinkWrap.create(WebArchive.class, "main.war");

        main.addClasses(AbstractVerifyHibernate51CompatibilityTestCase.class, SFSBHibernateSessionFactory.class);
        return main;
    }

    @Test
    public void testSimpleOperation() {
        // setup Configuration and SessionFactory
        sfsb.setupConfig();
        try {
            Student s1 = sfsb.createStudent("MADHUMITA", "SADHUKHAN", "99 Purkynova REDHAT BRNO CZ", 1);
            Student st = sfsb.getStudent(s1.getStudentId());
            assertEquals("name read from hibernate session is MADHUMITA", "MADHUMITA", st.getFirstName());
        } finally {
            sfsb.cleanup();
        }
    }

    @Test
    public void testORA5_3_1_Compatibility_getFlushModeFromSessionUninitialized() {
        // setup Configuration and SessionFactory
        sfsb.setupConfig();
        try {
            // Session#getFlushMode returns FlushMode.AUTO by default
            assertEquals(
                    "can handle Hibernate ORM 5.1 call to Session.getFlushMode() without setting FlushMode",
                    FlushMode.AUTO,
                    sfsb.getFlushModeFromSessionTest(null)
            );
        } finally {
            sfsb.cleanup();
        }
    }

    @Test
    public void testORA5_3_1_Compatibility_getFlushModeFromSession() {
        // setup Configuration and SessionFactory
        sfsb.setupConfig();
        try {
            assertEquals(
                    "can handle Hibernate ORM 5.1 call to Session.getFlushMode()",
                    FlushMode.MANUAL,
                    sfsb.getFlushModeFromSessionTest(FlushMode.MANUAL)
            );
        } finally {
            sfsb.cleanup();
        }
    }

    @Test
    public void testORA5_3_1_Compatibility_getFlushModeFromSessionNever() {
        // setup Configuration and SessionFactory
        sfsb.setupConfig();
        try {
            assertEquals(
                    "can handle Hibernate ORM 5.1 call to Session.getFlushMode() using FlushMode.NEVER",
                    FlushMode.NEVER,
                    sfsb.getFlushModeFromSessionTest(FlushMode.NEVER)
            );
        } finally {
            sfsb.cleanup();
        }
    }

    @Test
    public void testORA5_3_1_Compatibility_getFlushModeFromQueryUninitialized() {
        // setup Configuration and SessionFactory
        sfsb.setupConfig();
        try {
            assertNull(sfsb.getFlushModeFromQueryTest(null));
        } finally {
            sfsb.cleanup();
        }
    }

    @Test
    public void testORA5_3_1_Compatibility_getFlushModeFromQuery() {
        // setup Configuration and SessionFactory
        sfsb.setupConfig();
        try {
            assertEquals(
                    "can handle Hibernate ORM 5.1 call to Query.getFlushMode()",
                    FlushMode.MANUAL,
                    sfsb.getFlushModeFromQueryTest(FlushMode.MANUAL)
            );
        } finally {
            sfsb.cleanup();
        }
    }

    @Test
    public void testORA5_3_1_Compatibility_getFlushModeFromQueryNever() {
        // setup Configuration and SessionFactory
        sfsb.setupConfig();
        try {
            assertEquals(
                    "can handle Hibernate ORM 5.1 call to Query.getFlushMode() using FlushMode.NEVER",
                    FlushMode.NEVER,
                    sfsb.getFlushModeFromQueryTest(FlushMode.NEVER)
            );
        } finally {
            sfsb.cleanup();
        }
    }

    @Test
    public void testORA5_3_1_Compatibility_getFirstResultUninitialized() {
        // setup Configuration and SessionFactory
        sfsb.setupConfig();
        try {
            // In 5.1, o.h.Query#getFirstResult returns null if no value was set via o.h.Query#setFirstResult
            assertNull(sfsb.getFirstResultTest(null));

        } finally {
            sfsb.cleanup();
        }
    }

    @Test
    public void testORA5_3_1_Compatibility_getFirstResultZero() {
        // setup Configuration and SessionFactory
        sfsb.setupConfig();
        try {
            // In 5.1, o.h.Query#getFirstResult returned 0 if set via o.h.Query#setFirstResult(0)
            assertTrue(
                    "Hibernate ORM 5.1 call to Query.getFirstResult() returned Integer " + sfsb.getFirstResultTest(0),
                    sfsb.getFirstResultTest(0) instanceof Integer
            );
            assertEquals(Integer.valueOf(0), sfsb.getFirstResultTest(0));
        } finally {
            sfsb.cleanup();
        }
    }

    @Test
    public void testORA5_3_1_Compatibility_getFirstResultNegative() {
        // setup Configuration and SessionFactory
        sfsb.setupConfig();
        try {
            // In 5.1, o.h.Query#getFirstResult returned negative number if set via o.h.Query.setFirstResult( negativeNumber )
            assertTrue(
                    "Hibernate ORM 5.1 call to Query.getFirstResult() returned Integer " + sfsb.getFirstResultTest(-1),
                    sfsb.getFirstResultTest(-1) instanceof Integer
            );
            // check for value of 0 being returned, to match what query.getHibernateFirstResult() is expected to
            // return when -1 is passed into query.setHibernateFirstResult()
            assertEquals(Integer.valueOf(0), sfsb.getFirstResultTest(-1));
        } finally {
            sfsb.cleanup();
        }
    }

    @Test
    public void testORA5_3_1_Compatibility_getFirstResultPositive() {
        // setup Configuration and SessionFactory
        sfsb.setupConfig();
        try {
            assertTrue(
                    "Hibernate ORM 5.1 call to Query.getFirstResult() returned Integer " + sfsb.getFirstResultTest(1),
                    sfsb.getFirstResultTest(1) instanceof Integer
            );
            assertEquals(Integer.valueOf(1), sfsb.getFirstResultTest(1));

        } finally {
            sfsb.cleanup();
        }
    }

    @Test
    public void testORA5_3_1_Compatibility_getMaxResultsUninitialized() {
        // setup Configuration and SessionFactory
        sfsb.setupConfig();
        try {
            // In 5.1, o.h.Query#getMaxResults returns null if no value was set via o.h.Query#setMaxResults
            assertNull(sfsb.getMaxResultsTest(null));

        } finally {
            sfsb.cleanup();
        }
    }

    @Test
    public void testORA5_3_1_Compatibility_getMaxResultsZero() {
        // setup Configuration and SessionFactory
        sfsb.setupConfig();
        try {
            // In 5.1, o.h.Query#getMaxResults returns null if 0 was set via o.h.Query.setMaxResults(0)
            assertNull(sfsb.getMaxResultsTest(0));
        } finally {
            sfsb.cleanup();
        }
    }

    @Test
    public void testORA5_3_1_Compatibility_getMaxResultsNegative() {
        // setup Configuration and SessionFactory
        sfsb.setupConfig();
        try {
            // In 5.1, o.h.Query#getMaxResults returns null if set via o.h.Query.setMaxResults( negativeNumber )
            assertNull(sfsb.getMaxResultsTest(-1));
        } finally {
            sfsb.cleanup();
        }
    }

    @Test
    public void testORA5_3_1_Compatibility_getMaxResultsMaxValue() {
        // setup Configuration and SessionFactory
        sfsb.setupConfig();
        try {
            // In 5.1, o.h.Query#getMaxResults returns Integer.MAX_VALUE if set via o.h.Query.setMaxResults( Integer.MAX_VALUE )
            assertTrue(
                    "Hibernate ORM 5.1 call to Query.getMaxResult() returned Integer " + sfsb.getMaxResultsTest(Integer.MAX_VALUE),
                    sfsb.getMaxResultsTest(Integer.MAX_VALUE) instanceof Integer
            );
            assertEquals(Integer.valueOf(Integer.MAX_VALUE), sfsb.getMaxResultsTest(Integer.MAX_VALUE));
        } finally {
            sfsb.cleanup();
        }
    }

    @Test
    public void testORA5_3_1_Compatibility_getMaxResultsPositive() {
        // setup Configuration and SessionFactory
        sfsb.setupConfig();
        try {
            assertTrue(
                    "Hibernate ORM 5.1 call to Query.getMaxResult() returned Integer " + sfsb.getMaxResultsTest(1),
                    sfsb.getMaxResultsTest(1) instanceof Integer
            );
            assertEquals(Integer.valueOf(1), sfsb.getMaxResultsTest(1));
        } finally {
            sfsb.cleanup();
        }
    }

    @Test
    public void testQueryResults() {
        // setup Configuration and SessionFactory
        sfsb.setupConfig();
        try {
            for (int i = 0; i < 5; i++) {
                sfsb.createStudent(null, null, null, i);
            }
            final String query = "from Student order by id";
            checkResults(sfsb.executeQuery(query, null, null), 0, 4);
            checkResults(sfsb.executeQuery(query, 0, null), 0, 4);
            checkResults(sfsb.executeQuery(query, -1, null), 0, 4);
            checkResults(sfsb.executeQuery(query, null, 0), 0, 4);
            checkResults(sfsb.executeQuery(query, null, -1), 0, 4);
            checkResults(sfsb.executeQuery(query, null, 2), 0, 1);
            checkResults(sfsb.executeQuery(query, -1, 0), 0, 4);
            checkResults(sfsb.executeQuery(query, -1, 3), 0, 2);
            checkResults(sfsb.executeQuery(query, 1, null), 1, 4);
            checkResults(sfsb.executeQuery(query, 1, 0), 1, 4);
            checkResults(sfsb.executeQuery(query, 1, -1), 1, 4);
            checkResults(sfsb.executeQuery(query, 1, 1), 1, 1);
        } finally {
            sfsb.cleanup();
        }
    }

    private void checkResults(List results, int firstIdExpected, int lastIdExpected) {
        int resultIndex = 0;
        for (int i = firstIdExpected; i <= lastIdExpected; i++, resultIndex++) {
            assertEquals(i, ((Student) results.get(resultIndex)).getStudentId());
        }
    }


    @Test
    public void testUserTypeImplemented() {
        // setup Configuration and SessionFactory
        sfsb.setupConfig();
        try {
            sfsb.createGene(1, State.DORMANT);
            final Gene gene = sfsb.getGene(1);
            assertEquals(State.DORMANT, gene.getState());
        } finally {
            sfsb.cleanup();
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCollectionUserTypeImplementedPersistentBagExtended() {
        // setup Configuration and SessionFactory
        sfsb.setupConfig();
        try {
            Queue queue = new LinkedList<String>();
            queue.add("first");
            queue.add("second");
            sfsb.createQueueOwner(1, queue);
            final QueueOwner queueOwner = sfsb.getQueueOwner(1);
            assertEquals(2, queueOwner.getStrings().size());
            final Iterator<String> it = queueOwner.getStrings().iterator();
            assertEquals("first", it.next());
            assertEquals("second", it.next());

        } finally {
            sfsb.cleanup();
        }
    }

    @Test
    public void testCompositeUserTypeImplemented() {
        // setup Configuration and SessionFactory
        sfsb.setupConfig();
        try {
            final MonetaryAmount holdings = new MonetaryAmount(new BigDecimal("73000000.000"), Currency.getInstance("USD"));
            sfsb.createMutualFund(1L, holdings);
            final MutualFund mutualFund = sfsb.getMutualFund(1L);
            assertEquals(holdings, mutualFund.getHoldings());
        } finally {
            sfsb.cleanup();
        }
    }

    @Test
    public void testAbstractSingleColumnStandardBasicTypeReplaceExtended() {
        // setup Configuration and SessionFactory
        sfsb.setupConfig();
        try {
            BitSet bitset = BitSet.valueOf(new long[]{1, 2, 3});
            sfsb.createAndMergeProduct(1, bitset);
            final Product product = sfsb.getProduct(1);
            assertEquals(bitset, product.getBitSet());
        } finally {
            sfsb.cleanup();
        }

    }
}
