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
package org.jboss.as.cmp.jdbc2;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.sql.DataSource;
import org.jboss.as.cmp.CmpConfig;
import org.jboss.as.cmp.CmpLogger;
import org.jboss.as.cmp.CmpMessages;
import org.jboss.as.cmp.bridge.EntityBridgeInvocationHandler;
import org.jboss.as.cmp.bridge.FieldBridge;
import org.jboss.as.cmp.component.CmpEntityBeanComponent;
import org.jboss.as.cmp.context.CmpEntityBeanContext;
import org.jboss.as.cmp.ejbql.Catalog;
import org.jboss.as.cmp.jdbc.JDBCEntityPersistenceStore;
import org.jboss.as.cmp.jdbc.JDBCQueryCommand;
import org.jboss.as.cmp.jdbc.JDBCStartCommand;
import org.jboss.as.cmp.jdbc.JDBCStopCommand;
import static org.jboss.as.cmp.jdbc.JDBCStoreManager.CREATE_TABLES;
import static org.jboss.as.cmp.jdbc.JDBCStoreManager.EXISTING_TABLES;
import org.jboss.as.cmp.jdbc.JDBCTypeFactory;
import org.jboss.as.cmp.jdbc.bridge.JDBCAbstractEntityBridge;
import org.jboss.as.cmp.jdbc.metadata.JDBCEntityCommandMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCEntityMetaData;
import org.jboss.as.cmp.jdbc.metadata.JDBCQueryMetaData;
import org.jboss.as.cmp.jdbc2.bridge.EJBSelectBridge;
import org.jboss.as.cmp.jdbc2.bridge.JDBCEntityBridge2;
import org.jboss.as.cmp.jdbc2.schema.EntityTable;
import org.jboss.as.cmp.jdbc2.schema.Schema;
import org.jboss.as.cmp.keygenerator.KeyGeneratorFactory;
import org.jboss.as.cmp.keygenerator.KeyGeneratorFactoryRegistry;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.AttachmentList;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.logging.Logger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.value.InjectedValue;
import org.jboss.tm.TransactionLocal;


/**
 * @author <a href="mailto:alex@jboss.org">Alexey Loubyansky</a>
 * @version <tt>$Revision: 94413 $</tt>
 */
public class JDBCStoreManager2 implements JDBCEntityPersistenceStore {

    private final DeploymentUnit deploymentUnit;
    private CmpEntityBeanComponent component;
    private final JDBCEntityMetaData metaData;
    private final CmpConfig cmpConfig;
    private Logger log;
    private JDBCEntityBridge2 entityBridge;
    private EntityBridgeInvocationHandler bridgeInvocationHandler;
    private JDBCTypeFactory typeFactory;
    private Schema schema;
    private Catalog catalog;

    private final InjectedValue<KeyGeneratorFactoryRegistry> keyGeneratorFactoryRegistry = new InjectedValue<KeyGeneratorFactoryRegistry>();

    private QueryFactory queryFactory;
    private CreateCommand createCmd;
    private JDBCStartCommand startCmd;
    private JDBCStopCommand stop;

    private final TransactionLocal cascadeDeleteRegistry = new TransactionLocal() {
        protected Object initialValue() {
            return new HashMap();
        }
    };

    public JDBCStoreManager2(final DeploymentUnit deploymentUnit, final JDBCEntityMetaData metaData, final CmpConfig cmpConfig, final Catalog catalog) {
        this.deploymentUnit = deploymentUnit;
        this.metaData = metaData;
        this.catalog = catalog;
        this.cmpConfig = cmpConfig;
    }

    // Public
    public static final AttachmentKey<Schema> SCHEMA = AttachmentKey.create(Schema.class);

    public Schema getSchema() {
        schema = deploymentUnit.getAttachment(SCHEMA);
        if (schema == null) {
            schema = new Schema(component.getComponentName());
            deploymentUnit.putAttachment(SCHEMA, schema);
        }
        return schema;
    }

    public Catalog getCatalog() {
        return catalog;
    }

    public QueryFactory getQueryFactory() {
        return queryFactory;
    }

    public boolean registerCascadeDelete(Object key, Object value) {
        Map map = (Map) cascadeDeleteRegistry.get();
        return map.put(key, value) == null;
    }

    public boolean isCascadeDeleted(Object key) {
        Map map = (Map) cascadeDeleteRegistry.get();
        return map.containsKey(key);
    }

    public void unregisterCascadeDelete(Object key) {
        Map map = (Map) cascadeDeleteRegistry.get();
        map.remove(key);
    }

    // Service implementation
    private static final AttachmentKey<AttachmentList<JDBCStoreManager2>> CREATED_MANAGERS = AttachmentKey.createList(JDBCStoreManager2.class);


    public void init(final CmpEntityBeanComponent component) {
        this.component = component;
        deploymentUnit.addToAttachmentList(CREATED_MANAGERS, this);
        try {
            initStoreManager();
        } catch (Exception e) {
            throw CmpMessages.MESSAGES.failedToInitStoreManager(e);
        }
        entityBridge.resolveRelationships();
        try {
            startStoreManager();
        } catch (Exception e) {
            throw CmpMessages.MESSAGES.failedToStartStoreManagerRuntime(e);
        }
        startCmd.addForeignKeyConstraints();
    }

    public void stop() {
        if (stop != null) {
            List<JDBCStoreManager2> managers = deploymentUnit.getAttachment(CREATED_MANAGERS);
            while (managers != null && !managers.isEmpty()) {
                int stoppedInIteration = 0;
                for (Iterator<JDBCStoreManager2> i = managers.iterator(); i.hasNext(); ) {
                    JDBCStoreManager2 manager = i.next();
                    if (manager.stop.execute()) {
                        i.remove();
                        try {
                            manager.entityBridge.stop();
                        } catch (Exception e) {
                            CmpLogger.ROOT_LOGGER.failedToStopEntityBridge(e);

                        }
                        ++stoppedInIteration;
                    }
                }

                if (stoppedInIteration == 0) {
                    break;
                }
            }
        }
    }

    // JDBCEntityPersistenceStore implementation

    public JDBCAbstractEntityBridge getEntityBridge() {
        return entityBridge;
    }

    public JDBCEntityMetaData getMetaData() {
        return metaData;
    }

    public JDBCTypeFactory getJDBCTypeFactory() {
        return typeFactory;
    }

    public CmpEntityBeanComponent getComponent() {
        return component;
    }


    // EntityPersistenceStore implementation

    public Object createBeanClassInstance() throws Exception {
        return null; // TODO: jeb - Create a proxy for the component
    }

    public void initEntity(CmpEntityBeanContext ctx) {
        entityBridge.initPersistenceContext(ctx);
        entityBridge.initInstance(ctx);
    }

    public Object createEntity(Method m, Object[] args, CmpEntityBeanContext ctx)
            throws CreateException {
        return createCmd.execute(m, args, ctx);
    }

    public Object postCreateEntity(Method m, Object[] args, CmpEntityBeanContext ctx) throws CreateException {
        return null;
    }

    public Object findEntity(Method finderMethod,
                             Object[] args,
                             CmpEntityBeanContext instance, final JDBCQueryCommand.EntityProxyFactory factory)
            throws FinderException {
        QueryCommand query = queryFactory.getQueryCommand(finderMethod);
        return query.fetchOne(schema, args, factory);
    }

    public Collection findEntities(Method finderMethod,
                                   Object[] args,
                                   CmpEntityBeanContext instance, final JDBCQueryCommand.EntityProxyFactory factory)
            throws FinderException {
        QueryCommand query = queryFactory.getQueryCommand(finderMethod);
        return query.fetchCollection(schema, args, factory);
    }

    public void activateEntity(CmpEntityBeanContext ctx) {
        entityBridge.initPersistenceContext(ctx);
    }

    public void loadEntity(CmpEntityBeanContext ctx) {
        try {
            EntityTable.Row row = entityBridge.getTable().loadRow(ctx.getPrimaryKey());
            PersistentContext pctx = new PersistentContext(entityBridge, row);
            ctx.setPersistenceContext(pctx);
        } catch (EJBException e) {
            throw e;
        } catch (Exception e) {
            throw CmpMessages.MESSAGES.failedToLoadEntity(entityBridge.getEntityName(), ctx.getPrimaryKey(), e);
        }
    }

    public boolean isStoreRequired(CmpEntityBeanContext instance) {
        return entityBridge.isStoreRequired(instance);
    }

    public boolean isModified(CmpEntityBeanContext instance) throws Exception {
        return entityBridge.isModified(instance);
    }

    public void storeEntity(CmpEntityBeanContext instance) {
        // scary?
    }

    public void passivateEntity(CmpEntityBeanContext ctx) {
        JDBCEntityBridge2.destroyPersistenceContext(ctx);
    }

    public void removeEntity(CmpEntityBeanContext ctx) throws RemoveException {
        entityBridge.remove(ctx);
        PersistentContext pctx = (PersistentContext) ctx.getPersistenceContext();
        pctx.remove();
    }

    // Private

    protected void initStoreManager() {
        if (log.isDebugEnabled()) {
            log.debug("Initializing CMP plugin for " + component.getComponentName());
        }

        // setup the type factory, which is used to map java types to sql types.
        typeFactory = new JDBCTypeFactory(metaData.getTypeMapping(),
                metaData.getJDBCApplication().getValueClasses(),
                metaData.getJDBCApplication().getUserTypeMappings()
        );

        entityBridge = new JDBCEntityBridge2(this, metaData);
        entityBridge.init();

        Catalog catalog = getCatalog();
        catalog.addEntity(entityBridge);

        stop = new JDBCStopCommand(this);
    }

    protected void startStoreManager() throws Exception {
        queryFactory = new QueryFactory(entityBridge);
        queryFactory.init();

        bridgeInvocationHandler = new EntityBridgeInvocationHandler(createFieldMap(entityBridge), createSelectorMap(entityBridge, queryFactory));

        startCmd = new JDBCStartCommand(this);
        startCmd.execute();

        final JDBCEntityCommandMetaData entityCommand = getMetaData().getEntityCommand();
        if (entityCommand == null || "default".equals(entityCommand.getCommandName())) {
            createCmd = new ApplicationPkCreateCommand();
        } else {
            final Class cmdClass = entityCommand.getCommandClass();
            if (cmdClass == null) {
                throw CmpMessages.MESSAGES.entityCommandClassNotSpecified(entityBridge.getEntityName());
            }

            try {
                createCmd = (CreateCommand) cmdClass.newInstance();
            } catch (ClassCastException cce) {
                throw CmpMessages.MESSAGES.entityCommandNotValidClass(cmdClass.getName(), CreateCommand.class.getName());
            }
        }

        createCmd.init(this);
    }

    public CmpConfig getCmpConfig() {
        return cmpConfig;
    }

    public DataSource getDataSource(final String name) {
        return null; // TODO: jeb - inject these as needed
    }

    public boolean hasCreateTable(String entityName) {
        final List<String> tables = deploymentUnit.getAttachment(CREATE_TABLES);
        return tables != null && tables.contains(entityName);
    }

    public void addCreateTable(String entityName) {
        deploymentUnit.addToAttachmentList(CREATE_TABLES, entityName);
    }

    public void addExistingTable(String entityName) {
        deploymentUnit.addToAttachmentList(EXISTING_TABLES, entityName);
    }

    public EntityBridgeInvocationHandler getInvocationHandler() {
        return bridgeInvocationHandler;
    }

    private static Map<String, Method> getAbstractAccessors(Class<?> beanClass) {
        Method[] methods = beanClass.getMethods();
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

    private static Map<String, EntityBridgeInvocationHandler.BridgeInvoker> createFieldMap(JDBCEntityBridge2 entityBridge) {
        Map<String, Method> abstractAccessors = getAbstractAccessors(entityBridge.getMetaData().getEntityClass());

        List<FieldBridge> fields = entityBridge.getFields();
        Map<String, EntityBridgeInvocationHandler.BridgeInvoker> map = new HashMap<String, EntityBridgeInvocationHandler.BridgeInvoker>(fields.size() * 2);
        for (FieldBridge field : fields) {

            // get the names
            String fieldName = field.getFieldName();
            String fieldBaseName = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            String getterName = "get" + fieldBaseName;
            String setterName = "set" + fieldBaseName;

            // get the accessor methods
            Method getterMethod = (Method) abstractAccessors.get(getterName);
            Method setterMethod = (Method) abstractAccessors.get(setterName);

            // getters and setters must come in pairs
            if (getterMethod != null && setterMethod == null) {
                throw CmpMessages.MESSAGES.getterButNoSetterForField(fieldName);
            } else if (getterMethod == null && setterMethod != null) {
                throw CmpMessages.MESSAGES.setterButNoGetterForField(fieldName);
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

    private static Map<Method, EntityBridgeInvocationHandler.BridgeInvoker> createSelectorMap(JDBCEntityBridge2 entityBridge, QueryFactory queryFactory) {
        Collection<JDBCQueryMetaData> queries = entityBridge.getMetaData().getQueries();
        Map<Method, EntityBridgeInvocationHandler.BridgeInvoker> selectorsByMethod = new HashMap<Method, EntityBridgeInvocationHandler.BridgeInvoker>(queries.size());
        for (JDBCQueryMetaData metadata : queries) {
            if (metadata.getMethod().getName().startsWith("ejbSelect")) {
                try {
                    QueryCommand queryCommand = queryFactory.getQueryCommand(metadata.getMethod());
                    Schema schema = ((JDBCStoreManager2) entityBridge.getManager()).getSchema();
                    EJBSelectBridge ejbSelectBridge = new EJBSelectBridge((JDBCStoreManager2) entityBridge.getManager(), entityBridge.getComponent(), schema, metadata, queryCommand);
                    selectorsByMethod.put(metadata.getMethod(), ejbSelectBridge);
                } catch (FinderException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return selectorsByMethod;
    }

    public KeyGeneratorFactory getKeyGeneratorFactory(final String name) {
        return keyGeneratorFactoryRegistry.getValue().getFactory(name);
    }

    public Injector<KeyGeneratorFactoryRegistry> getKeyGeneratorFactoryInjector() {
        return keyGeneratorFactoryRegistry;
    }
}
