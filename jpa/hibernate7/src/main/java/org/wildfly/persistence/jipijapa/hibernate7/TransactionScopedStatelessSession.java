/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.persistence.jipijapa.hibernate7;

import java.util.List;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.TypedQueryReference;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;
import org.hibernate.CacheMode;
import org.hibernate.Filter;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.SessionFactory;
import org.hibernate.SharedSessionBuilder;
import org.hibernate.SharedStatelessSessionBuilder;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.query.MutationQuery;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaInsert;
import org.jipijapa.plugin.spi.ScopedStatelessSessionSupplier;

/**
 * StatelessSession implementation that delegates calls to another StatelessSession.
 * Which session is delegated too depends on any active transaction, and if there is no
 * active transaction, on the thread. The sessions that are delegated to are created
 * by the instance of this class. Closing them is controlled by associated transaction, or
 * for instances not associated with transactions, by the {@code NoTxEmCloser} ThreadLocal-based
 * utility.
 *
 * TODO: a 'DelegatingStatelessSession' in Hibernate would be nice.
 */
public final class TransactionScopedStatelessSession implements StatelessSession {

    private static final long serialVersionUID = 455498112L;


    private final ScopedStatelessSessionSupplier delegateSupplier;

    TransactionScopedStatelessSession(ScopedStatelessSessionSupplier delegateSupplier) {
        this.delegateSupplier = delegateSupplier;
    }

    private StatelessSession getDelegate() {
        return (StatelessSession) delegateSupplier.get();
    }

    /**
     * Catch the application trying to close the container managed entity manager and throw an IllegalStateException
     */
    @Override
    public void close() {
        // Transaction scoped session manager will be closed when the (owning) component invocation completes
        // For call stacks that wrap calls with NoTxEmCloser.pushCall/popCall, the popCall will close the session
        // TODO https://issues.redhat.com/browse/WFLY-21272 what about call stacks not using NoTxSSCloser.pushCall/popCall???
    }

    @Override
    public SharedStatelessSessionBuilder statelessWithOptions() {
        return getDelegate().statelessWithOptions();
    }

    @Override
    public SharedSessionBuilder sessionWithOptions() {
        return getDelegate().sessionWithOptions();
    }

    @Override
    public String getTenantIdentifier() {
        return getDelegate().getTenantIdentifier();
    }

    @Override
    public Object getTenantIdentifierValue() {
        return getDelegate().getTenantIdentifierValue();
    }

    @Override
    public CacheMode getCacheMode() {
        return getDelegate().getCacheMode();
    }

    @Override
    public void setCacheMode(CacheMode cacheMode) {
        getDelegate().setCacheMode(cacheMode);
    }

    @Override
    public boolean isOpen() {
        return getDelegate().isOpen();
    }

    @Override
    public boolean isConnected() {
        return getDelegate().isConnected();
    }

    @Override
    public Transaction beginTransaction() {
        return getDelegate().beginTransaction();
    }

    @Override
    public Transaction getTransaction() {
        return getDelegate().getTransaction();
    }

    @Override
    public void joinTransaction() {
        getDelegate().joinTransaction();
    }

    @Override
    public boolean isJoinedToTransaction() {
        return getDelegate().isJoinedToTransaction();
    }

    @Override
    public ProcedureCall getNamedProcedureCall(String name) {
        return getDelegate().getNamedProcedureCall(name);
    }

    @Override
    public ProcedureCall createStoredProcedureCall(String procedureName) {
        return getDelegate().createStoredProcedureCall(procedureName);
    }

    @Override
    public ProcedureCall createStoredProcedureCall(String procedureName, Class<?>... resultClasses) {
        return getDelegate().createStoredProcedureCall(procedureName, resultClasses);
    }

    @Override
    public ProcedureCall createStoredProcedureCall(String procedureName, String... resultSetMappings) {
        return getDelegate().createStoredProcedureCall(procedureName, resultSetMappings);
    }

    @Override
    public ProcedureCall createNamedStoredProcedureQuery(String name) {
        return getDelegate().createNamedStoredProcedureQuery(name);
    }

    @Override
    public ProcedureCall createStoredProcedureQuery(String procedureName) {
        return getDelegate().createStoredProcedureQuery(procedureName);
    }

    @Override
    public ProcedureCall createStoredProcedureQuery(String procedureName, Class<?>... resultClasses) {
        return getDelegate().createStoredProcedureQuery(procedureName, resultClasses);
    }

    @Override
    public ProcedureCall createStoredProcedureQuery(String procedureName, String... resultSetMappings) {
        return getDelegate().createStoredProcedureQuery(procedureName, resultSetMappings);
    }

    @Override
    public Integer getJdbcBatchSize() {
        return getDelegate().getJdbcBatchSize();
    }

    @Override
    public void setJdbcBatchSize(Integer jdbcBatchSize) {
        getDelegate().setJdbcBatchSize(jdbcBatchSize);
    }

    @Override
    public HibernateCriteriaBuilder getCriteriaBuilder() {
        return getDelegate().getCriteriaBuilder();
    }

    @Override
    public void doWork(Work work) throws HibernateException {
        getDelegate().doWork(work);
    }

    @Override
    public <T> T doReturningWork(ReturningWork<T> returningWork) {
        return getDelegate().doReturningWork(returningWork);
    }

    @Override
    public <T> RootGraph<T> createEntityGraph(Class<T> rootType) {
        return getDelegate().createEntityGraph(rootType);
    }

    @Override
    public RootGraph<?> createEntityGraph(String graphName) {
        return getDelegate().createEntityGraph(graphName);
    }

    @Override
    public <T> RootGraph<T> createEntityGraph(Class<T> rootType, String graphName) {
        return getDelegate().createEntityGraph(rootType, graphName);
    }

    @Override
    public RootGraph<?> getEntityGraph(String graphName) {
        return getDelegate().getEntityGraph(graphName);
    }

    @Override
    public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass) {
        return getDelegate().getEntityGraphs(entityClass);
    }

    @Override
    public Filter enableFilter(String filterName) {
        return getDelegate().enableFilter(filterName);
    }

    @Override
    public Filter getEnabledFilter(String filterName) {
        return getDelegate().getEnabledFilter(filterName);
    }

    @Override
    public void disableFilter(String filterName) {
        getDelegate().disableFilter(filterName);
    }

    @Override
    public SessionFactory getFactory() {
        return getDelegate().getFactory();
    }

    @Override
    public Object insert(Object entity) {
        return getDelegate().insert(entity);
    }

    @Override
    public void insertMultiple(List<?> list) {
        getDelegate().insertMultiple(list);
    }

    @Override
    public Object insert(String entityName, Object entity) {
        return getDelegate().insert(entityName, entity);
    }

    @Override
    public void update(Object entity) {
        getDelegate().update(entity);
    }

    @Override
    public void updateMultiple(List<?> list) {
        getDelegate().updateMultiple(list);
    }

    @Override
    public void update(String entityName, Object entity) {
        getDelegate().update(entityName, entity);
    }

    @Override
    public void delete(Object entity) {
        getDelegate().delete(entity);
    }

    @Override
    public void deleteMultiple(List<?> list) {
        getDelegate().deleteMultiple(list);
    }

    @Override
    public void delete(String entityName, Object entity) {
        getDelegate().delete(entityName, entity);
    }

    @Override
    public void upsert(Object entity) {
        getDelegate().upsert(entity);
    }

    @Override
    public void upsertMultiple(List<?> list) {
        getDelegate().upsertMultiple(list);
    }

    @Override
    public void upsert(String entityName, Object entity) {
        getDelegate().upsert(entityName, entity);
    }

    @Override
    public Object get(String entityName, Object id) {
        return getDelegate().get(entityName, id);
    }

    @Override
    public <T> T get(Class<T> entityClass, Object id) {
        return getDelegate().get(entityClass, id);
    }

    @Override
    public Object get(String entityName, Object id, LockMode lockMode) {
        return getDelegate().get(entityName, id, lockMode);
    }

    @Override
    public <T> T get(Class<T> entityClass, Object id, LockMode lockMode) {
        return getDelegate().get(entityClass, id, lockMode);
    }

    @Override
    public <T> T get(EntityGraph<T> entityGraph, Object o) {
        return getDelegate().get(entityGraph, o);
    }

    @Override
    public <T> T get(EntityGraph<T> entityGraph, Object o, LockMode lockMode) {
        return getDelegate().get(entityGraph, o, lockMode);
    }

    @Override
    public void refresh(Object entity) {
        getDelegate().refresh(entity);
    }

    @Override
    public void refresh(String entityName, Object entity) {
        getDelegate().refresh(entityName, entity);
    }

    @Override
    public void refresh(Object entity, LockMode lockMode) {
        getDelegate().refresh(entity, lockMode);
    }

    @Override
    public void refresh(String entityName, Object entity, LockMode lockMode) {
        getDelegate().refresh(entityName, entity, lockMode);
    }

    @Override
    public void fetch(Object association) {
        getDelegate().fetch(association);
    }

    @Override
    public Object getIdentifier(Object entity) {
        return getDelegate().getIdentifier(entity);
    }

    @Override
    public <T> T get(EntityGraph<T> graph, GraphSemantic graphSemantic, Object id, LockMode lockMode) {
        return getDelegate().get(graph, graphSemantic, id, lockMode);
    }

    @Override
    public <T> List<T> getMultiple(Class<T> aClass, List<?> list) {
        return getDelegate().getMultiple(aClass, list);
    }

    @Override
    public <T> List<T> getMultiple(Class<T> aClass, List<?> list, LockMode lockMode) {
        return getDelegate().getMultiple(aClass, list, lockMode);
    }

    @Override
    public <T> List<T> getMultiple(EntityGraph<T> entityGraph, List<?> list) {
        return getDelegate().getMultiple(entityGraph, list);
    }

    @Override
    public <T> List<T> getMultiple(EntityGraph<T> entityGraph, GraphSemantic graphSemantic, List<?> list) {
        return getDelegate().getMultiple(entityGraph, graphSemantic, list);
    }

    @Override
    public <T> T get(EntityGraph<T> graph, GraphSemantic graphSemantic, Object id) {
        return getDelegate().get(graph, graphSemantic, id);
    }

    @Override
    public Query createQuery(String queryString) {
        return getDelegate().createQuery(queryString);
    }

    @Override
    public <R> Query<R> createQuery(String queryString, Class<R> resultClass) {
        return getDelegate().createQuery(queryString, resultClass);
    }

    @Override
    public <R> Query<R> createQuery(TypedQueryReference<R> typedQueryReference) {
        return getDelegate().createQuery(typedQueryReference);
    }

    @Override
    public NativeQuery createNativeQuery(String sqlString) {
        return getDelegate().createNativeQuery(sqlString);
    }

    @Override
    public <R> NativeQuery<R> createNativeQuery(String sqlString, Class<R> resultClass) {
        return getDelegate().createNativeQuery(sqlString, resultClass);
    }

    @Override
    public <R> NativeQuery<R> createNativeQuery(String sqlString, Class<R> resultClass, String tableAlias) {
        return getDelegate().createNativeQuery(sqlString, resultClass, tableAlias);
    }

    @Override
    public NativeQuery createNativeQuery(String sqlString, String resultSetMappingName) {
        return getDelegate().createNativeQuery(sqlString, resultSetMappingName);
    }

    @Override
    public <R> NativeQuery<R> createNativeQuery(String sqlString, String resultSetMappingName, Class<R> resultClass) {
        return getDelegate().createNativeQuery(sqlString, resultSetMappingName, resultClass);
    }

    @Override
    public SelectionQuery<?> createSelectionQuery(String hqlString) {
        return getDelegate().createSelectionQuery(hqlString);
    }

    @Override
    public <R> SelectionQuery<R> createSelectionQuery(String hqlString, Class<R> resultType) {
        return getDelegate().createSelectionQuery(hqlString, resultType);
    }

    @Override
    public <R> SelectionQuery<R> createSelectionQuery(String s, EntityGraph<R> entityGraph) {
        return getDelegate().createSelectionQuery(s, entityGraph);
    }

    @Override
    public MutationQuery createMutationQuery(String hqlString) {
        return getDelegate().createMutationQuery(hqlString);
    }

    @Override
    public MutationQuery createMutationQuery(JpaCriteriaInsert insertSelect) {
        return getDelegate().createMutationQuery(insertSelect);
    }

    @Override
    public MutationQuery createNativeMutationQuery(String sqlString) {
        return getDelegate().createNativeMutationQuery(sqlString);
    }

    @Override
    public Query createNamedQuery(String name) {
        return getDelegate().createNamedQuery(name);
    }

    @Override
    public <R> Query<R> createNamedQuery(String name, Class<R> resultClass) {
        return getDelegate().createNamedQuery(name, resultClass);
    }

    @Override
    public SelectionQuery<?> createNamedSelectionQuery(String name) {
        return getDelegate().createNamedSelectionQuery(name);
    }

    @Override
    public <R> SelectionQuery<R> createNamedSelectionQuery(String name, Class<R> resultType) {
        return getDelegate().createNamedSelectionQuery(name, resultType);
    }

    @Override
    public MutationQuery createNamedMutationQuery(String name) {
        return getDelegate().createNamedMutationQuery(name);
    }

    @Override
    public Query getNamedQuery(String queryName) {
        return getDelegate().getNamedQuery(queryName);
    }

    @Override
    public NativeQuery getNamedNativeQuery(String name) {
        return getDelegate().getNamedNativeQuery(name);
    }

    @Override
    public NativeQuery getNamedNativeQuery(String name, String resultSetMapping) {
        return getDelegate().getNamedNativeQuery(name, resultSetMapping);
    }

    @Override
    public MutationQuery createMutationQuery(CriteriaDelete deleteQuery) {
        return getDelegate().createMutationQuery(deleteQuery);
    }

    @Override
    public MutationQuery createMutationQuery(CriteriaUpdate updateQuery) {
        return getDelegate().createMutationQuery(updateQuery);
    }

    @Override
    public <R> SelectionQuery<R> createSelectionQuery(CriteriaQuery<R> criteria) {
        return getDelegate().createSelectionQuery(criteria);
    }

    @Override
    public Query createQuery(CriteriaDelete deleteQuery) {
        return getDelegate().createQuery(deleteQuery);
    }

    @Override
    public Query createQuery(CriteriaUpdate updateQuery) {
        return getDelegate().createQuery(updateQuery);
    }

    @Override
    public <R> Query<R> createQuery(CriteriaQuery<R> criteriaQuery) {
        return getDelegate().createQuery(criteriaQuery);
    }
}
