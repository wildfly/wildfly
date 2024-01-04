/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.packaging.multimodule;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;

/**
 * User: jpai
 */
@Stateless
@LocalBean
public class MrBean implements LocalBeanInterfaceInEarLib, LocalBeanInterfaceInEjbJar, RemoteBeanInterfaceInEarLib, RemoteBeanInterfaceInEjbJar {
}
