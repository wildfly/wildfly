/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.view;

import jakarta.ejb.Local;
import jakarta.ejb.Stateful;

/**
 * @author Jaikiran Pai
 */
@Stateful
@Local(LocalEcho.class)
public class StatefulEcho extends CommonEcho {
}
