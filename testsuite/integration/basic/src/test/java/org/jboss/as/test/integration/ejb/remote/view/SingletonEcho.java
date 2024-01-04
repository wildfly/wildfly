/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.view;

import jakarta.ejb.Local;
import jakarta.ejb.Singleton;

/**
 * @author Jaikiran Pai
 */
@Singleton
@Local(LocalEcho.class)
public class SingletonEcho extends CommonEcho {
}
