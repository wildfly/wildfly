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
package org.jboss.as.test.integration.ejb.entity.cmp.optimisticlock;

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.FinderException;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.jboss.as.test.integration.ejb.entity.cmp.optimisticlock.interfaces.CmpEntityLocal;
import org.jboss.as.test.integration.ejb.entity.cmp.optimisticlock.interfaces.CmpEntityLocalHome;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:aloubyansky@hotmail.com">Alex Loubyansky</a>
 * @ejb.bean name="Facade"
 * type="Stateless"
 * jndi-name="FacadeBean"
 * view-type="remote"
 */
@Stateless
@LocalBean
@TransactionManagement(TransactionManagementType.CONTAINER)
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class FacadeBean {
    // Attributes --------------------------------------------
    static Logger log = Logger.getLogger(FacadeBean.class);

    @Resource
    private SessionContext ejbContext;

    // Business methods --------------------------------------
    public void createCmpEntity(String jndiName,
                                Integer id,
                                String stringGroup1,
                                Integer integerGroup1,
                                Double doubleGroup1,
                                String stringGroup2,
                                Integer integerGroup2,
                                Double doubleGroup2)
            throws Exception {
        System.out.println("Facade.createCmpEntity");
        if (log.isDebugEnabled()) {
            log.debug("createCmpEntity> jndiName=" + jndiName
                    + ", id=" + id
                    + ", stringGroup1=" + stringGroup1
                    + ", integerGroup1=" + integerGroup1
                    + ", doubleGroup1=" + doubleGroup1
                    + ", stringGroup2=" + stringGroup2
                    + ", integerGroup2=" + integerGroup2
                    + ", doubleGroup2=" + doubleGroup2);
        }

        CmpEntityLocalHome entityHome = getCmpEntityHome(jndiName);
        entityHome.create(id, stringGroup1, integerGroup1, doubleGroup1,
                stringGroup2, integerGroup2, doubleGroup2);
    }

    /**
     * @ejb.interface-method
     */
    public void safeRemove(String jndiName, Integer id) throws Exception {
        log.debug("safeRemove> jndiName=" + jndiName + ", id=" + id);
        try {
            CmpEntityLocal entity = getCmpEntityHome(jndiName).findByPrimaryKey(id);
            entity.remove();
        } catch (FinderException e) {
        }
    }

    /**
     * @ejb.interface-method
     */
    public void testNullLockedFields(String jndiName, Integer id) throws Exception {
        log.debug("testNullLockedFields> begin");
        CmpEntityLocal entity = getCmpEntityHome(jndiName).findByPrimaryKey(id);
        entity.setDoubleGroup1(new Double(11.11));
        entity.setDoubleGroup2(new Double(22.22));
        entity.setIntegerGroup1(new Integer(11));
        entity.setIntegerGroup2(new Integer(22));
        entity.setStringGroup1("str1 modified in testNullLockedFields");
        entity.setStringGroup2("str2 modified in testNullLockedFields");
        log.debug("testNullLockedFields> done");
    }

    /**
     * @ejb.interface-method
     */
    public void testKeygenStrategyPass(String jndiName, Integer id) throws Exception {
        log.debug("testKeygenStrategyPass> begin");
        CmpEntityLocal entity = getCmpEntityHome(jndiName).findByPrimaryKey(id);
        entity.setIntegerGroup1(new Integer(111));
        entity.setStringGroup2("modified in testKeygenStrategyPass");
        entity.getDoubleGroup1();
        log.debug("testKeygenStrategyPass> done");
    }

    public void testKeygenStrategyFail(String jndiName, Integer id) throws Exception {
        log.info("testKeygenStrategyFail> begin");
        CmpEntityLocal entity = getCmpEntityHome(jndiName).findByPrimaryKey(id);
        entity.setIntegerGroup1(new Integer(111));
        entity.setStringGroup2("modified in testKeygenStrategyFail");
        entity.getDoubleGroup1();
        getFacade().modifyGroup1InRequiresNew(jndiName, id);
        log.info("testKeygenStrategyFail> done");
    }

    /**
     * @ejb.interface-method
     */
    public void testTimestampStrategyPass(String jndiName, Integer id) throws Exception {
        log.debug("testTimestampStrategyPass> begin");
        CmpEntityLocal entity = getCmpEntityHome(jndiName).findByPrimaryKey(id);
        entity.setIntegerGroup1(new Integer(111));
        entity.setStringGroup2("modified in testTimestampStrategyPass");
        entity.getDoubleGroup1();
        log.debug("testTimestampStrategyPass> done");
    }

    /**
     * @ejb.interface-method
     */
    public void testTimestampStrategyFail(String jndiName, Integer id) throws Exception {
        log.debug("testTimestampStrategyFail> begin");
        CmpEntityLocal entity = getCmpEntityHome(jndiName).findByPrimaryKey(id);
        entity.setIntegerGroup1(new Integer(111));
        entity.setStringGroup2("modified in testTimestampStrategyFail");
        entity.getDoubleGroup1();
        getFacade().modifyGroup1InRequiresNew(jndiName, id);
        log.debug("testTimestampStrategyFail> done");
    }

    /**
     * @ejb.interface-method
     */
    public void testVersionStrategyPass(String jndiName, Integer id) throws Exception {
        log.debug("testVersionStrategyPass> begin");
        CmpEntityLocal entity = getCmpEntityHome(jndiName).findByPrimaryKey(id);
        entity.setIntegerGroup1(new Integer(111));
        entity.setStringGroup2("modified in testVersionStrategyPass");
        entity.getDoubleGroup1();
        log.debug("testVersionStrategyPass> done");
    }

    /**
     * @ejb.interface-method
     */
    public void testVersionStrategyFail(String jndiName, Integer id) throws Exception {
        log.debug("testVersionStrategyFail> begin");
        CmpEntityLocal entity = getCmpEntityHome(jndiName).findByPrimaryKey(id);
        entity.setIntegerGroup1(new Integer(111));
        entity.setStringGroup2("modified in testVersionStrategyFail");
        entity.getDoubleGroup1();
        getFacade().modifyGroup1InRequiresNew(jndiName, id);
        log.debug("testVersionStrategyFail> done");
    }

    /**
     * @ejb.interface-method
     */
    public void testGroupStrategyPass(String jndiName, Integer id) throws Exception {
        log.debug("testGroupStrategyPass> begin");
        CmpEntityLocal entity = getCmpEntityHome(jndiName).findByPrimaryKey(id);
        entity.setIntegerGroup1(new Integer(111));
        entity.setStringGroup2("modified in testGroupStrategyPass");
        entity.getDoubleGroup1();
        getFacade().modifyGroup1InRequiresNew(jndiName, id);
        log.debug("testGroupStrategyPass> done");
    }

    /**
     * @ejb.interface-method
     */
    public void testGroupStrategyFail(String jndiName, Integer id) throws Exception {
        log.debug("testGroupStrategyFail> begin");
        CmpEntityLocal entity = getCmpEntityHome(jndiName).findByPrimaryKey(id);
        entity.setIntegerGroup1(new Integer(111));
        entity.setStringGroup2("modified in testGroupStrategyPass");
        entity.getDoubleGroup1();
        getFacade().modifyGroup2InRequiresNew(jndiName, id);
        log.debug("testGroupStrategyFail> done");
    }

    /**
     * @ejb.interface-method
     */
    public void testReadStrategyPass(String jndiName, Integer id) throws Exception {
        log.debug("testReadStrategyPass> begin");
        CmpEntityLocal entity = getCmpEntityHome(jndiName).findByPrimaryKey(id);
        entity.setIntegerGroup1(new Integer(111));
        entity.getStringGroup1();
        entity.getDoubleGroup1();
        getFacade().modifyGroup2InRequiresNew(jndiName, id);
        log.debug("testReadStrategyPass> done");
    }

    /**
     * @ejb.interface-method
     */
    public void testReadStrategyFail(String jndiName, Integer id) throws Exception {
        log.debug("testReadStrategyFail> begin");
        CmpEntityLocal entity = getCmpEntityHome(jndiName).findByPrimaryKey(id);
        entity.setIntegerGroup1(new Integer(111));
        entity.getStringGroup2();
        entity.getDoubleGroup1();
        getFacade().modifyGroup2InRequiresNew(jndiName, id);
        log.debug("testReadStrategyFail> done");
    }

    /**
     * @ejb.interface-method
     */
    public void testModifiedStrategyPass(String jndiName, Integer id) throws Exception {
        log.debug("testModifiedStrategyPass> begin");
        CmpEntityLocal entity = getCmpEntityHome(jndiName).findByPrimaryKey(id);
        entity.setIntegerGroup1(new Integer(111));
        entity.setStringGroup1("modified in testModifiedStrategyPass");
        entity.setDoubleGroup1(new Double(111.111));
        getFacade().modifyGroup2InRequiresNew(jndiName, id);
        log.debug("testModifiedStrategyPass> done");
    }

    /**
     * @ejb.interface-method
     */
    public void testModifiedStrategyFail(String jndiName, Integer id) throws Exception {
        log.debug("testModifiedStrategyFail> begin");
        CmpEntityLocal entity = getCmpEntityHome(jndiName).findByPrimaryKey(id);
        entity.setStringGroup2("modified by testModifiedStrategyFail");
        getFacade().modifyGroup2InRequiresNew(jndiName, id);
        log.debug("testModifiedStrategyFail> done");
    }

    /**
     * @ejb.interface-method
     * @ejb.transaction type="RequiresNew"
     */
    public void modifyGroup2InRequiresNew(String jndiName, Integer id) throws Exception {
        log.debug("modifyGroup2InRequiresNew");
        CmpEntityLocal entity = getCmpEntityHome(jndiName).findByPrimaryKey(id);
        entity.setIntegerGroup2(new Integer(222));
        entity.setStringGroup2("modified by modifyGroup2InRequiresNew");
        entity.setDoubleGroup2(new Double(222.222));
    }

    /**
     * @ejb.interface-method
     * @ejb.transaction type="RequiresNew"
     */
    public void modifyGroup1InRequiresNew(String jndiName, Integer id) throws Exception {
        log.info("modifyGroup1InRequiresNew> begin");
        CmpEntityLocal entity = getCmpEntityHome(jndiName).findByPrimaryKey(id);
        entity.setIntegerGroup1(new Integer(333));
        entity.setStringGroup1("modified by modifyGroup1InRequiresNew");
        entity.setDoubleGroup1(new Double(333.333));
        log.info("modifyGroup1InRequiresNew> done");
    }

    /**
     * @ejb.interface-method
     */
    public void testUpdateLockOnSync(String jndiName, Integer id) throws Exception {
        log.debug("testUpdateLockOnSync> begin");
        CmpEntityLocal entity = getCmpEntityHome(jndiName).findById(id);
        entity.setStringGroup1("FIRST UPDATE");

        entity = getCmpEntityHome(jndiName).findById(id);
        entity.setStringGroup1("SECOND UPDATE");

        log.debug("testUpdateLockOnSync> done");
    }

    /**
     * @ejb.interface-method
     */
    public void testExplicitVersionUpdateOnSync(String jndiName, Integer id) throws Exception {
        log.debug("testExplicitVersionUpdateOnSync> begin");
        CmpEntityLocal entity = getCmpEntityHome(jndiName).findById(id);
        if (entity.getVersionField().longValue() != 1)
            throw new Exception("entity.getVersionField().longValue() != 1");
        entity.setStringGroup1("FIRST UPDATE");

        entity = getCmpEntityHome(jndiName).findById(id);
        if (entity.getVersionField().longValue() != 2)
            throw new Exception("entity.getVersionField().longValue() != 2");
        entity.setStringGroup1("SECOND UPDATE");

        log.debug("testExplicitVersionUpdateOnSync> done");
    }

    // Private -----------------------------------------------
    private CmpEntityLocalHome getCmpEntityHome(String entityJndiName)
            throws NamingException {
        InitialContext ic = new InitialContext();
        CmpEntityLocalHome cmpEntityHome = (CmpEntityLocalHome)
                ic.lookup(entityJndiName);
        return cmpEntityHome;
    }

    private FacadeBean getFacade() throws NamingException {
        return (FacadeBean) new InitialContext().lookup("java:module/FacadeBean!org.jboss.as.test.integration.ejb.entity.cmp.optimisticlock.FacadeBean");
    }
}
