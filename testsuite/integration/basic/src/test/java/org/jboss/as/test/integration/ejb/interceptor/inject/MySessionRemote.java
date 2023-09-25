/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.interceptor.inject;

import java.util.ArrayList;
import jakarta.ejb.Remote;

/**
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 */
@Remote
public interface MySessionRemote {
    ArrayList doit();

}
