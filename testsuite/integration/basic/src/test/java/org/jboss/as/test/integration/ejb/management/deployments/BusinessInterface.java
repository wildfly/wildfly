/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.management.deployments;

import jakarta.ejb.Remote;
/**
 * Dummy interface for session beans in this class.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
@Remote
public interface BusinessInterface {

    void doIt();

    void remove();

}
