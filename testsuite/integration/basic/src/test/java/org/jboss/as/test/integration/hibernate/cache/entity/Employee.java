package org.jboss.as.test.integration.hibernate.cache.entity;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Version;

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL, region="invalidation")
public class Employee {
    @Id
    private String name;

    @Version
    private long oca;

    private String title;

    public Employee(String name, String title) {
        this();
        setName(name);
        setTitle(title);
    }

    public String getName() {
        return name;
    }

    public long getOca() {
        return oca;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    protected Employee() {
        // this form used by Hibernate
    }

    protected void setName(String name) {
        this.name = name;
    }

    protected void setOca(long oca) {
        this.oca = oca;
    }
}
