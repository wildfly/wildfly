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

package org.jboss.as.test.integration.jpa.secondlevelcache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.persistence.Query;

import org.hibernate.Session;
import org.hibernate.stat.QueryStatistics;
import org.hibernate.stat.SecondLevelCacheStatistics;
import org.hibernate.stat.Statistics;

/**
 * SFSB for Second level cache tests
 * 
 * @author Zbynek Roubalik
 */
@Stateful
@TransactionManagement(TransactionManagementType.CONTAINER)
public class SFSB2LC {
	@PersistenceUnit(unitName = "mypc")
	EntityManagerFactory emf;

	@PersistenceUnit(unitName = "mypc_no_2lc")
	EntityManagerFactory emfNo2LC;
	
	
	/**
	 * Check if disabling 2LC works as expected
	 */
	public String disabled2LCCheck() {
		
		EntityManager em = emfNo2LC.createEntityManager();
		Statistics stats = em.unwrap(Session.class).getSessionFactory().getStatistics();
		stats.clear();
		
		try {
			// check if entities are NOT cached in 2LC
			String names[] = stats.getSecondLevelCacheRegionNames();
			assertEquals("There aren't any 2LC regions.", 0, names.length);

			createEmployee(em, "Martin", "Prague 132", 1);
			assertEquals("There aren't any puts in the 2LC.", 0,stats.getSecondLevelCachePutCount());

			// check if queries are NOT cached in 2LC
			Employee emp = getEmployeeQuery(em, 1);
			assertNotNull("Employee returned", emp);
			assertEquals("There aren't any query puts in the 2LC.", 0,stats.getQueryCachePutCount());
			
		}catch (AssertionError e) {
			return e.getMessage();
		}	finally{
			em.close();
		}
		return "OK";
	}
	
	
	/**
	 *  Checking entity 2LC in one EntityManager session
	 */
	public String sameSessionCheck(String CACHE_REGION_NAME) {
		
		EntityManager em = emf.createEntityManager();
		Statistics stats = em.unwrap(Session.class).getSessionFactory().getStatistics();
		stats.clear();
		SecondLevelCacheStatistics emp2LCStats = stats.getSecondLevelCacheStatistics(CACHE_REGION_NAME+"Employee");
			
		try{
			// add new entities and check if they are put in 2LC
			createEmployee(em, "Peter", "Ostrava", 2);
			createEmployee(em, "Tom", "Brno", 3);
			assertEquals("There are 2 puts in the 2LC"+generateEntityCacheStats(emp2LCStats), 2, emp2LCStats.getPutCount());
			
			// loading all Employee entities should put in 2LC one Employee entity from previous test method
			List<?> empList = getAllEmployeesQuery(em);
			assertEquals("There are 3 entities.", empList.size(), 3);
			assertEquals("There are 3 entities in the 2LC"+generateEntityCacheStats(emp2LCStats), 3, emp2LCStats.getElementCountInMemory());
			
			// clear session
			em.clear();
			
			// entity should be loaded from 2L cache, we'are expecting hit in 2L cache
			Employee emp = getEmployee(em, 2);
			assertNotNull("Employee returned", emp);
			assertEquals("Expected 1 hit in cache"+generateEntityCacheStats(emp2LCStats), 1,  emp2LCStats.getHitCount());
			
		}catch (AssertionError e) {
			return e.getMessage();
		}	finally{
			em.close();
		}
		return "OK";
	}
	
	
	/**
	 *  Checking entity stored in a cache in a different EntityManager session
	 */
	public String secondSessionCheck(String CACHE_REGION_NAME) {
		
		EntityManager em = emf.createEntityManager();
		Statistics stats = em.unwrap(Session.class).getSessionFactory().getStatistics();
		stats.clear();
		SecondLevelCacheStatistics emp2LCStats = stats.getSecondLevelCacheStatistics(CACHE_REGION_NAME+"Employee");
		
		try{	
			// loading entity stored in previous session, we'are expecting hit in cache
			Employee emp = getEmployee(em, 2);
			assertNotNull("Employee returned", emp);
			assertEquals("Expected 1 hit in cache"+generateEntityCacheStats(emp2LCStats), 1,  emp2LCStats.getHitCount());
			
		}catch (AssertionError e) {
			return e.getMessage();
		}	finally{
			em.close();
		}
		return "OK";
	}
	
	
	/**
	 * Check if eviction of entity cache is working
	 */
	public String evict2LCCheck(String CACHE_REGION_NAME){

		EntityManager em = emf.createEntityManager();
		Statistics stats = em.unwrap(Session.class).getSessionFactory().getStatistics();
		stats.clear();
		SecondLevelCacheStatistics emp2LCStats = stats.getSecondLevelCacheStatistics(CACHE_REGION_NAME+"Employee");
		
		try{	
			assertTrue("Expected entities stored in the cache"+generateEntityCacheStats(emp2LCStats), emp2LCStats.getElementCountInMemory() > 0);
			
			// evict cache and check if is empty
			emf.getCache().evictAll();
			assertEquals("Expected no entities stored in the cache"+generateEntityCacheStats(emp2LCStats), 0, emp2LCStats.getElementCountInMemory());
			
			
		}catch (AssertionError e) {
			return e.getMessage();
		}	finally{
			em.close();
		}
		return "OK";
		
	}
	
	
	/**
	 *  Check if query cache works as expected, running same query twice - second should hit the cache 
	 */
	public String sameQueryTwice(){
		
		EntityManager em = emf.createEntityManager();
		Statistics stats = em.unwrap(Session.class).getSessionFactory().getStatistics();
		stats.clear();
		
		try{
			int id = 2;
			String queryString = "from Employee e where e.id="+id;
			
			Query query = em.createQuery(queryString);
			query.setHint("org.hibernate.cacheable", true);
			QueryStatistics queryStats = stats.getQueryStatistics(queryString);
						
			// first query call
			query.getSingleResult();
			assertEquals("Expected 1 put in cache"+generateQueryCacheStats(queryStats), 1,  queryStats.getCachePutCount());
			
			// second query call
			query.getSingleResult();
			assertEquals("Expected 1 hit in cache"+generateQueryCacheStats(queryStats), 1,  queryStats.getCacheHitCount());

		}catch (AssertionError e) {
			return e.getMessage();
		}	finally{
			em.close();
		}
		return "OK";
	}
	
	
	/**
	 *  Query cache is invalidated and restored then
	 */
	public String invalidateQuery(){
		
		EntityManager em = emf.createEntityManager();
		Statistics stats = em.unwrap(Session.class).getSessionFactory().getStatistics();
		stats.clear();

		try{
			String queryString = "from Employee e where e.id > 1";
			QueryStatistics queryStats = stats.getQueryStatistics(queryString);
			Query query = em.createQuery(queryString);
			query.setHint("org.hibernate.cacheable", true);

			// query - this call should fill the cache
			query.getResultList();
			assertEquals("Expected 1 put in cache"+generateQueryCacheStats(queryStats), 1,  queryStats.getCachePutCount());
			assertEquals("Expected no hits in cache"+generateQueryCacheStats(queryStats), 0,  queryStats.getCacheHitCount());
			
			// query - should hit cache
			query.getResultList();
			assertEquals("Expected 1 hit in cache"+generateQueryCacheStats(queryStats), 1,  queryStats.getCacheHitCount());
			
			// invalidate cache
			createEmployee(em, "Newman", "Paul", 4);
			
			// first call should miss and the second should hit the cache
			query.getResultList();
			assertEquals("Expected 2x miss in cache"+generateQueryCacheStats(queryStats), 2,  queryStats.getCacheMissCount());

			query.getResultList();
			assertEquals("Expected 2 hits in cache"+generateQueryCacheStats(queryStats), 2,  queryStats.getCacheHitCount());

		}catch (AssertionError e) {
			return e.getMessage();
		}	finally{
			em.close();
		}
		return "OK";
	}

	
	/**
	 * Check if eviction of query cache is working
	 */
	public String evictQueryCacheCheck(){
		
		EntityManager em = emf.createEntityManager();
		Statistics stats = em.unwrap(Session.class).getSessionFactory().getStatistics();
		stats.clear();

		try{
			String queryString = "from Employee e where e.id > 2";
			QueryStatistics queryStats = stats.getQueryStatistics(queryString);
			Query query = em.createQuery(queryString);
			query.setHint("org.hibernate.cacheable", true);

			// query - this call should fill the cache
			query.getResultList();
			assertEquals("Expected 1 put in cache"+generateQueryCacheStats(queryStats), 1,  queryStats.getCachePutCount());
			assertEquals("Expected no hits in cache"+generateQueryCacheStats(queryStats), 0,  queryStats.getCacheHitCount());
			
			// query - should hit cache
			query.getResultList();
			assertEquals("Expected 1 hit in cache"+generateQueryCacheStats(queryStats), 1,  queryStats.getCacheHitCount());
			
			// this should evict query cache -> expected second put in the cache
			em.unwrap(Session.class).getSessionFactory().getCache().evictQueryRegions();
			query.getResultList();
			assertEquals("Expected 2 puts in cache"+generateQueryCacheStats(queryStats), 2,  queryStats.getCachePutCount());

		}catch (AssertionError e) {
			return e.getMessage();
		}	finally{
			em.close();
		}
		return "OK";
	}
	
	
	/**
	 * Generate query cache statistics for put, hit and miss count as one String
	 */
	public String generateQueryCacheStats(QueryStatistics stats){
		String result = "(hitCount="+stats.getCacheHitCount()
				+", missCount="+stats.getCacheMissCount()
				+", putCount="+stats.getCachePutCount()+").";
		
		return result;
	}
	
	
	/**
	 * Generate entity cache statistics for put, hit and miss count as one String
	 */
	public String generateEntityCacheStats(SecondLevelCacheStatistics stats){
		String result = "(hitCount="+stats.getHitCount()
				+", missCount="+stats.getMissCount()
				+", putCount="+stats.getPutCount()+").";
		
		return result;
	}
	
	
	/**
	 * Create employee in provided EntityManager
	 */
	public void createEmployee(EntityManager em, String name, String address,
			int id) {
		Employee emp = new Employee();
		emp.setId(id);
		emp.setAddress(address);
		emp.setName(name);
		try {
			em.persist(emp);
			em.flush();
		} catch (Exception e) {
			throw new RuntimeException(	"transactional failure while persisting employee entity", e);
		}
	}

	
	/**
	 * Load employee from provided EntityManager
	 */
	public Employee getEmployee(EntityManager em, int id) {
		Employee emp = em.find(Employee.class, id);
		return emp;
	}

	
	/**
	 * Load employee using Query from provided EntityManager
	 */
	public Employee getEmployeeQuery(EntityManager em, int id) {

		Query query;

		query = em.createQuery("from Employee e where e.id=:id");

		query.setParameter("id", id);
		query.setHint("org.hibernate.cacheable", true);

		return (Employee) query.getSingleResult();
	}

	
	/**
	 * Load all employees using Query from provided EntityManager
	 */
	@SuppressWarnings("unchecked")
	public List<Employee> getAllEmployeesQuery(EntityManager em) {

		Query query;

		query = em.createQuery("from Employee");
		query.setHint("org.hibernate.cacheable", true);

		return query.getResultList();
	}

}
