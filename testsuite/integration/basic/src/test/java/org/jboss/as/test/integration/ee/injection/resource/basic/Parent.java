/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.resource.basic;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBContext;

/**
 * User: jpai
 */
public class Parent {

    @Resource
    protected EJBContext ejbContext;

    protected CommonBean commonBean;

    @EJB
    protected void setCommonBean(CommonBean commonBean) {
        this.commonBean = commonBean;
    }
}
