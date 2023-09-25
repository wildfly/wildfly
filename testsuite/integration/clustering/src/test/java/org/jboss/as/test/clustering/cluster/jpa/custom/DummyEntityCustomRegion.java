/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.jpa.custom;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.io.Serializable;

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL, region = DummyEntityCustomRegion.DUMMY_ENTITY_REGION_NAME)
public class DummyEntityCustomRegion implements Serializable {

    public static final String DUMMY_ENTITY_REGION_NAME = "DUMMY_ENTITY_REGION_NAME";

    @Id
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "DummyEntityCustomRegion{" +
                "id=" + id +
                '}';
    }
}
