/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.preview.hibernate.orm.internalcache;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * A trivial entity with nothing special: we only need to boot the test
 */
@Entity
public class Employee {
    @Id
    private int id;

    private String name;
}