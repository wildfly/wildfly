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

package org.jboss.as.test.clustering.cluster.ejb.forwarding.bean.forwarding;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.jboss.as.test.clustering.cluster.ejb.forwarding.bean.stateful.RemoteStatefulSB;
import org.jboss.as.test.clustering.ejb.EJBDirectory;
import org.jboss.as.test.clustering.ejb.RemoteEJBDirectory;
import org.jboss.logging.Logger;

/**
 * @author Radoslav Husar
 */
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public abstract class AbstractForwardingStatefulSBImpl {

    private static final Logger log = Logger.getLogger(AbstractForwardingStatefulSBImpl.class.getName());
    public static final String MODULE_NAME = "terminus";

    private RemoteStatefulSB bean;

    @SuppressWarnings("unchecked")
    private RemoteStatefulSB forward() {
        if (bean == null) {
            try (EJBDirectory directory = new RemoteEJBDirectory(MODULE_NAME)) {
                bean = directory.lookupStateful("RemoteStatefulSBImpl", RemoteStatefulSB.class);
            } catch (Exception e) {
                log.infof("exception occurred looking up ejb on forwarding node %s", getCurrentNode());
                throw new RuntimeException(e);
            }
        }
        return bean;
    }

    public int getSerialAndIncrement() {
        log.infof("getSerialAndIncrement() called on forwarding node %s", getCurrentNode());
        return forward().getSerialAndIncrement();
    }

    private static String getCurrentNode() {
        return System.getProperty("jboss.node.name", "unknown");
    }
}
