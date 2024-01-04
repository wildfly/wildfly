/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jpa.epcpropagation.hierarchy;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceContextType;

/**
 * @author Stuart Douglas
 */
public class BeanParent {

    @PersistenceContext(type = PersistenceContextType.EXTENDED)
    protected EntityManager entityManager;

}
