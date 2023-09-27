/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.resource.resourceref;

import jakarta.ejb.Remote;
import javax.naming.NamingException;

/**
 * ResourceRefRemote
 *
 * @author Jaikiran Pai
 */
@Remote
public interface ResourceRefRemote {
    boolean isDataSourceAvailableInEnc() throws NamingException;
}
