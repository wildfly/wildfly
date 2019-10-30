/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.clustering.cluster.ejb.forwarding.bean.common;

import javax.annotation.PostConstruct;
import javax.ejb.Remove;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.jboss.logging.Logger;

@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class CommonStatefulSBImpl implements CommonStatefulSB {

    private int serial;
    private static final Logger log = Logger.getLogger(CommonStatefulSBImpl.class.getName());

    @PostConstruct
    private void init() {
        serial = 0;
        log.infof("New SFSB created: %s.", this);
    }

    @Override
    public int getSerialAndIncrement() {
        log.infof("getSerialAndIncrement() called on non-forwarding node %s", getCurrentNode());
        return serial++;
    }

    @Remove
    private void destroy() {
        serial = -1;
    }

    private static String getCurrentNode() {
        return System.getProperty("jboss.node.name", "unknown");
    }
}
