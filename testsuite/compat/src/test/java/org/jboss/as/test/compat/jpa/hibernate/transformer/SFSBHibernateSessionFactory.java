/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import java.util.BitSet;
import java.util.List;
import java.util.Properties;
import java.util.Queue;

import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;

import org.hibernate.BasicQueryContract;
import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.internal.util.config.ConfigurationHelper;

/**
 * Test that a Hibernate sessionfactory can be inititated from hibernate.cfg.xml and properties added to Hibernate Configuration
 * in AS7 container without any JPA assistance
 *
 * @author Madhumita Sadhukhan
 */
@Stateful
@TransactionManagement(TransactionManagementType.BEAN)
public class SFSBHibernateSessionFactory {

    private static SessionFactory sessionFactory;

    public void cleanup() {
        sessionFactory.close();
    }

    public void setupConfig() {
        // static {
        try {

            // prepare the configuration
            Configuration configuration = new Configuration().setProperty(AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS,
                    "true");
            configuration.setProperty(Environment.HBM2DDL_AUTO, "create-drop");
            configuration.setProperty(Environment.DATASOURCE, "java:jboss/datasources/ExampleDS");
            configuration.setProperty("hibernate.listeners.envers.autoRegister", "false");

            // fetch the properties
            Properties properties = new Properties();
            configuration = configuration.configure("hibernate.cfg.xml");
            properties.putAll(configuration.getProperties());

            Environment.verifyProperties( properties );
            ConfigurationHelper.resolvePlaceHolders( properties );

            sessionFactory = configuration.buildSessionFactory();
        } catch (Throwable ex) { // Make sure you log the exception, as it might be swallowed
            throw new RuntimeException("Could not setup config", ex);
        }

    }

    // create student
    public Student createStudent(String firstName, String lastName, String address, int id) {

        Student student = new Student();
        student.setStudentId(id);
        student.setAddress(address);
        student.setFirstName(firstName);
        student.setLastName(lastName);

        try {
            Session session = sessionFactory.openSession();
            Transaction tx = session.beginTransaction();
            session.save( student );
            session.flush();
            tx.commit();
            session.close();
        } catch (Exception e) {
            throw new RuntimeException("transactional failure while persisting student entity", e);
        }

        return student;
    }

    // fetch student
    public Student getStudent(int id) {
        Student emp = sessionFactory.openSession().load(Student.class, id);
        return emp;
    }

    public FlushMode getFlushModeFromQueryTest(FlushMode flushMode) {
        FlushMode result;
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        try {
            BasicQueryContract basicQueryContract = session.createQuery("from Student");
            if ( flushMode != null ) {
                basicQueryContract.setFlushMode(flushMode);
            }
            result = basicQueryContract.getFlushMode();
            return result;
        } finally {
            transaction.rollback();
            session.close();
        }
    }

    public FlushMode getFlushModeFromSessionTest(FlushMode flushMode) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        try {
            if ( flushMode != null ) {
                session.setFlushMode(flushMode);
            }
            return session.getFlushMode();
        } finally {
            transaction.rollback();
            session.close();
        }
     }

    public Integer getFirstResultTest(Integer firstValue) {

        Session session = sessionFactory.openSession();

        try {
            Query query = session.createQuery("from Student");
            if ( firstValue != null ) {
                query.setFirstResult( firstValue );
            }
            return query.getFirstResult();
        } finally {
            session.close();
        }
    }

    public Integer getMaxResultsTest(Integer maxResults) {

        Session session = sessionFactory.openSession();
        try {
            Query query = session.createQuery( "from Student" );
            if ( maxResults != null ) {
                query.setMaxResults( maxResults );
            }
            return query.getMaxResults();
        } finally {
            session.close();
        }
    }

    public List executeQuery(String queryString, Integer firstResult, Integer maxResults) {
        Session session = sessionFactory.openSession();
        try {
            Query query = session.createQuery( queryString );
            if ( firstResult != null ) {
                query.setFirstResult( firstResult );
            }
            if ( maxResults != null ) {
                query.setMaxResults(maxResults);
            }
            return query.list();
        } finally {
            session.close();
        }

    }

    public Gene createGene(int id, State state) {
        final Gene gene = new Gene();
        gene.setId( id );
        gene.setState( state );
        try {
            Session session = sessionFactory.openSession();
            Transaction tx = session.beginTransaction();
            session.save( gene );
            session.flush();
            tx.commit();
            session.close();
        } catch (Exception e) {
            throw new RuntimeException("transactional failure while persisting gene entity", e);
        }

        return gene;
    }

    public Gene getGene(int id) {
        try {
            Session session = sessionFactory.openSession();
            Transaction tx = session.beginTransaction();
            Gene gene = session.get( Gene.class, id );
            tx.commit();
            session.close();
            return gene;
        } catch (Exception e) {
            throw new RuntimeException("transactional failure while getting gene entity", e);
        }
   }

    public QueueOwner createQueueOwner(Integer id, Queue strings) {
        final QueueOwner queueOwner = new QueueOwner();
        queueOwner.setId( id );
        queueOwner.setStrings(strings );
        try {
            Session session = sessionFactory.openSession();
            Transaction tx = session.beginTransaction();
            session.save( queueOwner );
            session.flush();
            tx.commit();
            session.close();
        } catch (Exception e) {
            throw new RuntimeException("transactional failure while persisting QueueOwner entity", e);
        }

        return queueOwner;
    }

    public QueueOwner getQueueOwner(int id) {
        try {
            Session session = sessionFactory.openSession();
            Transaction tx = session.beginTransaction();
            QueueOwner queueOwner = session.get( QueueOwner.class, id );
            // initialize the bag while the session is still open
            queueOwner.getStrings().size();
            tx.commit();
            session.close();
            return queueOwner;
        } catch (Exception e) {
            throw new RuntimeException("transactional failure while getting gene entity", e);
        }
    }

    public MutualFund createMutualFund(Long id, MonetaryAmount monetaryAmount) {
        final MutualFund mutualFund = new MutualFund();
        mutualFund.setId( id );
        mutualFund.setHoldings( monetaryAmount );
        try {
            Session session = sessionFactory.openSession();
            Transaction tx = session.beginTransaction();
            session.save( mutualFund );
            session.flush();
            tx.commit();
            session.close();
        } catch (Exception e) {
            throw new RuntimeException("transactional failure while persisting MutualFund entity", e);
        }

        return mutualFund;
    }

    public MutualFund getMutualFund(Long id) {
        try {
            Session session = sessionFactory.openSession();
            Transaction tx = session.beginTransaction();
            MutualFund mutualFund = session.get( MutualFund.class, id );
            tx.commit();
            session.close();
            return mutualFund;
        } catch (Exception e) {
            throw new RuntimeException("transactional failure while getting MutualFund entity", e);
        }
    }

    public Product createAndMergeProduct(int id, BitSet bitset) {
        final Product product = new Product();
        product.setId( id );
        product.setBitSet( bitset );
        try {
            Session session = sessionFactory.openSession();
            Transaction tx = session.beginTransaction();
            session.merge( product );
            session.flush();
            tx.commit();
            session.close();
        } catch (Exception e) {
            throw new RuntimeException("transactional failure while merging Product entity", e);
        }

        return product;
    }

    public Product getProduct(int id) {
        try {
            Session session = sessionFactory.openSession();
            Transaction tx = session.beginTransaction();
            Product product = session.get( Product.class, id );
            tx.commit();
            session.close();
            return product;
        } catch (Exception e) {
            throw new RuntimeException("transactional failure while getting Product entity", e);
        }
    }
}
