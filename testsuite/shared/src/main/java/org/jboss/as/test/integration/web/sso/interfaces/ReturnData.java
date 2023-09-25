/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.sso.interfaces;

import java.io.Serializable;

/**
 * A class that is placed into the WEB-INF/classes directory that accesses a
 * class loaded from a jar jbosstest-web.ear/lib due to a reference from the
 * jbosstest-web-ejbs.jar manifest ClassPath.
 *
 * @author Scott.Stark@jboss.org
 */
public class ReturnData implements Serializable {

    private static final long serialVersionUID = -6620950977249481726L;

    public String data;
}
