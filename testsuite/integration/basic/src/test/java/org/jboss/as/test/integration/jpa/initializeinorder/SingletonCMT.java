/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.initializeinorder;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Singleton;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;

/**
 * @author Scott Marlow
 */
@Singleton
@LocalBean
@TransactionManagement(TransactionManagementType.CONTAINER)
public class SingletonCMT extends AbstractCMTBean {
}
