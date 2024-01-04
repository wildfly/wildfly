/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.sar.injection.depends;

import javax.management.ObjectName;

public interface AMBean {
    ObjectName getObjectName();

    void setObjectName(ObjectName text);
}
