/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component;

/**
 * An enum that is used as a marker for Enterprise Beans 2.x views.
 *
 * {@code MethodInterfaceType} is not sufficient for this, as it cannot differentiate
 * between Enterprise Beans 3 business and Enterprise Beans 2 component views
 *
 * @author Stuart Douglas
 */
public enum Ejb2xViewType {

    LOCAL,
    LOCAL_HOME,
    REMOTE,
    HOME,
}
