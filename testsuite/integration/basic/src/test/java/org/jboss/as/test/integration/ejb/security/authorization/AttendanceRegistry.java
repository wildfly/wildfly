/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.security.authorization;

import jakarta.ejb.Remote;

/**
 * @author Jaikiran Pai
 */
@Remote
public interface AttendanceRegistry<T extends TimeProvider> {

    String recordEntry(String user, T timeProvider);

    String recordEntry(String user, long time);

}
