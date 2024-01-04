/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.webtxem.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * WebJPTATestCase entity class
 */
@Entity
public class WebJPAEntity {

    private Long id;
    private String name;

    public WebJPAEntity() {
    }

    @Id
    @Column(name = "web_jpa_")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long getId() {
        return id;
    }

    public void setId(Long long1) {
        id = long1;
    }

    @Column(updatable = false, name = "web_jpa_name", nullable = false, length = 50)
    public String getName() {
        return name;
    }

    public void setName(String string) {
        name = string;
    }

}
