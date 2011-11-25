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
package org.jboss.as.test.integration.ejb.remote.entity.bmp;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.ejb.EJBException;
import javax.ejb.EntityBean;
import javax.ejb.EntityContext;
import javax.ejb.NoSuchEJBException;
import javax.ejb.RemoveException;

/**
 * @author Stuart Douglas
 */
public class SimpleBMPBean implements EntityBean {

    private static final AtomicInteger ID = new AtomicInteger();
    public static final int HOME_METHOD_RETURN = 100;

    private String myField;
    private EntityContext entityContext;
    private boolean ejbPostCreateCalled;


    public Integer ejbCreateEmpty() {
        int primaryKey = ID.incrementAndGet();
        DataStoreBean.DATA.put(primaryKey, myField);
        return primaryKey;
    }

    public Integer ejbCreateWithValue(String value) {
        int primaryKey = ID.incrementAndGet();
        myField = value;
        DataStoreBean.DATA.put(primaryKey, myField);
        return primaryKey;
    }

    public void ejbPostCreateEmpty() {
        ejbPostCreateCalled = true;
    }

    public void ejbPostCreateWithValue(String value) {
        ejbPostCreateCalled = true;
    }

    public int ejbHomeExampleHomeMethod() {
        return HOME_METHOD_RETURN;
    }

    public Integer ejbFindByValue(String value) {
        for(Map.Entry<Integer, String> entry : DataStoreBean.DATA.entrySet()) {
            if(entry.getValue().equals(value)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public Integer ejbFindByPrimaryKey(Integer primaryKey) {
        return DataStoreBean.DATA.containsKey(primaryKey) ?  primaryKey : null;
    }

    public Collection<Integer> ejbFindCollection() {
        final HashSet<Integer> set = new HashSet<Integer>();
        set.add(1000);
        set.add(1001);
        return set;
    }


    @Override
    public void setEntityContext(final EntityContext ctx) throws EJBException, RemoteException {
        this.entityContext = ctx;
    }

    @Override
    public void unsetEntityContext() throws EJBException, RemoteException {
        this.entityContext = null;
    }

    @Override
    public void ejbRemove() throws RemoveException, EJBException, RemoteException {
        DataStoreBean.DATA.remove(entityContext.getPrimaryKey());
    }



    @Override
    public void ejbActivate() throws EJBException, RemoteException {

    }

    @Override
    public void ejbPassivate() throws EJBException, RemoteException {
        myField = null;
    }

    @Override
    public void ejbLoad() throws EJBException, RemoteException {
        if(!DataStoreBean.DATA.containsKey(entityContext.getPrimaryKey())) {
            throw new NoSuchEJBException("no EJB with id: " + entityContext.getPrimaryKey());
        }
        this.myField = DataStoreBean.DATA.get(entityContext.getPrimaryKey());
    }

    @Override
    public void ejbStore() throws EJBException, RemoteException {
        DataStoreBean.DATA.put((Integer) entityContext.getPrimaryKey(), myField);
    }

    public String getMyField() {
        return myField;
    }

    public void setMyField(final String myField) {
        this.myField = myField;
    }

    public boolean isEjbPostCreateCalled() {
        return ejbPostCreateCalled;
    }
}
