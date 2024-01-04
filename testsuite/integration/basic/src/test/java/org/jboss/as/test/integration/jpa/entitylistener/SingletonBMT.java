/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.entitylistener;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Singleton;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;

/**
 * @author Jaikiran Pai
 */
@Singleton
@LocalBean
@TransactionManagement(TransactionManagementType.BEAN)
public class SingletonBMT extends AbstractBMTBean {
}
