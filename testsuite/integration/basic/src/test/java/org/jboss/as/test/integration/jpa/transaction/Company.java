package org.jboss.as.test.integration.jpa.transaction;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

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
