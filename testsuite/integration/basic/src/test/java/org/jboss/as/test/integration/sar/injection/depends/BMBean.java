/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.sar.injection.depends;

import java.util.List;

import javax.management.ObjectName;

public interface BMBean {
        List<ObjectName> getObjectNames();
        void setObjectNames(List<ObjectName> names);
}
