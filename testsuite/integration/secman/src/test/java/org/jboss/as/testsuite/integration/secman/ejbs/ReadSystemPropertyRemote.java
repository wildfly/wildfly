/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.testsuite.integration.secman.ejbs;

/**
 * Remote interface dedicated for ReadSystemPropertyBean.
 *
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
public interface ReadSystemPropertyRemote {
    String readSystemProperty(final String propertyName);
}
