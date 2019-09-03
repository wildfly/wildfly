package org.jboss.as.test.integration.jpa.secondlevelcache;

import org.hibernate.Session;
import org.hibernate.stat.QueryStatistics;
import org.hibernate.stat.Statistics;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.persistence.Query;

import static org.junit.Assert.assertEquals;

@Stateless
public class SFSB4LC {

    @PersistenceUnit(unitName = "mypc")
    EntityManagerFactory emf;

    /**
     * Create employee in provided EntityManager
     */
    public void createEmployee(String name, String address, int id) {
        EntityManager em = emf.createEntityManager();
        Employee emp = new Employee();
        emp.setId(id);
        emp.setAddress(address);
        emp.setName(name);
        try {
            em.persist(emp);
            em.flush();
        } catch (Exception e) {
            throw new RuntimeException("transactional failure while persisting employee entity", e);
        }
    }

    /**
     * Performs 2 query calls, first call put entity in the cache and second should hit the cache
     *
     * @param id Employee's id in the query
     */
    public String entityCacheCheck(int id) {

        // the nextTimestamp from infinispan is "return System.currentTimeMillis()"
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return e.getMessage();
        }

        EntityManager em = emf.createEntityManager();
        Statistics stats = em.unwrap(Session.class).getSessionFactory().getStatistics();
        stats.clear();

        try {
            String queryString = "from Employee e where e.id = " + id;
            QueryStatistics queryStats = stats.getQueryStatistics(queryString);
            Query query = em.createQuery(queryString);
            query.setHint("org.hibernate.cacheable", true);

            // query - this call should fill the cache
            query.getResultList();
            assertEquals("Expected 1 miss in cache" + generateQueryCacheStats(queryStats), 1, queryStats.getCacheMissCount());
            assertEquals("Expected 1 put in cache" + generateQueryCacheStats(queryStats), 1, queryStats.getCachePutCount());
            assertEquals("Expected no hits in cache" + generateQueryCacheStats(queryStats), 0, queryStats.getCacheHitCount());

            // query - second call should hit cache
            query.getResultList();
            assertEquals("Expected 1 hit in cache" + generateQueryCacheStats(queryStats), 1, queryStats.getCacheHitCount());


        } catch (AssertionError e) {
            return e.getMessage();
        } finally {
            em.close();
        }
        return "OK";
    }

    /**
     * Update Employee's address
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void updateEmployeeAddress(int id, String address, boolean fail) {
        EntityManager em = emf.createEntityManager();
        Employee emp = em.find(Employee.class, id);
        emp.setAddress(address);
        em.merge(emp);
        em.flush();

        if (fail) {
            // Asked to fail...throwing exception for rollback
            throw new RollbackException();
        }
    }

    /**
     * Generate query cache statistics for put, hit and miss count as one String
     */
    public String generateQueryCacheStats(QueryStatistics stats) {
        String result = "(hitCount=" + stats.getCacheHitCount()
                + ", missCount=" + stats.getCacheMissCount()
                + ", putCount=" + stats.getCachePutCount() + ").";

        return result;
    }

}