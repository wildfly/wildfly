/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.suspend;

import jakarta.ejb.Remote;

/**
 * @author Stuart Douglas
 */
@Remote
public interface Echo {

    String echo(String val);

}
