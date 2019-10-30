/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.jsr77.ejb;

import static org.jboss.as.jsr77.subsystem.Constants.EJB_IDENTIFIER;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.management.j2ee.Management;
import javax.management.j2ee.ManagementHome;

import org.jboss.as.ee.utils.DescriptorUtils;
import org.jboss.ejb.client.Affinity;
import org.jboss.ejb.client.EJBClient;
import org.jboss.ejb.client.StatelessEJBLocator;
import org.jboss.invocation.InterceptorContext;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ManagementHomeEjbComponentView extends BaseManagementEjbComponentView {

    private static final StatelessEJBLocator<Management> LOCATOR = StatelessEJBLocator.create(Management.class, EJB_IDENTIFIER, Affinity.LOCAL);

    private volatile Method create;

    @Override
    public Object invoke(InterceptorContext interceptorContext) throws Exception {
        if (interceptorContext.getMethod().equals(create)) {
            return EJBClient.createProxy(LOCATOR);
        }
        throw new UnsupportedOperationException(interceptorContext.getMethod().toString());
    }

    @Override
    Map<String, Map<String, Method>> initMethods() {
        try {
            create = ManagementHome.class.getMethod("create");

            //TODO all the other ejbhome interface methods
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        Map<String, Map<String, Method>> map = new HashMap<String, Map<String,Method>>();
        addMethod(map, create);
        return map;
    }

    private void addMethod(Map<String, Map<String, Method>> map, Method m) {
        map.put(m.getName(), Collections.singletonMap(DescriptorUtils.methodDescriptor(m), m));
    }

    @Override
    public <T> T getPrivateData(Class<T> clazz) {
        return null;
    }


}
