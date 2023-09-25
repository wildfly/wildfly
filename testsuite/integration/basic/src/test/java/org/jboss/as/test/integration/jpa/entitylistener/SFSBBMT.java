/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.entitylistener;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateful;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;

/**
 * stateful session bean
 *
 * @author Scott Marlow
 */
@Stateful
@LocalBean
@TransactionManagement(TransactionManagementType.BEAN)
public class SFSBBMT extends AbstractBMTBean {
}
