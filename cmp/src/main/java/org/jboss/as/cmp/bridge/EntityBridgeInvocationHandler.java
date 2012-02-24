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
import java.util.Map;
import javax.ejb.EJBException;
import javax.ejb.FinderException;
import org.jboss.as.cmp.CmpMessages;
import org.jboss.as.cmp.component.CmpEntityBeanComponentInstance;
import org.jboss.as.cmp.context.CmpEntityBeanContext;

/**
 * EntityBridgeInvocationHandler is the invocation handler used by the CMP 2.x
 * dynamic proxy. This class only interacts with the EntityBridge. The main
 * job of this class is to delegate invocation of abstract methods to the
 * appropriate EntityBridge method.
 * <p/>
 * Life-cycle:
 * Tied to the life-cycle of an entity bean instance.
 * <p/>
 * Multiplicity:
 * One per cmp entity bean instance, including beans in pool.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @author <a href="mailto:alex@jboss.org">Alexey Loubyansky</a>
 * @version $Revision: 81030 $
 */
public class EntityBridgeInvocationHandler {

    private final Map<String, BridgeInvoker> fieldMap;
    private final Map<Method, BridgeInvoker> selectorMap;

    public EntityBridgeInvocationHandler(final Map<String, BridgeInvoker> fieldMap, final Map<Method, BridgeInvoker> selectorMap) {
        this.fieldMap = fieldMap;
        this.selectorMap = selectorMap;
    }

    public Object invoke(final CmpEntityBeanComponentInstance instance, final Object proxy, final Method method, final Object[] args) throws FinderException {
        String methodName = method.getName();

        BridgeInvoker invoker = fieldMap.get(methodName);
        if (invoker == null) {
            invoker = selectorMap.get(method);
            if (invoker == null) {
                throw CmpMessages.MESSAGES.methodNoCmpAccessor(methodName);
            }
        }
        try {
            return invoker.invoke(instance.getEjbContext(), method, args);
        } catch (RuntimeException e) {
            throw e;
        } catch (FinderException e) {
            throw e;
        } catch (Exception e) {
            throw CmpMessages.MESSAGES.internalInvocationBridgeError(e);
        }
    }

    // Inner

    public interface BridgeInvoker {
        Object invoke(CmpEntityBeanContext ctx, Method method, Object[] args) throws Exception;
    }

    public static class FieldGetInvoker implements BridgeInvoker {
        private final FieldBridge field;

        public FieldGetInvoker(FieldBridge field) {
            this.field = field;
        }

        public Object invoke(CmpEntityBeanContext ctx, Method method, Object[] args) {
            // In the case of ejbHome methods there is no context, but ejb home
            // methods are only allowed to call selectors.
            if (ctx == null) {
                throw CmpMessages.MESSAGES.homeMethodsCanNotAccessCmpFields(method.getName());
            }

            return field.getValue(ctx);
        }
    }

    public static class FieldSetInvoker implements BridgeInvoker {
        private final FieldBridge field;

        public FieldSetInvoker(FieldBridge field) {
            this.field = field;
        }

        public Object invoke(CmpEntityBeanContext ctx, Method method, Object[] args) {
            // In the case of ejbHome methods there is no context, but ejb home
            // methods are only allowed to call selectors.
            if (ctx == null) {
                throw CmpMessages.MESSAGES.homeMethodsCanNotAccessCmpFields(method.getName());
            }
            field.setValue(ctx, args[0]);
            return null;
        }
    }


}
