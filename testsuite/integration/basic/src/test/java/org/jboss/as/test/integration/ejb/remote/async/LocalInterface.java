/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.remote.async;

import jakarta.ejb.Local;
import java.util.concurrent.Future;

/**
 * @author Stuart Douglas
 */
@Local
public interface LocalInterface {
    void passByReference(String[] value);

    Future<Void> alwaysFail() throws AppException;

}
