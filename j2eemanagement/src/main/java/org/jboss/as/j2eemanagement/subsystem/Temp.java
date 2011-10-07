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
package org.jboss.as.j2eemanagement.subsystem;

import java.rmi.RemoteException;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import javax.management.j2ee.ListenerRegistration;
import javax.management.j2ee.Management;
import javax.management.j2ee.ManagementHome;
import javax.naming.InitialContext;

import org.jboss.ejb.client.EJBClientContext;

/**
 * A temp mbean to wrap calls to the management ejb while
 * waiting for proper remote ejb calls to be implemented
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class Temp implements TempMBean {

    private final EJBClientContext context;

    Temp(EJBClientContext context){
        this.context = context;
    }

    private Management getManagement() {
        try {
            InitialContext ctx = new InitialContext();
            Object o = ctx.lookup(Constants.JNDI_NAME);
            //System.out.println(o);
            System.out.println(o.getClass());
            for (Class<?> iface : o.getClass().getInterfaces()) {
                System.out.println(iface);
            }

            return ((ManagementHome)o).create();
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public Set queryNames(ObjectName name, QueryExp query) {
        //EJBClientContext.setSelector(newSelector) Current(context);
        try {
            return getManagement().queryNames(name, query);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } finally {
            //EJBClientContext.suspendCurrent();
        }
    }

    @Override
    public boolean isRegistered(ObjectName name) {
        //EJBClientContext.setCurrent(context);
        try {
            return getManagement().isRegistered(name);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } finally {
            //EJBClientContext.suspendCurrent();
        }
    }

    @Override
    public Integer getMBeanCount() {
        //EJBClientContext.setCurrent(context);
        try {
            return getManagement().getMBeanCount();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } finally {
            //EJBClientContext.suspendCurrent();
        }
    }

    @Override
    public MBeanInfo getMBeanInfo(ObjectName name) throws IntrospectionException, InstanceNotFoundException, ReflectionException {
        //EJBClientContext.setCurrent(context);
        try {
            return getManagement().getMBeanInfo(name);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } finally {
            //EJBClientContext.suspendCurrent();
        }
    }

    @Override
    public Object getAttribute(ObjectName name, String attribute) throws MBeanException, AttributeNotFoundException,
            InstanceNotFoundException, ReflectionException {
        //EJBClientContext.setCurrent(context);
        try {
            return getManagement().getAttribute(name, attribute);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } finally {
            //EJBClientContext.suspendCurrent();
        }
    }

    @Override
    public AttributeList getAttributes(ObjectName name, String[] attributes) throws InstanceNotFoundException,
            ReflectionException {
        //EJBClientContext.setCurrent(context);
        try {
            return getManagement().getAttributes(name, attributes);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } finally {
            //EJBClientContext.suspendCurrent();
        }
    }

    @Override
    public void setAttribute(ObjectName name, Attribute attribute) throws InstanceNotFoundException, AttributeNotFoundException,
            InvalidAttributeValueException, MBeanException, ReflectionException {
        //EJBClientContext.setCurrent(context);
        try {
            getManagement().setAttribute(name, attribute);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } finally {
            //EJBClientContext.suspendCurrent();
        }
    }

    @Override
    public AttributeList setAttributes(ObjectName name, AttributeList attributes) throws InstanceNotFoundException,
            ReflectionException {
        //EJBClientContext.setCurrent(context);
        try {
            return getManagement().setAttributes(name, attributes);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } finally {
            //EJBClientContext.suspendCurrent();
        }
    }

    @Override
    public Object invoke(ObjectName name, String operationName, Object[] params, String[] signature)
            throws InstanceNotFoundException, MBeanException, ReflectionException {
        //EJBClientContext.setCurrent(context);
        try {
            return getManagement().invoke(name, operationName, params, signature);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } finally {
            //EJBClientContext.suspendCurrent();
        }
    }

    @Override
    public String getDefaultDomain() {
        //EJBClientContext.setCurrent(context);
        try {
            return getManagement().getDefaultDomain();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } finally {
            //EJBClientContext.suspendCurrent();
        }
    }

    @Override
    public ListenerRegistration getListenerRegistry() {
        //EJBClientContext.setCurrent(context);
        try {
            return getManagement().getListenerRegistry();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } finally {
            //EJBClientContext.suspendCurrent();
        }
    }
}
