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
import java.lang.reflect.Modifier;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.sql.DataSource;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import org.jboss.as.cmp.CmpConfig;
import org.jboss.as.cmp.bridge.EntityBridgeInvocationHandler;
import org.jboss.as.cmp.bridge.FieldBridge;
import org.jboss.as.cmp.bridge.SelectorBridge;
import org.jboss.as.cmp.component.CmpEntityBeanComponent;
import org.jboss.as.cmp.context.CmpEntityBeanContext;
import org.jboss.as.cmp.ejbql.Catalog;
import org.jboss.as.cmp.jdbc.bridge.JDBCAbstractEntityBridge;
import org.jboss.as.cmp.jdbc.bridge.JDBCCMPFieldBridge;
import org.jboss.as.cmp.jdbc.bridge.JDBCCMRFieldBridge;
import org.jboss.as.cmp.jdbc.bridge.JDBCEntityBridge;
import org.jboss.as.cmp.jdbc.bridge.JDBCSelectorBridge;
import org.jboss.as.cmp.jdbc.metadata.JDBCEntityMetaData;
import org.jboss.as.cmp.keygenerator.KeyGeneratorFactory;
import org.jboss.as.cmp.keygenerator.KeyGeneratorFactoryRegistry;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.AttachmentList;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.logging.Logger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;
import org.jboss.tm.TransactionLocal;

/**
 * JDBCStoreManager manages storage of persistence data into a table.
 * Other then loading the initial jbosscmp-jdbc.xml file this class
 * does very little. The interesting tasks are performed by the command
 * classes.
 * <p/>
 * Life-cycle:
 * Tied to the life-cycle of the entity container.
 * <p/>
 * Multiplicity:
 * One per cmp entity bean. This could be less if another implementaion of
 * EntityPersistenceStore is created and thoes beans use the implementation
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @author <a href="mailto:alex@jboss.org">Alex Loubyansky</a>
 * @version $Revision: 81030 $
 */
public final class JDBCStoreManager implements JDBCEntityPersistenceStore {
    private Logger log = Logger.getLogger(JDBCStoreManager.class);

    private final DeploymentUnit deploymentUnit;
    private final JDBCEntityMetaData metaData;
    private final CmpConfig cmpConfig;

    private InjectedValue<CmpEntityBeanComponent> component = new InjectedValue<CmpEntityBeanComponent>();

    private JDBCEntityBridge entityBridge;

    private JDBCTypeFactory typeFactory;
    private JDBCQueryManager queryManager;

    private JDBCCommandFactory commandFactory;

    private ReadAheadCache readAheadCache;

    private EntityBridgeInvocationHandler bridgeInvocationHandler;

    private final Map<String, InjectedValue<DataSource>> dataSources = new HashMap<String, InjectedValue<DataSource>>();

    private final InjectedValue<KeyGeneratorFactoryRegistry> keyGeneratorFactoryRegistry = new InjectedValue<KeyGeneratorFactoryRegistry>();

    private final Catalog catalog;

    // Manager life cycle commands
    private JDBCInitCommand initCommand;
    private JDBCStartCommand startCommand;
    private JDBCStopCommand stopCommand;
    private JDBCDestroyCommand destroyCommand;

    // Entity life cycle commands
    private JDBCInitEntityCommand initEntityCommand;
    private JDBCFindEntityCommand findEntityCommand;
    private JDBCFindEntitiesCommand findEntitiesCommand;
    private JDBCCreateCommand createEntityCommand;
    private JDBCPostCreateEntityCommand postCreateEntityCommand;
    private JDBCRemoveEntityCommand removeEntityCommand;
    private JDBCLoadEntityCommand loadEntityCommand;
    private JDBCIsModifiedCommand isModifiedCommand;
    private JDBCStoreEntityCommand storeEntityCommand;
    private JDBCActivateEntityCommand activateEntityCommand;
    private JDBCPassivateEntityCommand passivateEntityCommand;

    // commands
    private JDBCLoadRelationCommand loadRelationCommand;
    private JDBCDeleteRelationsCommand deleteRelationsCommand;
    private JDBCInsertRelationsCommand insertRelationsCommand;

    /**
     * A Transaction manager so that we can link preloaded data to a transaction
     */
    private TransactionManager tm;

    /**
     * Set of EJBLocalObject instances to be cascade-deleted excluding those that should be batch-cascade-deleted.
     */
    private TransactionLocal cascadeDeleteSet;

    public JDBCStoreManager(final DeploymentUnit deploymentUnit, final JDBCEntityMetaData metaData, final CmpConfig cmpConfig, final Catalog catalog) {
        this.deploymentUnit = deploymentUnit;
        this.metaData = metaData;
        this.cmpConfig = cmpConfig;
        this.catalog = catalog;
    }

    void initStoreManager() throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("Initializing CMP plugin for " + metaData.getName());
        }

        // setup the type factory, which is used to map java types to sql types.
        typeFactory = new JDBCTypeFactory(
                metaData.getTypeMapping(),
                metaData.getJDBCApplication().getValueClasses(),
                metaData.getJDBCApplication().getUserTypeMappings()
        );

        // create the bridge between java land and this engine (sql land)
        entityBridge = new JDBCEntityBridge(metaData, this);
        entityBridge.init();
        bridgeInvocationHandler = new EntityBridgeInvocationHandler(createFieldMap(), createSelectorMap());

        getCatalog().addEntity(entityBridge);

        // create the read ahead cache
        readAheadCache = new ReadAheadCache(this);
        readAheadCache.create();

        // Set up Commands
        commandFactory = new JDBCCommandFactory(this);

        // Execute the init command
        initCommand = commandFactory.createInitCommand();
        initCommand.execute();
    }

    /**
     * Brings the store manager into a completely running state.
     * This method will create the database table and compile the queries.
     */
    void startStoreManager() throws Exception {
        tm = getComponent().getTransactionManager();

        cascadeDeleteSet = new TransactionLocal(tm) {
            protected Object initialValue() {
                return new CascadeDeleteRegistry();
            }

            public Transaction getTransaction() {
                try {
                    return transactionManager.getTransaction();
                } catch (SystemException e) {
                    throw new IllegalStateException("An error occured while getting the " +
                            "transaction associated with the current thread: " + e);
                }
            }
        };

        entityBridge.start();

        // Store manager life cycle commands
        startCommand = commandFactory.createStartCommand();
        stopCommand = commandFactory.createStopCommand();
        destroyCommand = commandFactory.createDestroyCommand();

        // Entity commands
        initEntityCommand = commandFactory.createInitEntityCommand();
        findEntityCommand = commandFactory.createFindEntityCommand();
        findEntitiesCommand = commandFactory.createFindEntitiesCommand();
        createEntityCommand = commandFactory.createCreateEntityCommand();
        postCreateEntityCommand = commandFactory.createPostCreateEntityCommand();
        removeEntityCommand = commandFactory.createRemoveEntityCommand();
        loadEntityCommand = commandFactory.createLoadEntityCommand();
        isModifiedCommand = commandFactory.createIsModifiedCommand();
        storeEntityCommand = commandFactory.createStoreEntityCommand();
        activateEntityCommand = commandFactory.createActivateEntityCommand();
        passivateEntityCommand = commandFactory.createPassivateEntityCommand();

        // Relation commands
        loadRelationCommand = commandFactory.createLoadRelationCommand();
        deleteRelationsCommand = commandFactory.createDeleteRelationsCommand();
        insertRelationsCommand = commandFactory.createInsertRelationsCommand();

        // Create the query manager
        queryManager = new JDBCQueryManager(this);

        // Execute the start command, creates the tables
        startCommand.execute();

        // Start the query manager. At this point is creates all of the
        // query commands. The must occure in the start phase, as
        // queries can opperate on other entities in the application, and
        // all entities are gaurenteed to be createed until the start phase.
        queryManager.start();

        readAheadCache.start();
    }

    void resolveRelationships() {
        entityBridge.resolveRelationships();
    }

    void addForeignKeyConstraints() {
        startCommand.addForeignKeyConstraints();
    }

    void stopStoreManager() {
        stopCommand.execute();
        readAheadCache.stop();
    }

    void destroy() {
        // On deploy errors, sometimes CMPStoreManager was never initialized!
        if (destroyCommand != null) {
            destroyCommand.execute();
        }

        if (readAheadCache != null) {
            readAheadCache.destroy();
        }

        readAheadCache = null;
        if (queryManager != null) {
            queryManager.clear();
        }
        queryManager = null;
    }

    public JDBCAbstractEntityBridge getEntityBridge() {
        return entityBridge;
    }

    public JDBCTypeFactory getJDBCTypeFactory() {
        return typeFactory;
    }

    public JDBCEntityMetaData getMetaData() {
        return metaData;
    }

    public JDBCQueryManager getQueryManager() {
        return queryManager;
    }

    public JDBCCommandFactory getCommandFactory() {
        return commandFactory;
    }

    public ReadAheadCache getReadAheadCache() {
        return readAheadCache;
    }

    public static final AttachmentKey<Object> TX_DATA_MAP = AttachmentKey.create(Object.class);

    private Map<Object, Object> getApplicationTxDataMap() {
        Map<Transaction, Map<Object, Object>> txDataMap = (Map<Transaction, Map<Object, Object>>) deploymentUnit.getAttachment(TX_DATA_MAP);
        if (txDataMap == null) {
            txDataMap = new HashMap<Transaction, Map<Object, Object>>();
            deploymentUnit.putAttachment(TX_DATA_MAP, txDataMap);
        }

        try {
            Transaction tx = tm.getTransaction();
            if (tx == null) {
                return null;
            }

            // get the txDataMap from the txMap
            Map<Object, Object> txMap = txDataMap.get(tx);

            // do we have an existing map
            if (txMap == null) {
                int status = tx.getStatus();
                if (status == Status.STATUS_ACTIVE || status == Status.STATUS_PREPARING) {
                    // create and add the new map
                    txMap = new HashMap<Object, Object>();
                    txDataMap.put(tx, txMap);
                }
            }
            return txMap;
        } catch (EJBException e) {
            throw e;
        } catch (Exception e) {
            throw new EJBException("Error getting application tx data map.", e);
        }
    }


    /**
     * Schedules instances for cascade-delete
     */
    public void scheduleCascadeDelete(List pks) {
        CascadeDeleteRegistry registry = (CascadeDeleteRegistry) cascadeDeleteSet.get();
        registry.scheduleAll(pks);
    }

    /**
     * Unschedules instance cascade delete.
     *
     * @param pk instance primary key.
     * @return true if the instance was scheduled for cascade deleted.
     */
    public boolean unscheduledCascadeDelete(Object pk) {
        CascadeDeleteRegistry registry = (CascadeDeleteRegistry) cascadeDeleteSet.get();
        return registry.unschedule(pk);
    }

    public Object getApplicationTxData(Object key) {
        Map<Object, Object> map = getApplicationTxDataMap();
        if (map != null) {
            return map.get(key);
        }
        return null;
    }

    public void putApplicationTxData(Object key, Object value) {
        Map<Object, Object> map = getApplicationTxDataMap();
        if (map != null) {
            map.put(key, value);
        }
    }

    private Map getEntityTxDataMap() {
        Map entityTxDataMap = (Map) getApplicationTxData(this);
        if (entityTxDataMap == null) {
            entityTxDataMap = new HashMap();
            putApplicationTxData(this, entityTxDataMap);
        }
        return entityTxDataMap;
    }

    public Object getEntityTxData(Object key) {
        return getEntityTxDataMap().get(key);
    }

    public void putEntityTxData(Object key, Object value) {
        getEntityTxDataMap().put(key, value);
    }

    public void removeEntityTxData(Object key) {
        getEntityTxDataMap().remove(key);
    }


    public CmpEntityBeanComponent getComponent() {
        return component.getValue();
    }

    public Injector<CmpEntityBeanComponent> getComponentInjector() {
        return component;
    }

    //
    // EJB Life Cycle Commands
    //

    /**
     * Returns a new instance of a class which implemnts the bean class.
     *
     * @return the new instance
     */
    public Object createBeanClassInstance() throws Exception {
        return null; // TODO: jeb - Create a proxy for the component
    }

    public void initEntity(CmpEntityBeanContext ctx) {
        initEntityCommand.execute(ctx);
    }

    public Object createEntity(Method createMethod, Object[] args, CmpEntityBeanContext ctx) throws CreateException {
        Object pk = createEntityCommand.execute(createMethod, args, ctx);
        if (pk == null)
            throw new CreateException("Primary key for created instance is null.");
        return pk;
    }

    public Object postCreateEntity(Method createMethod, Object[] args, CmpEntityBeanContext ctx) {
        return postCreateEntityCommand.execute(createMethod, args, ctx);
    }

    public Object findEntity(Method finderMethod, Object[] args, CmpEntityBeanContext ctx, final JDBCQueryCommand.EntityProxyFactory factory) throws FinderException {
        return findEntityCommand.execute(finderMethod, args, ctx, factory);
    }

    public Collection findEntities(Method finderMethod, Object[] args, CmpEntityBeanContext ctx, final JDBCQueryCommand.EntityProxyFactory factory) throws FinderException {
        return findEntitiesCommand.execute(finderMethod, args, ctx, factory);
    }

    public void activateEntity(CmpEntityBeanContext ctx) {
        activateEntityCommand.execute(ctx);
    }

    /**
     * Loads entity.
     * If entity not found NoSuchEntityException is thrown.
     *
     * @param ctx - entity context.
     */
    public void loadEntity(CmpEntityBeanContext ctx) {
        loadEntity(ctx, true);
    }

    public boolean loadEntity(CmpEntityBeanContext ctx, boolean failIfNotFound) {
        // is any on the data already in the entity valid
        if (!ctx.isValid()) {
            if (log.isTraceEnabled()) {
                log.trace("RESET PERSISTENCE CONTEXT: id=" + ctx.getPrimaryKey());
            }
            entityBridge.resetPersistenceContext(ctx);
        }

        // mark the entity as created; if it was loading it was created
        JDBCEntityBridge.setCreated(ctx);

        return loadEntityCommand.execute(ctx, failIfNotFound);
    }

    public void loadField(JDBCCMPFieldBridge field, CmpEntityBeanContext ctx) {
        loadEntityCommand.execute(field, ctx);
    }

    public boolean isStoreRequired(CmpEntityBeanContext ctx) {
        return isModifiedCommand.execute(ctx);
    }

    public boolean isModified(CmpEntityBeanContext ctx) {
        return entityBridge.isModified(ctx);
    }

    public void storeEntity(CmpEntityBeanContext ctx) {
        storeEntityCommand.execute(ctx);
        synchronizeRelationData();
    }

    private void synchronizeRelationData() {
        final JDBCCMRFieldBridge[] cmrFields = (JDBCCMRFieldBridge[]) entityBridge.getCMRFields();
        for (int i = 0; i < cmrFields.length; ++i) {
            final JDBCCMRFieldBridge.RelationDataManager relationManager = cmrFields[i].getRelationDataManager();
            if (relationManager.isDirty()) {
                final RelationData relationData = relationManager.getRelationData();

                deleteRelations(relationData);
                insertRelations(relationData);

                relationData.addedRelations.clear();
                relationData.removedRelations.clear();
                relationData.notRelatedPairs.clear();
            }
        }
    }

    public void passivateEntity(CmpEntityBeanContext ctx) {
        passivateEntityCommand.execute(ctx);
    }

    public void removeEntity(CmpEntityBeanContext ctx) throws RemoveException, RemoteException {
        removeEntityCommand.execute(ctx);
    }

    //
    // Relationship Commands
    //
    public Collection loadRelation(JDBCCMRFieldBridge cmrField, Object pk) {
        return loadRelationCommand.execute(cmrField, pk);
    }

    private void deleteRelations(RelationData relationData) {
        deleteRelationsCommand.execute(relationData);
    }

    private void insertRelations(RelationData relationData) {
        insertRelationsCommand.execute(relationData);
    }

    private final class CascadeDeleteRegistry {
        private Set scheduled;

        public void scheduleAll(List pks) {
            if (scheduled == null) {
                scheduled = new HashSet();
            }
            scheduled.addAll(pks);
        }

        public boolean unschedule(Object pk) {
            return scheduled.remove(pk);
        }
    }

    public DataSource getDataSource(final String name) {
        final Value<DataSource> value = dataSources.get(name);
        if (value == null) {
            throw new IllegalArgumentException("Error: can't find data source: " + name);
        }
        final DataSource dataSource = value.getValue();
        if (dataSource == null) {
            throw new IllegalArgumentException("Error: can't find data source: " + name);
        }
        return dataSource;
    }

    public CmpConfig getCmpConfig() {
        return cmpConfig;
    }

    public Catalog getCatalog() {
        return catalog;
    }

    public static final AttachmentKey<AttachmentList<String>> CREATE_TABLES = AttachmentKey.createList(String.class);

    public boolean hasCreateTable(final String entityName) {
        final List<String> tables = deploymentUnit.getAttachment(CREATE_TABLES);
        return tables != null && tables.contains(entityName);
    }

    public void addCreateTable(String entityName) {
        deploymentUnit.addToAttachmentList(CREATE_TABLES, entityName);
    }

    public static final AttachmentKey<AttachmentList<String>> EXISTING_TABLES = AttachmentKey.createList(String.class);

    public void addExistingTable(final String entityName) {
        deploymentUnit.addToAttachmentList(EXISTING_TABLES, entityName);
    }

    public Injector<DataSource> getDataSourceInjector(final String name) {
        final InjectedValue<DataSource> injector = new InjectedValue<DataSource>();
        dataSources.put(name, injector);
        return injector;
    }

    public EntityBridgeInvocationHandler getInvocationHandler() {
        return bridgeInvocationHandler;
    }

    private Map<String, Method> getAbstractAccessors() {
        Method[] methods = entityBridge.getMetaData().getEntityClass().getMethods();
        Map<String, Method> abstractAccessors = new HashMap<String, Method>(methods.length);

        for (Method method : methods) {
            if (Modifier.isAbstract(method.getModifiers())) {
                String methodName = method.getName();
                if (methodName.startsWith("get") || methodName.startsWith("set")) {
                    abstractAccessors.put(methodName, method);
                }
            }
        }
        return abstractAccessors;
    }

    private Map<String, EntityBridgeInvocationHandler.BridgeInvoker> createFieldMap() {
        Map<String, Method> abstractAccessors = getAbstractAccessors();

        List<FieldBridge> fields = entityBridge.getFields();
        Map<String, EntityBridgeInvocationHandler.BridgeInvoker> map = new HashMap<String, EntityBridgeInvocationHandler.BridgeInvoker>(fields.size() * 2);
        for (FieldBridge field : fields) {
            // get the names
            String fieldName = field.getFieldName();
            String fieldBaseName = Character.toUpperCase(fieldName.charAt(0)) +
                    fieldName.substring(1);
            String getterName = "get" + fieldBaseName;
            String setterName = "set" + fieldBaseName;

            // get the accessor methods
            Method getterMethod = abstractAccessors.get(getterName);
            Method setterMethod = abstractAccessors.get(setterName);

            // getters and setters must come in pairs
            if (getterMethod != null && setterMethod == null) {
                throw new RuntimeException("Getter was found but no setter was found for field " + fieldName + " in entity " + entityBridge.getEntityName());
            } else if (getterMethod == null && setterMethod != null) {
                throw new RuntimeException("Setter was found but no getter was found for field " + fieldName + " in entity " + entityBridge.getEntityName());
            } else if (getterMethod != null && setterMethod != null) {
                // add methods
                map.put(getterMethod.getName(), new EntityBridgeInvocationHandler.FieldGetInvoker(field));
                map.put(setterMethod.getName(), new EntityBridgeInvocationHandler.FieldSetInvoker(field));

                // remove the accessors (they have been used)
                abstractAccessors.remove(getterName);
                abstractAccessors.remove(setterName);
            }
        }
        return Collections.unmodifiableMap(map);
    }

    private Map<Method, EntityBridgeInvocationHandler.BridgeInvoker> createSelectorMap() {
        Collection<JDBCSelectorBridge> selectors = entityBridge.getSelectors();
        Map<Method, EntityBridgeInvocationHandler.BridgeInvoker> map = new HashMap<Method, EntityBridgeInvocationHandler.BridgeInvoker>(selectors.size());
        for (SelectorBridge selector : selectors) {
            map.put(selector.getMethod(), selector);
        }
        return Collections.unmodifiableMap(map);
    }

    public KeyGeneratorFactory getKeyGeneratorFactory(final String name) {
        return keyGeneratorFactoryRegistry.getValue().getFactory(name);
    }

    public Injector<KeyGeneratorFactoryRegistry> getKeyGeneratorFactoryInjector() {
        return keyGeneratorFactoryRegistry;
    }
}
