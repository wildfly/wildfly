/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.connector.deployers.spec.rars;

import javax.naming.Reference;

/**
 * BaseReference
 *
 * @author <a href="mailto:jeff.zhang@ironjacamar.org">Jeff Zhang</a>.
 */
public class BaseReference extends Reference {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 1L;

    /**
     * Contains the fully-qualified name of the class of the object to which this Reference refers.
     *
     * @param className class name
     * @see java.lang.Class#getName
     */
    public BaseReference(String className) {
        super(className);
    }

}
