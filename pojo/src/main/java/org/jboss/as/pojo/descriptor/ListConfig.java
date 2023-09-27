/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.pojo.descriptor;

import java.util.ArrayList;
import java.util.Collection;

/**
 * List meta data.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class ListConfig extends CollectionConfig {
    private static final long serialVersionUID = 1L;

    @Override
    protected Collection<Object> createDefaultInstance() {
        return new ArrayList<Object>();
    }
}