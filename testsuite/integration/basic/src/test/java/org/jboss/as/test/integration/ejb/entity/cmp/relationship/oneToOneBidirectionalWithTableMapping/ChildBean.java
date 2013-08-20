package org.jboss.as.test.integration.ejb.entity.cmp.relationship.oneToOneBidirectionalWithTableMapping;

import java.rmi.RemoteException;
import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.EntityBean;
import javax.ejb.EntityContext;
import javax.ejb.RemoveException;

/**
 * @ejb.bean
 *   name="Child"
 *   type="CMP"
 *   cmp-version="2.x"
 *   view-type="local"
 *   reentrant="false"
 *   local-jndi-name="java:global/one2one-table-mapping/Child!org.jboss.as.test.integration.ejb.entity.cmp.relationship.oneToOneBidirectionalWithTableMapping.ChildLocalHome"
 * @ejb.util generate="physical"
 * @ejb.pk generate="true"
 * @ejb.persistence table-name="CHILD"
 * @jboss.persistence
 *   create-table="true"
 *   remove-table="false"
 */
public abstract class ChildBean implements EntityBean {

    /*
	java:global/one2one-table-mapping/Child!org.jboss.as.test.integration.ejb.entity.cmp.relationship.oneToOneBidirectionalWithTableMapping.ChildLocalHome
	java:app/one2one-table-mapping/Child!org.jboss.as.test.integration.ejb.entity.cmp.relationship.oneToOneBidirectionalWithTableMapping.ChildLocalHome
	java:module/Child!org.jboss.as.test.integration.ejb.entity.cmp.relationship.oneToOneBidirectionalWithTableMapping.ChildLocalHome
	java:global/one2one-table-mapping/Child!org.jboss.as.test.integration.ejb.entity.cmp.relationship.oneToOneBidirectionalWithTableMapping.ChildLocal
	java:app/one2one-table-mapping/Child!org.jboss.as.test.integration.ejb.entity.cmp.relationship.oneToOneBidirectionalWithTableMapping.ChildLocal
	java:module/Child!org.jboss.as.test.integration.ejb.entity.cmp.relationship.oneToOneBidirectionalWithTableMapping.ChildLocal
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
     *    role-name="Child-has-one-parent"
     *    target-ejb="ExampleBean"
     *    target-role-name="Parent-has-one-child"
     *    target-multiple="false"
     *
     * @jboss.relation fk-column="ID"
     *     related-pk-field="id"
     */
    public abstract ParentLocal getParent();

    /**
     * @ejb.interface-method
     */
    public abstract void setParent(ParentLocal el);

    /**
     * @ejb.create-method
     */
    public ChildPK ejbCreate(ChildPK pk) throws CreateException {
        setId(pk.getId());
        return null;
    }

    public void ejbPostCreate(ChildPK pk) {}


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

