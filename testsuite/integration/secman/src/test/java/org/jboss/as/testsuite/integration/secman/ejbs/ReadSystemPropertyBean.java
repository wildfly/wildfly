/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.testsuite.integration.secman.ejbs;

import jakarta.ejb.Local;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;

/**
 * A SLSB bean which reads the given system property and returns its value.
 *
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
@Stateless
@Remote(ReadSystemPropertyRemote.class)
@Local(ReadSystemPropertyLocal.class)
public class ReadSystemPropertyBean {
    public String readSystemProperty(final String propertyName) {
        return System.getProperty(propertyName, "");
    }
}
