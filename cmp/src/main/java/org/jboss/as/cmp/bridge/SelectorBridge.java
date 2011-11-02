/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.cmp.bridge;

import java.lang.reflect.Method;
import javax.ejb.FinderException;
import org.jboss.as.cmp.context.CmpEntityBeanContext;

/**
 * SelectorBridge represents one ejbSelect method.
 * <p/>
 * Life-cycle:
 * Tied to the EntityBridge.
 * <p/>
 * Multiplicity:
 * One for each entity bean ejbSelect method.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @version $Revision: 81030 $
 */
public interface SelectorBridge extends EntityBridgeInvocationHandler.BridgeInvoker {
    String getSelectorName();

    Method getMethod();

    Object execute(CmpEntityBeanContext context, Object[] args) throws FinderException;
}
