/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.pojo.descriptor;

import java.util.Collection;
import java.util.HashSet;

/**
 * Set meta data.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class SetConfig extends CollectionConfig {
    private static final long serialVersionUID = 1L;

    @Override
    protected Collection<Object> createDefaultInstance() {
        return new HashSet<Object>();
    }
}