/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.spi;

import org.jboss.as.ee.component.ComponentDescription;

/**
 * Certain Jakarta EE components imply an existence of an implicit bean archive.
 *
 * @author Martin Kouba
 */
public interface ImplicitBeanArchiveDetector {

    /**
     *
     * @param description
     * @return <code>true</code> if the specified component implies an implicit bean archive, <code>false</code> otherwise
     */
    boolean isImplicitBeanArchiveRequired(ComponentDescription description);

}
