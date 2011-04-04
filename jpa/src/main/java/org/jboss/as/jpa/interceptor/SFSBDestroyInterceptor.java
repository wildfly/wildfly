/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jpa.interceptor;

import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.ee.component.ComponentLifecycle;
import org.jboss.as.ejb3.component.stateful.StatefulSessionComponentInstance;
import org.jboss.as.jpa.container.AbstractEntityManager;
import org.jboss.as.jpa.container.ExtendedEntityManager;
import org.jboss.as.jpa.container.SFSBXPCMap;
import org.jboss.as.jpa.ejb3.SFSBContextHandleImpl;

import javax.persistence.EntityManager;
import java.util.List;

/**
 * For SFSB life cycle management.
 * Handles the closing of XPC after last SFSB using it is destroyed.
 *
 * @author Scott Marlow
 */
public class SFSBDestroyInterceptor implements ComponentLifecycle {


    @Override
    public void invoke(ComponentInstance target) throws Exception {
        StatefulSessionComponentInstance sfsb = (StatefulSessionComponentInstance)target;
        SFSBContextHandleImpl sfsbContextHandle = new SFSBContextHandleImpl(sfsb);
        List<EntityManager> readyToClose = SFSBXPCMap.getINSTANCE().remove(sfsbContextHandle);
        if (readyToClose != null && readyToClose.size() > 0) {
            for( EntityManager entityManager: readyToClose) {
                if (entityManager instanceof ExtendedEntityManager) {
                    // TODO:  continue iteratorating through remaing entity managers and chain any exceptions
                    ((ExtendedEntityManager) entityManager).containerClose();
                }
                else {
                    throw new RuntimeException("can only close SFSB XPC entity manager that are instances of ExtendedEntityManager" + entityManager.getClass().getName());
                }
            }
        }
    }
}
