/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.resourceadapterbinding.metadata;

import org.jboss.metadata.ejb.parser.jboss.ejb3.AbstractEJBBoundMetaData;

/**
 * Metadata for resource adapter binding of message driven beans
 *
 * @author <a href="mailto:robert.panzer@wincor-nixdorf.com">Robert Panzer</a>
 *
 */
public class EJBBoundResourceAdapterBindingMetaData extends AbstractEJBBoundMetaData {

    private static final long serialVersionUID = -5981839317495286524L;

    private String resourceAdapterName;

    public String getResourceAdapterName() {
        return resourceAdapterName;
    }

    public void setResourceAdapterName(String resourceAdapterName) {
        this.resourceAdapterName = resourceAdapterName;
    }

}
