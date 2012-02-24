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
package org.jboss.as.cmp.jdbc.bridge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.ejb.EJBLocalObject;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.TransactionRolledbackLocalException;

import static org.jboss.as.cmp.CmpMessages.MESSAGES;
import org.jboss.as.cmp.context.CmpEntityBeanContext;

/**
 * This is the relationship set.  An instance of this class
 * is returned when collection valued cmr field is accessed.
 * See the EJB 2.0 specification for a more detailed description
 * or the responsibilities of this class.
 *
 * @author <a href="mailto:dain@daingroup.com">Dain Sundstrom</a>
 * @version $Revision: 81030 $
 */
public class RelationSet implements Set {
    private JDBCCMRFieldBridge cmrField;
    private CmpEntityBeanContext ctx;
    private List[] setHandle;
    private boolean readOnly;
    private Class relatedLocalInterface;

    //
    // Most of this class is a boring wrapper arround the id set.
    // The only interesting hitch is the setHandle.  This class doesn't
    // have a direct referance to the related id set, it has a referance
    // to a referance to the set. When the transaction is completed the
    // CMR field sets my referance to the set to null, so that I know that
    // this set is no longer valid. See the ejb spec for more info.
    //
    public RelationSet(
            JDBCCMRFieldBridge cmrField,
            CmpEntityBeanContext ctx,
            List[] setHandle,
            boolean readOnly) {

        this.cmrField = cmrField;
        this.ctx = ctx;
        this.setHandle = setHandle;
        this.readOnly = readOnly;
        relatedLocalInterface = cmrField.getRelatedLocalInterface();
    }

    private List getIdList() {
        if (setHandle[0] == null) {
            throw MESSAGES.cmrFieldUsedOutSideOfCreatingTx();
        }
        return setHandle[0];
    }

    public int size() {
        List idList = getIdList();
        return idList.size();
    }

    public boolean isEmpty() {
        List idList = getIdList();
        return idList.isEmpty();
    }

    public boolean add(Object o) {
        if (o == null) {
            throw MESSAGES.addedNullToCmrRelationship();
        }

        checkForPKChange();

        List idList = getIdList();
        if (readOnly) {
            throw MESSAGES.collectionIsReadOnlySnapshot();
        }

        if (cmrField.isReadOnly()) {
            throw MESSAGES.fieldIsReadOnly(cmrField.getFieldName());
        }

        if (!relatedLocalInterface.isInstance(o)) {
            Class[] classes = o.getClass().getInterfaces();
            throw MESSAGES.incorrectInterface( relatedLocalInterface.getName(), Arrays.toString(classes));
        }
        try {
            Object id = getPrimaryKey((EJBLocalObject) o);
            if (idList.contains(id)) {
                return false;
            }
            cmrField.createRelationLinks(ctx, id);
            return true;
        } catch (TransactionRolledbackLocalException e) {
            //handle the case where the entity has already been removed
            //TODO: there is probably a better way to handle this
            throw MESSAGES.instanceAlreadyRemoved(e);
        }
    }

    public boolean addAll(Collection c) {
        if (readOnly) {
            throw MESSAGES.collectionIsReadOnlySnapshot();
        }
        if (cmrField.isReadOnly()) {
            throw MESSAGES.fieldIsReadOnly(cmrField.getFieldName());
        }

        if (c == null) {
            return false;
        }

        boolean isModified = false;

        Iterator iterator = (new HashSet(c)).iterator();
        while (iterator.hasNext()) {
            isModified = add(iterator.next()) || isModified;
        }
        return isModified;
    }

    public boolean remove(Object o) {
        List idList = getIdList();
        if (readOnly) {
            throw MESSAGES.collectionIsReadOnlySnapshot();
        }
        if (cmrField.isReadOnly()) {
            throw MESSAGES.fieldIsReadOnly(cmrField.getFieldName());
        }

        checkForPKChange();

        if (!relatedLocalInterface.isInstance(o)) {
            throw MESSAGES.incorrectInterface(relatedLocalInterface.getName());
        }

        Object id = getPrimaryKey((EJBLocalObject) o);
        if (!idList.contains(id)) {
            return false;
        }
        cmrField.destroyRelationLinks(ctx, id);
        return true;
    }

    public boolean removeAll(Collection c) {
        if (readOnly) {
            throw MESSAGES.collectionIsReadOnlySnapshot();
        }
        if (cmrField.isReadOnly()) {
            throw MESSAGES.fieldIsReadOnly(cmrField.getFieldName());
        }

        if (c == null) {
            return false;
        }

        boolean isModified = false;

        Iterator iterator = (new HashSet(c)).iterator();
        while (iterator.hasNext()) {
            isModified = remove(iterator.next()) || isModified;
        }
        return isModified;
    }

    public void clear() {
        checkForPKChange();

        List idList = getIdList();
        if (readOnly) {
            throw MESSAGES.collectionIsReadOnlySnapshot();
        }
        if (cmrField.isReadOnly()) {
            throw MESSAGES.fieldIsReadOnly(cmrField.getFieldName());
        }

        Iterator iterator = (new ArrayList(idList)).iterator();
        while (iterator.hasNext()) {
            cmrField.destroyRelationLinks(ctx, iterator.next());
        }
    }

    public boolean retainAll(Collection c) {
        List idList = getIdList();
        if (readOnly) {
            throw MESSAGES.collectionIsReadOnlySnapshot();
        }
        if (cmrField.isReadOnly()) {
            throw MESSAGES.fieldIsReadOnly(cmrField.getFieldName());
        }

        checkForPKChange();

        if (c == null) {
            if (idList.size() == 0) {
                return false;
            }
            clear();
            return true;
        }

        // get a set of the argument collection's ids
        List argIds = new ArrayList();
        Iterator iterator = c.iterator();
        while (iterator.hasNext()) {
            EJBLocalObject localObject = (EJBLocalObject) iterator.next();
            Object relatedId = getPrimaryKey(localObject);
            argIds.add(relatedId);
        }

        boolean isModified = false;

        iterator = (new ArrayList(idList)).iterator();
        while (iterator.hasNext()) {
            Object id = iterator.next();
            if (!argIds.contains(id)) {
                cmrField.destroyRelationLinks(ctx, id);
                isModified = true;
            }
        }
        return isModified;
    }

    public boolean contains(Object o) {
        List idList = getIdList();

        if (!relatedLocalInterface.isInstance(o)) {
            throw MESSAGES.incorrectInterface(relatedLocalInterface.getName());
        }

        Object id = getPrimaryKey((EJBLocalObject) o);
        return idList.contains(id);
    }

    public boolean containsAll(Collection c) {
        List idList = getIdList();

        if (c == null) {
            return true;
        }

        // get a set of the argument collection's ids
        List argIds = new ArrayList();
        Iterator iterator = c.iterator();
        while (iterator.hasNext()) {
            EJBLocalObject localObject = (EJBLocalObject) iterator.next();
            Object relatedId = getPrimaryKey(localObject);
            argIds.add(relatedId);
        }

        return idList.containsAll(argIds);
    }

    public Object[] toArray(Object[] a) {
        List idList = getIdList();

        Collection c = cmrField.getRelatedComponent().getEntityLocalCollection(idList);
        return c.toArray(a);
    }

    public Object[] toArray() {
        List idList = getIdList();
        Collection c = cmrField.getRelatedComponent().getEntityLocalCollection(idList);
        return c.toArray();
    }

    // Private

    private static void checkForPKChange() {

    }

    // Inner

    public Iterator iterator() {
        return new Iterator() {
            private final Iterator idIterator = getIdList().iterator();
            private Object currentId;

            public boolean hasNext() {
                verifyIteratorIsValid();

                try {
                    return idIterator.hasNext();
                } catch (ConcurrentModificationException e) {
                    throw MESSAGES.underlyingCollectionModified();
                }
            }

            public Object next() {
                verifyIteratorIsValid();

                try {
                    currentId = idIterator.next();
                    return cmrField.getRelatedComponent().getEJBLocalObject(currentId);
                } catch (ConcurrentModificationException e) {
                    throw MESSAGES.underlyingCollectionModified();
                }
            }

            public void remove() {
                verifyIteratorIsValid();
                if (readOnly) {
                    throw MESSAGES.collectionIsReadOnlySnapshot();
                }
                if (cmrField.isReadOnly()) {
                    throw MESSAGES.fieldIsReadOnly(cmrField.getFieldName());
                }

                checkForPKChange();

                try {
                    idIterator.remove();
                    cmrField.destroyRelationLinks(ctx, currentId, false);
                } catch (ConcurrentModificationException e) {
                    throw MESSAGES.collectionHasBeenModified(e);
                }
            }

            private void verifyIteratorIsValid() {
                if (setHandle[0] == null) {
                    throw MESSAGES.iteratorUsedOutsideOfTx();
                }
            }
        };
    }

    public String toString() {
        return new StringBuffer()
                .append('[')
                .append(cmrField.getEntity().getEntityName())
                .append('.')
                .append(cmrField.getFieldName())
                .append(':')
                .append(getIdList()).append(']')
                .toString();
    }

    private Object getPrimaryKey(EJBLocalObject o) {
        try {
            return o.getPrimaryKey();
        } catch (NoSuchObjectLocalException e) {
            throw MESSAGES.noSuchLocalObject(e);
        }
    }
}
