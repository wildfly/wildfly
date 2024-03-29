/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.resource.enventry;

import jakarta.annotation.ManagedBean;
import jakarta.annotation.Resource;

/**
 * @author Stuart Douglas
 */
@SuppressWarnings("deprecation")
@ManagedBean
public class EnvEntryManagedBean {

    @Resource
    private String nonExistentString = "hi";

    @Resource
    private String existingString = "hi";

    private byte byteField;

    public String getNonExistentString() {
        return nonExistentString;
    }

    public String getExistingString() {
        return existingString;
    }

    public byte getByteField() {
        return byteField;
    }
}
