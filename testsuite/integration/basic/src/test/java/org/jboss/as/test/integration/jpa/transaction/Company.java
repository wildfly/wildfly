package org.jboss.as.test.integration.jpa.transaction;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

/**
 * Company
 *
 * @author Scott Marlow
 */
@Entity
public class Company {

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Set<Employee> getEmployees() {
        return employees;
    }

    public void setEmployees(Set<Employee> employees) {
        this.employees = employees;
    }

    @Id
    private int id;

    @OneToMany(fetch = FetchType.LAZY)
    public Set<Employee> employees = new HashSet<Employee>();

}
