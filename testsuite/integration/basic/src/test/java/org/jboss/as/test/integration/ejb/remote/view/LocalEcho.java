/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.view;

import jakarta.ejb.Local;

/**
 * @author Jaikiran Pai
 */
@Local
public interface LocalEcho {

    String echo(String message);
}
