/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.support;

import java.util.UUID;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class Alpha {

    private String id;


    @PostConstruct
    public void init() {
        this.id = UUID.randomUUID().toString();
    }

    public String getId() {
        return this.id;
    }

}
