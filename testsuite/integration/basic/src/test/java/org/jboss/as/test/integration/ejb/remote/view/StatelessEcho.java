/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.view;

import jakarta.ejb.Local;
import jakarta.ejb.Stateless;

/**
 * @author Jaikiran Pai
 */
@Stateless
@Local(LocalEcho.class)
public class StatelessEcho extends CommonEcho {
}
