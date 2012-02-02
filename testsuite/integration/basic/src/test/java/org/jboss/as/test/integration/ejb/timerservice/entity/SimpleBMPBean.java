/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.test.integration.ejb.timerservice.entity;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import javax.ejb.EJBException;
import javax.ejb.EntityBean;
import javax.ejb.EntityContext;
import javax.ejb.NoSuchEJBException;
import javax.ejb.RemoveException;
import javax.ejb.TimedObject;
import javax.ejb.Timer;

import org.jboss.logging.Logger;

/**
 * @author Stuart Douglas
 */
public class SimpleBMPBean implements EntityBean, TimedObject {
    private static final Logger log = Logger.getLogger(SimpleBMPBean.class);
    private static final long serialVersionUID = 1L;
    private static final AtomicInteger ID = new AtomicInteger();
    public static final int HOME_METHOD_RETURN = 100;
    private static int TIMER_TIMEOUT_TIME_MS = 100;

    private String myField;
    private EntityContext entityContext;
    private boolean ejbPostCreateCalled;
   
    private static CountDownLatch latch = new CountDownLatch(2);
    private static final Map<Integer, String> timerData = new HashMap<Integer, String>();
    
    public Integer ejbCreateEmpty() {
        int primaryKey = ID.incrementAndGet();
        DataStore.DATA.put(primaryKey, myField);
        return primaryKey;
    }

    public Integer ejbCreateWithValue(String value) {
        int primaryKey = ID.incrementAndGet();
        myField = value;
        DataStore.DATA.put(primaryKey, myField);
        return primaryKey;
    }

    public Integer ejbCreateWithValueAndPk(Integer pk, String value) {
        myField = value;
        DataStore.DATA.put(pk, myField);
        return pk;
    }

    public void ejbPostCreateEmpty() {
        ejbPostCreateCalled = true;
    }

    public void ejbPostCreateWithValue(String value) {
        ejbPostCreateCalled = true;
    }

    public void ejbPostCreateWithValueAndPk(Integer pk, String value) {
        ejbPostCreateCalled = true;
    }

    public int ejbHomeExampleHomeMethod() {
        return HOME_METHOD_RETURN;
    }

    public Integer ejbFindByValue(String value) {
        for(Map.Entry<Integer, String> entry : DataStore.DATA.entrySet()) {
            if(entry.getValue().equals(value)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public Integer ejbFindByPrimaryKey(Integer primaryKey) {
        return DataStore.DATA.containsKey(primaryKey) ?  primaryKey : null;
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
        DataStore.DATA.remove(entityContext.getPrimaryKey());
    }

    @Override
    public void ejbActivate() throws EJBException, RemoteException {
        log.info("EJB " + ID + " activated [" + entityContext.getPrimaryKey() + "]");
    }

    @Override
    public void ejbPassivate() throws EJBException, RemoteException {
        myField = null;
    }

    @Override
    public void ejbLoad() throws EJBException, RemoteException {
        if(!DataStore.DATA.containsKey(entityContext.getPrimaryKey())) {
            throw new NoSuchEJBException("no EJB with id" + entityContext.getPrimaryKey());
        }
        this.myField = DataStore.DATA.get(entityContext.getPrimaryKey());
    }

    @Override
    public void ejbStore() throws EJBException, RemoteException {
        DataStore.DATA.put((Integer) entityContext.getPrimaryKey(), myField);
    }

    public void setupTimer() {
        entityContext.getTimerService().createTimer(TIMER_TIMEOUT_TIME_MS, null);
    }

    public void setupTimerDefined(int time) {
        entityContext.getTimerService().createTimer(time, null);
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

    @Override
    public synchronized void ejbTimeout(final Timer timer) {
        timerData.put((Integer) entityContext.getPrimaryKey(), getMyField());
        latch.countDown();
    }

    public static Map<Integer, String> getTimerData() {
        return timerData;
    }

    public static CountDownLatch getLatch() {
        return latch;
    }
    
    public static void redefineLatch(int countDownLatchNumber) {
        latch = new CountDownLatch(countDownLatchNumber);
    }
    
    public Collection<Timer> getTimers() {
        return entityContext.getTimerService().getTimers();
    }
}
