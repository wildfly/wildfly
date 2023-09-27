/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.statistics.xa;

import jakarta.ejb.Remote;

/**
 * @author dsimko@redhat.com
 */
@Remote
public interface SLSB {
    void commit() throws Exception;

    void rollback() throws Exception;
}
