package org.jboss.as.test.integration.deployment.xml.datasource;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * @author Stuart Douglas
 */
@Stateless
public class JpaRemoteBean implements JpaRemote {

    private static final AtomicInteger idGenerator = new AtomicInteger(0);

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void addEmployee(final String name) {
        Employee e = new Employee();
        e.setId(idGenerator.incrementAndGet());
        e.setName(name);
        entityManager.persist(e);
    }

    @Override
    public Set<String> getEmployees() {
        final List<Employee> emps = entityManager.createQuery("from Employee").getResultList();
        final Set<String> ret = new HashSet<String>();
        for (Employee e : emps) {
            ret.add(e.getName());
        }
        return ret;
    }
}
