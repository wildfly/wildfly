/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.deployment.scopes;

import java.io.Serializable;

import jakarta.transaction.TransactionScoped;

@SuppressWarnings("serial")
@TransactionScoped
public class TransactionScopedBean implements Serializable {

}
