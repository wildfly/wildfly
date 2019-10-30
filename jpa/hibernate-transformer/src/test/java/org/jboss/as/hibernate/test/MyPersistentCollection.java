/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.hibernate.test;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;

import org.hibernate.HibernateException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.loader.CollectionAliases;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.Type;


public class MyPersistentCollection implements PersistentCollection {

    @Override
    public Object getOwner() {
        return null;
    }

    @Override
    public void setOwner(Object entity) {
    }

    @Override
    public boolean empty() {
        return false;
    }

    @Override
    public void setSnapshot(Serializable key, String role, Serializable snapshot) {
    }

    @Override
    public void postAction() {
    }

    @Override
    public Object getValue() {
        return null;
    }

    @Override
    public void beginRead() {
    }

    @Override
    public boolean endRead() {
        return false;
    }

    @Override
    public boolean afterInitialize() {
        return false;
    }

    @Override
    public boolean isDirectlyAccessible() {
        return false;
    }

    @Override
    public boolean unsetSession(SessionImplementor currentSession) {
        return false;
    }

    @Override
    public boolean setCurrentSession(SessionImplementor session) throws HibernateException {
        return false;
    }

    @Override
    public void initializeFromCache(CollectionPersister persister, Serializable disassembled, Object owner) {
    }

    @Override
    public Iterator entries(CollectionPersister persister) {
        return null;
    }

    @Override
    public Object readFrom(ResultSet rs, CollectionPersister role, CollectionAliases descriptor, Object owner)
            throws HibernateException, SQLException {
        return null;
    }

    @Override
    public Object getIdentifier(Object entry, int i) {
        return null;
    }

    @Override
    public Object getIndex(Object entry, int i, CollectionPersister persister) {
        return null;
    }

    @Override
    public Object getElement(Object entry) {
        return null;
    }

    @Override
    public Object getSnapshotElement(Object entry, int i) {
        return null;
    }

    @Override
    public void beforeInitialize(CollectionPersister persister, int anticipatedSize) {
    }

    @Override
    public boolean equalsSnapshot(CollectionPersister persister) {
        return false;
    }

    @Override
    public boolean isSnapshotEmpty(Serializable snapshot) {
        return false;
    }

    @Override
    public Serializable disassemble(CollectionPersister persister) {
        return null;
    }

    @Override
    public boolean needsRecreate(CollectionPersister persister) {
        return false;
    }

    @Override
    public Serializable getSnapshot(CollectionPersister persister) {
        return null;
    }

    @Override
    public void forceInitialization() {
    }

    @Override
    public boolean entryExists(Object entry, int i) {
        return false;
    }

    @Override
    public boolean needsInserting(Object entry, int i, Type elemType) {
        return false;
    }

    @Override
    public boolean needsUpdating(Object entry, int i, Type elemType) {
        return false;
    }

    @Override
    public boolean isRowUpdatePossible() {
        return false;
    }

    @Override
    public Iterator getDeletes(CollectionPersister persister, boolean indexIsFormula) {
        return null;
    }

    @Override
    public boolean isWrapper(Object collection) {
        return false;
    }

    @Override
    public boolean wasInitialized() {
        return false;
    }

    @Override
    public boolean hasQueuedOperations() {
        return false;
    }

    @Override
    public Iterator queuedAdditionIterator() {
        return null;
    }

    @Override
    public Collection getQueuedOrphans(String entityName) {
        return null;
    }

    @Override
    public Serializable getKey() {
        return null;
    }

    @Override
    public String getRole() {
        return null;
    }

    @Override
    public boolean isUnreferenced() {
        return false;
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public void clearDirty() {
    }

    @Override
    public Serializable getStoredSnapshot() {
        return null;
    }

    @Override
    public void dirty() {
    }

    @Override
    public void preInsert(CollectionPersister persister) {

    }

    @Override
    public void afterRowInsert(CollectionPersister persister, Object entry, int i) {

    }

    @Override
    public Collection getOrphans(Serializable snapshot, String entityName) {
        return null;
    }
}
