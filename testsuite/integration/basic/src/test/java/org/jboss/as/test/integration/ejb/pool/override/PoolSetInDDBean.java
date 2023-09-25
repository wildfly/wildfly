/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.pool.override;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;

/**
 * @author Jaikiran Pai
 */
@Stateless
@LocalBean
public class PoolSetInDDBean extends AbstractSlowBean {

    public static final String POOL_NAME_IN_DD = "B";
}
