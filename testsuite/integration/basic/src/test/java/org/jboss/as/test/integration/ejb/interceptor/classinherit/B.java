/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.interceptor.classinherit;

/**
 * @author <a href="mailto:amay@ingenta.com">Andrew May</a>
 */
public interface B extends A {
    String getOtherMessage();
}
