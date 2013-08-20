package org.jboss.as.test.integration.ejb.entity.cmp.relationship.oneToOneBidirectionalWithTableMapping;

import java.rmi.RemoteException;
import javax.ejb.EJBException;
import javax.ejb.EntityBean;
import javax.ejb.EntityContext;
import javax.ejb.RemoveException;

/**
 * @ejb.bean
 *   name="Parent"
 *   type="CMP"
 *   cmp-version="2.x"
 *   view-type="local"
 *   reentrant="false"
 *   local-jndi-name="java:global/one2one-table-mapping/Parent!org.jboss.as.test.integration.ejb.entity.cmp.relationship.oneToOneBidirectionalWithTableMapping.ParentLocalHome"
 * @ejb.util generate="physical"
 * @ejb.pk generate="true"
 * @ejb.persistence table-name="PARENT"
 * @jboss.persistence
 *   create-table="true"
 *   remove-table="false"
 */
public abstract class ParentBean implements EntityBean {

    /*
	java:global/one2one-table-mapping/Parent!org.jboss.as.test.integration.ejb.entity.cmp.relationship.oneToOneBidirectionalWithTableMapping.ParentLocalHome
	java:app/one2one-table-mapping/Parent!org.jboss.as.test.integration.ejb.entity.cmp.relationship.oneToOneBidirectionalWithTableMapping.ParentLocalHome
	java:module/Parent!org.jboss.as.test.integration.ejb.entity.cmp.relationship.oneToOneBidirectionalWithTableMapping.ParentLocalHome
	java:global/one2one-table-mapping/Parent!org.jboss.as.test.integration.ejb.entity.cmp.relationship.oneToOneBidirectionalWithTableMapping.ParentLocal
	java:app/one2one-table-mapping/Parent!org.jboss.as.test.integration.ejb.entity.cmp.relationship.oneToOneBidirectionalWithTableMapping.ParentLocal
	java:module/Parent!org.jboss.as.test.integration.ejb.entity.cmp.relationship.oneToOneBidirectionalWithTableMapping.ParentLocal
    */

    /**
     * @ejb.pk-field
     * @ejb.persistent-field
     * @ejb.interface-method
     * @ejb.persistence column-name="ID"
     */
    public abstract Integer getId();
    
    public abstract void setId(Integer id);

    /**
     * @ejb.persistent-field
     * @ejb.interface-method
     * @ejb.persistence column-name="NAME"
     */
    public abstract String getName();

    /**
     * @ejb.interface-method
     */
    public abstract void setName(String name);

    /**
     * @ejb.interface-method
     *
     * @ejb.relation name="Parent-to-Child"
     *    role-name="Parent-has-one-child"
     *    target-ejb="ChildBean"
     *    target-role-name="Child-has-one-parent"
     *
     * @jboss.relation fk-column="ID"
     *     related-pk-field="id"
     */
    public abstract ChildLocal getChild();

    /**
     * @ejb.interface-method
     */
    public abstract void setChild(ChildLocal c);

    /**
     * @ejb.create-method
     */
    public ParentPK ejbCreate(ParentPK pk) {
        setId(pk.getId());
        return null;
    }

    public void ejbPostCreate(ParentPK pk) {}


    private EntityContext ctx;
    public void ejbActivate() throws EJBException, RemoteException {}
    public void ejbLoad() throws EJBException, RemoteException {}
    public void ejbPassivate() throws EJBException, RemoteException {}
    public void ejbRemove() throws RemoveException, EJBException, RemoteException {}
    public void ejbStore() throws EJBException, RemoteException {}
    public void setEntityContext(EntityContext ctx) throws EJBException, RemoteException {
        this.ctx = ctx;
    }
    public void unsetEntityContext() throws EJBException, RemoteException {
        this.ctx = null;
    }
}

