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

package org.jboss.as.cmp.jdbc;

import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.Collection;
import javax.crypto.KeyGenerator;
import javax.ejb.RemoveException;
import javax.sql.DataSource;
import org.jboss.as.cmp.CmpConfig;
import org.jboss.as.cmp.bridge.EntityBridgeInvocationHandler;
import org.jboss.as.cmp.component.CmpEntityBeanComponent;
import org.jboss.as.cmp.context.CmpEntityBeanContext;
import org.jboss.as.cmp.ejbql.Catalog;
import org.jboss.as.cmp.jdbc.bridge.JDBCAbstractEntityBridge;
import org.jboss.as.cmp.jdbc.metadata.JDBCEntityMetaData;
import org.jboss.as.cmp.keygenerator.KeyGeneratorFactory;

/**
 * @author <a href="mailto:alex@jboss.org">Alexey Loubyansky</a>
 * @version <tt>$Revision: 81030 $</tt>
 */
public interface JDBCEntityPersistenceStore {
    /**
     * Returns a new instance of the bean class or a subclass of the bean class.
     *
     * @return the new instance
     * @throws Exception
     */
    Object createBeanClassInstance() throws Exception;

    /**
     * Initializes the instance context.
     * <p/>
     * <p>This method is called before createEntity, and should
     * resetStats the value of all cmpFields to 0 or null.
     *
     * @param ctx
     */
    void initEntity(CmpEntityBeanContext ctx);

    /**
     * This method is called whenever an entity is to be created.
     * The persistence manager is responsible for handling the results properly
     * wrt the persistent store.
     *
     * @param m        the create method in the home interface that was
     *                 called
     * @param args     any create parameters
     * @param instance the instance being used for this create call
     * @return The primary key computed by CMP PM or null for BMP
     * @throws Exception
     */
    Object createEntity(Method m, Object[] args, CmpEntityBeanContext instance) throws Exception;

    /**
     * This method is called after the createEntity.
     * The persistence manager is responsible for handling the results properly
     * wrt the persistent store.
     *
     * @param m        the ejbPostCreate method in the bean class that was
     *                 called
     * @param args     any create parameters
     * @param instance the instance being used for this create call
     * @return null
     * @throws Exception
     */
    Object postCreateEntity(Method m, Object[] args, CmpEntityBeanContext instance)
            throws Exception;

    /**
     * This method is called when single entities are to be found. The
     * persistence manager must find out whether the wanted instance is
     * available in the persistence store, if so it returns the primary key of
     * the object.
     *
     * @param finderMethod the find method in the home interface that was
     *                     called
     * @param args         any finder parameters
     * @param instance     the instance to use for the finder call
     * @return a primary key representing the found entity
     * @throws java.rmi.RemoteException thrown if some system exception occurs
     * @throws Exception                thrown if some heuristic problem occurs
     */
    Object findEntity(Method finderMethod, Object[] args, CmpEntityBeanContext instance, final JDBCQueryCommand.EntityProxyFactory factory) throws Exception;

    /**
     * This method is called when collections of entities are to be found. The
     * persistence manager must find out whether the wanted instances are
     * available in the persistence store, and if so  it must return a
     * collection of primaryKeys.
     *
     * @param finderMethod the find method in the home interface that was
     *                     called
     * @param args         any finder parameters
     * @param instance     the instance to use for the finder call
     * @return an primary key collection representing the found
     *         entities
     * @throws java.rmi.RemoteException thrown if some system exception occurs
     * @throws Exception                thrown if some heuristic problem occurs
     */
    Collection findEntities(Method finderMethod, Object[] args, CmpEntityBeanContext instance, final JDBCQueryCommand.EntityProxyFactory factory) throws Exception;

    /**
     * This method is called when an entity shall be activated.
     * <p/>
     * <p>With the PersistenceManager factorization most EJB calls should not
     * exists However this calls permits us to introduce optimizations in
     * the persistence store.  Particularly the context has a
     * "PersistenceContext" that a PersistenceStore can use (JAWS does for
     * smart updates) and this is as good a callback as any other to set it
     * up.
     *
     * @param instance the instance to use for the activation
     * @throws java.rmi.RemoteException thrown if some system exception occurs
     */
    void activateEntity(CmpEntityBeanContext instance)
            throws RemoteException;

    /**
     * This method is called whenever an entity shall be load from the
     * underlying storage. The persistence manager must load the state from
     * the underlying storage and then call ejbLoad on the supplied instance.
     *
     * @param instance the instance to synchronize
     * @throws RemoteException thrown if some system exception occurs
     */
    void loadEntity(CmpEntityBeanContext instance)
            throws RemoteException;

    /**
     * This method is used to determine if an entity should be stored.
     *
     * @param instance the instance to check
     * @return true, if the entity has been modified
     * @throws Exception thrown if some system exception occurs
     */
    boolean isStoreRequired(CmpEntityBeanContext instance) throws Exception;

    /**
     * This method is used to determined whether the instance was modified.
     * NOTE, even if the method returns true the isStoreRequired for this same instance
     * might return false, e.g. a CMR field that doesn't have a foreign key was modified.
     *
     * @param instance
     * @return
     * @throws Exception
     */
    boolean isModified(CmpEntityBeanContext instance) throws Exception;

    /**
     * This method is called whenever an entity shall be stored to the
     * underlying storage. The persistence manager must call ejbStore on the
     * supplied instance and then store the state to the underlying storage.
     *
     * @param instance the instance to synchronize
     * @throws RemoteException thrown if some system exception occurs
     */
    void storeEntity(CmpEntityBeanContext instance)
            throws RemoteException;

    /**
     * This method is called when an entity shall be passivate. The persistence
     * manager must call the ejbPassivate method on the instance.
     * <p/>
     * <p>See the activate discussion for the reason for exposing EJB callback
     * calls to the store.
     *
     * @param instance the instance to passivate
     * @throws RemoteException thrown if some system exception occurs
     */
    void passivateEntity(CmpEntityBeanContext instance)
            throws RemoteException;

    /**
     * This method is called when an entity shall be removed from the
     * underlying storage. The persistence manager must call ejbRemove on the
     * instance and then remove its state from the underlying storage.
     *
     * @param instance the instance to remove
     * @throws RemoteException           thrown if some system exception occurs
     * @throws javax.ejb.RemoveException thrown if the instance could not be removed
     */
    void removeEntity(CmpEntityBeanContext instance)
            throws RemoteException, RemoveException;

    JDBCAbstractEntityBridge getEntityBridge();

    JDBCEntityMetaData getMetaData();

    JDBCTypeFactory getJDBCTypeFactory();

    CmpEntityBeanComponent getComponent();

    Catalog getCatalog();

    CmpConfig getCmpConfig();

    DataSource getDataSource(String name);

    boolean hasCreateTable(String entityName);

    void addCreateTable(String entityName);

    void addExistingTable(String entityName);

    EntityBridgeInvocationHandler getInvocationHandler();

    KeyGeneratorFactory getKeyGeneratorFactory(String name);
}

