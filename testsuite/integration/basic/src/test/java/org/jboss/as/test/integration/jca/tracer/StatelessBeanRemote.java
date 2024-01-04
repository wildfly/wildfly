/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.tracer;

import jakarta.ejb.Remote;

/**
 * @author Ondra Chaloupka <ochaloup@redhat.com>
 */
@Remote
public interface StatelessBeanRemote {
    void insertToDB();
    void createTable();
}
