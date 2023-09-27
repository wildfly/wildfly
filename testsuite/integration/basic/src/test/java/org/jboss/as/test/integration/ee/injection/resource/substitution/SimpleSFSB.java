/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.resource.substitution;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateful;

/**
 * @author wangchao
 *
 */
@Stateful
public class SimpleSFSB {

    @Resource(name = "${resource.name}")
    private String resourceByName;

    @Resource(lookup = "${resource.lookup}")
    private Object resourceByLookupName;

    @Resource(mappedName = "${resource.mappedname}")
    private Object resourceByMappedName;

    public boolean isResourceWithNameInjected() {
        return this.resourceByName != null;
    }

    public boolean isResourceWithMappedNameInjected() {
        return this.resourceByMappedName != null;
    }

    public boolean isResourceWithLookupNameInjected() {
        return this.resourceByLookupName != null;
    }
}
