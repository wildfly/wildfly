/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.testsuite.integration.secman.ejbs;

/**
 * Local interface dedicated for ReadSystemPropertyBean.
 *
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
public interface ReadSystemPropertyLocal {
    String readSystemProperty(final String propertyName);
}
