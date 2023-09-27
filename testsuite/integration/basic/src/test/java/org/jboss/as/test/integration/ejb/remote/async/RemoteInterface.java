/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.remote.async;

import jakarta.ejb.Remote;
import java.util.concurrent.Future;

/**
 * @author Stuart Douglas
 */
@Remote
public interface RemoteInterface {

    void modifyArray(final String[] array);

    Future<String> hello();

}
