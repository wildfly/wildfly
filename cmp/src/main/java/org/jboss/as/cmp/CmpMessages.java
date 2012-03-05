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

package org.jboss.as.cmp;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.ejb.CreateException;
import javax.ejb.DuplicateKeyException;
import javax.ejb.EJBException;
import javax.ejb.FinderException;
import javax.ejb.NoSuchEJBException;
import javax.ejb.NoSuchEntityException;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.ObjectNotFoundException;
import javax.ejb.RemoveException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;

import org.jboss.as.cmp.ejbql.ASTNullComparison;
import org.jboss.as.cmp.ejbql.SimpleNode;
import org.jboss.as.cmp.jdbc.bridge.CMRMessage;
import org.jboss.as.cmp.jdbc.metadata.JDBCQueryMetaData;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.logging.Cause;
import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;
import org.jboss.msc.service.StartException;

/**
 * Date: 05.11.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author John Bailey
 */
@MessageBundle(projectCode = "JBAS")
public interface CmpMessages {

    /**
     * The messages
     */
    CmpMessages MESSAGES = Messages.getBundle(CmpMessages.class);

    @Message(id = 10700, value = "No CMP Store Manager set for component %s")
    IllegalStateException notStoreMangerForComponent(String componentName);

    @Message(id = 10701, value = "Method is not a known CMP field accessor, CMR field accessor, or ejbSelect method: methodName=%s")
    EJBException methodNoCmpAccessor(String methodName);

    @Message(id = 10702, value = "Internal error in invocation bridge")
    EJBException internalInvocationBridgeError(@Cause Exception e);

    @Message(id = 10703, value = "Failed to invoke relationship request")
    EJBException failedToInvokeRelationshipRequest(@Cause Exception e);

    @Message(id = 10704, value = "Missing setComponentInstance method on CmpProxy")
    RuntimeException missingSetComponentInstanceMethodOnCmpProxy();

    @Message(id = 10705, value = "No component instance set on proxy")
    EJBException noComponentInstanceSetOnProxy();

    @Message(id = 10706, value = "No component instance associated with interceptor context")
    IllegalStateException noComponentInstanceAssociated();

    @Message(id = 10707, value = "Failed to create proxy instance for: %s")
    RuntimeException failedToCreateProxyInstance(Class<?> beanClass, @Cause Exception e);

    @Message(id = 10708, value = "Unknown cmp2.0-relationship-message=%s")
    EJBException unknownCmpRelationshipMessage(CMRMessage relationshipMessage);

    @Message(id = 10709, value = "Invocation was not associated with an instance, primary key was null, instance may have been removed")
    NoSuchEJBException primaryKeyNotAssociatedWithInvocation();

    @Message(id = 10710, value = "Failed to get current transaction")
    RuntimeException failedToGetCurrentTransaction(@Cause Exception e);

    @Message(id = 10711, value = "No entity metadata for EntityBean: %s")
    DeploymentUnitProcessingException noEntityMetaDataForEntity(String ejbName);

    @Message(id = 10712, value = "Deployment %s illegally marked as a CMP deployment")
    IllegalStateException invalidCmpDeployment(DeploymentUnit deploymentUnit);

    @Message(id = 10713, value = "Failed to parse '%s'")
    DeploymentUnitProcessingException failedToParse(String file, @Cause Exception e);

    @Message(id = 10714, value = "No transaction sync available")
    IllegalStateException noTransactionSync();

    @Message(id = 10715, value = "Path is collection valued: %s")
    IllegalArgumentException pathIsCollectionValued(String path);

    @Message(id = 10716, value = "ByteArrayBlob is immutable")
    UnsupportedOperationException byteArrayBlobIsImmutable();

    @Message(id = 10717, value = "Unimplemented method")
    UnsupportedOperationException unimplementedMethod();

    @Message(id = 10718, value = "Could not remove %s, no rows affected")
    RemoveException couldNotRemoveEntityNoRows(Object key);

    @Message(id = 10719, value = "Row locking template is not defined for given mapping: %s")
    IllegalStateException rowLockingTemplateNotDefinedFor(String mapping);

    @Message(id = 10720, value = "OFFSET parameter must be an int")
    IllegalStateException offsetParameterMustBeInt();

    @Message(id = 10721, value = "LIMIT parameter must be an int")
    IllegalStateException limitParameterMustBeInt();

    @Message(id = 10722, value = "The function in SELECT clause does not contain a path expression.")
    IllegalStateException noPathExpressionInSelect();

    @Message(id = 10723, value = "Unexpected node in IS NULL clause: %s")
    IllegalStateException unexpectedNodeInNull(ASTNullComparison node);

    @Message(id = 10724, value = "IS EMPTY can be applied only to collection valued CMR field.")
    IllegalStateException isEmptyCanOnlyBeAppliedToCmr();

    @Message(id = 10725, value = "Must not visit ASTIdentifier node.")
    UnsupportedOperationException mustNotVisitIdentifierNode();

    @Message(id = 10726, value = "No security-domain configured but created-by specified")
    RuntimeException noSecurityDomainForCreatedBy();

    @Message(id = 10727, value = "Invalid parameter - missing closing '}' : %s")
    RuntimeException invalidParamMissingCloseParen(String sql);

    @Message(id = 10728, value = "Find failed")
    FinderException findFailed(@Cause Exception e);

    @Message(id = 10729, value = "Failed to read ResultSet.")
    EJBException failedToReadResultSet(@Cause Exception e);

    @Message(id = 10730, value = "Failed to obtain current transaction")
    EJBException failedToObtainCurrentTx(@Cause Exception e);

    @Message(id = 10731, value = "Can't modify collection while the first iterator is not exhausted.")
    IllegalStateException cannotModifyCollectionWhileIteratorNotExhausted();

    @Message(id = 10732, value = "Couldn't create entity command")
    RuntimeException couldNotCreateEntityCommand(@Cause Exception e);

    @Message(id = 10733, value = "Unknown entity: %s")
    RuntimeException unknownEntity(String entityName);

    @Message(id = 10734, value = "Unknown cmp field: %s")
    RuntimeException unknownCmpField(String fieldName);

    @Message(id = 10735, value = "Failed to get query compiler - %s")
    FinderException failedToGetQueryCompiler(Class<?> compiler, @Cause Throwable t);

    @Message(id = 10736, value = "Parameter[%s] is null")
    FinderException nullParameter(int paramIndex);

    @Message(id = 10737, value = "Error compiling ejbql")
    FinderException errorCompilingEjbQl(@Cause Throwable t);

    @Message(id = 10738, value = "path must be a cmr field")
    IllegalArgumentException pathMustBeCmrField();

    @Message(id = 10739, value = "The function in SELECT clause does not contain a path expression.")
    IllegalStateException functionInSelectNoPath();

    @Message(id = 10740, value = "Failed to get left-join nodes")
    IllegalStateException failedToGetLeftJoinNodes(@Cause Exception e);

    @Message(id = 10741, value = "No such entity!")
    ObjectNotFoundException noSuchEntity();

    @Message(id = 10742, value = "More than one entity matches the finder criteria: %s")
    FinderException moreThanOneEntityMatch(Collection<?> results);

    @Message(id = 10743, value = "ResultSet was empty")
    EJBException resultSetEmpty();

    @Message(id = 10744, value = "Error extracting generated key")
    EJBException errorExtractingGeneratedKey(@Cause Exception e);

    @Message(id = 10745, value = "Error checking if entity with primary pk %s exists: SQL returned no rows")
    CreateException errorCheckingEntityExists(Object pk);

    @Message(id = 10746, value = "Entity with primary key %s already exists")
    DuplicateKeyException entityWithPKExists(Object pk);

    @Message(id = 10747, value = "Error checking if entity exists")
    CreateException errorCheckingIfEntityExists(@Cause Exception e);

    @Message(id = 10748, value = "Entity not found: primaryKey=%s")
    NoSuchEntityException entityNotFound(Object pk);

    @Message(id = 10749, value = "Load failed")
    EJBException entityLoadFailed(@Cause Exception e);

    @Message(id = 10750, value = "Load relation failed")
    EJBException loadRelationFailed(@Cause Exception e);

    @Message(id = 10751, value = "ql-compiler is not specified.")
    RuntimeException qlCompilerNotSpecified();

    @Message(id = 10752, value = "Compiler class does not have a constructor which takes %s")
    RuntimeException compilerConstructorMissingArg(String argType);

    @Message(id = 10753, value = "Failed to create an instance of %s")
    RuntimeException failedToCreateCompilerInstance(String compilerName, @Cause Exception e);

    @Message(id = 10754, value = "Unknown query: %s")
    FinderException unknownQueryMethod(Method query);

    @Message(id = 10755, value = "Home interface [%s] does not have a findByPrimaryKey method")
    RuntimeException homeInterfaceNoPKMethod(Class<?> homeClass);

    @Message(id = 10756, value = "Instance was already removed: id=%s")
    IllegalStateException instanceAlreadyRemoved(Object pk);

    @Message(id = 10757, value = "Could not remove %s")
    RemoveException couldNotRemoveEntity(Object pk, @Cause Exception e);

    @Message(id = 10758, value = "Failed to read CLOB character stream")
    SQLException failedToReadClob(@Cause Exception e);

    @Message(id = 10759, value = "Unable to load EJBObject back from Handle")
    SQLException unableToLoadFromHandle(@Cause Exception e);

    @Message(id = 10760, value = "Unable to load to deserialize result")
    SQLException unableToDeserializeResult(@Cause Exception e);

    @Message(id = 10761, value = "Could not suspend current transaction before  alter table.")
    RuntimeException couldNotSuspendAfterAlterTable(@Cause Exception e);

    @Message(id = 10762, value = "Error while alter table %s %s")
    RuntimeException errorAlteringTable(String tableName, String sql, @Cause Exception e);

    @Message(id = 10763, value = "Could not reattach original transaction after alter table")
    RuntimeException couldNotReattachAfterAlterTable(@Cause Exception e);

    @Message(id = 10764, value = "Could not suspend current transaction before creating table.")
    RuntimeException couldNotSuspendBeforeCreateTable(@Cause Exception e);

    @Message(id = 10765, value = "Error while creating table: %s")
    RuntimeException errorCreatingTable(String tableName, @Cause Exception e);

    @Message(id = 10766, value = "Could not reattach original transaction after create table")
    RuntimeException couldNotReattachAfterCreateTable();

    @Message(id = 10767, value = "Could not suspend current transaction before creating index.")
    RuntimeException couldNotSuspendBeforeCreateIndex(@Cause Exception e);

    @Message(id = 10768, value = "Could not create index %s on table %s")
    RuntimeException couldNotCreateIndex(String indexName, String tableName, @Cause Exception e);

    @Message(id = 10769, value = "Could not reattach original transaction after create index")
    RuntimeException couldNotReattachAfterCreateIndex(@Cause Exception e);

    @Message(id = 10770, value = "Could not suspend current transaction before sending sql command.")
    RuntimeException couldNotSuspendBeforeSendingSql(@Cause Exception e);

    @Message(id = 10771, value = "Error while issuing sql in post-table-create")
    RuntimeException errorInPostTableCreate(@Cause Exception e);

    @Message(id = 10772, value = "Could not reattach original transaction after post-table-create")
    RuntimeException couldNotReattachAfterPostTableCreate(@Cause Exception e);

    @Message(id = 10773, value = "auto-increment template not found")
    IllegalStateException autoIncTemplateNotFound();

    @Message(id = 10774, value = "Primary key constraint is not allowed for this type of data store")
    IllegalStateException pkConstraintNotAllowed();

    @Message(id = 10775, value = "Foreign key constraint is not allowed for this type of datastore")
    IllegalStateException fkConstraintNotAllowed();

    @Message(id = 10776, value = "Could not suspend current transaction before alter table create foreign key.")
    RuntimeException couldNotSuspendBeforeFk(@Cause Exception e);

    @Message(id = 10777, value = "Error while adding foreign key constraint for table %s")
    RuntimeException errorAddingFk(String tableName, @Cause Exception e);

    @Message(id = 10778, value = "Store failed")
    EJBException storeFailed(@Cause Exception e);

    @Message(id = 10779, value = "Error getting application tx data map.")
    EJBException errorGettingTxMap(@Cause Exception e);

    @Message(id = 10780, value = "Primary key for created instance is null.")
    CreateException pkIsNullForCreatedInstance();

    @Message(id = 10781, value = "Error: can't find data source: %s")
    IllegalArgumentException canNotFindDataSource(String name);

    @Message(id = 10782, value = "Getter was found but no setter was found for field %s in entity %s")
    RuntimeException getterNotFoundForField(String fieldName, String entityName);

    @Message(id = 10783, value = "Setter was found but no getter was found for field %s in entity %s")
    RuntimeException setterNotFoundForField(String fieldName, String entityName);

    @Message(id = 10784, value = "Failed to start JDBCStore")
    StartException failedToStartJdbcStore(@Cause Exception e);

    @Message(id = 10785, value = "Failed start store manager")
    StartException failedToStateStoreManager(@Cause Exception e);

    @Message(id = 10786, value = "Error getting column value")
    EJBException errorGettingColumnValue(@Cause Exception e);

    @Message(id = 10787, value = "Error setting column value")
    EJBException errorSettingColumnValue(@Cause Exception e);

    @Message(id = 10788, value = "Could not create state factory: %s")
    RuntimeException failedToCreateFieldStateFactory(String implClassName, @Cause Exception e);

    @Message(id = 10789, value = "Failed to load mapped type: %s")
    IllegalStateException failedToLoadMappedType(String mappedType, @Cause Exception e);

    @Message(id = 10790, value = "Failed to create Mapper instance of %s")
    IllegalStateException failedToLoadMapperInstance(String typeMapper, @Cause Throwable t);

    @Message(id = 10791, value = "Failed to create param setter %s")
    IllegalStateException failedToCreateParamSetter(String paramSetter, @Cause Throwable t);

    @Message(id = 10792, value = "Failed to create result reader %s")
    IllegalStateException failedToCreateResultReader(String resultReader, @Cause Throwable t);

    @Message(id = 10793, value = "Could not create mapper: %s")
    RuntimeException couldNotCreateTypeMapper(String typeMapper, @Cause Exception e);

    @Message(id = 10794, value = "Failed to instantiate %s")
    RuntimeException couldNotInstantiateClass(String className, @Cause Exception e);

    @Message(id = 10795, value = "Failed to load class: %s")
    RuntimeException couldNotLoadClass(String className, @Cause Exception e);

    @Message(id = 10796, value = "JDBCSimpleType does not support an index>0.")
    IndexOutOfBoundsException simpleTypeRequiresOneIndex();

    @Message(id = 10797, value = "Cannot get Handle of EJBObject")
    SQLException couldNotGetEjbHandle(@Cause Exception e);

    @Message(id = 10798, value = "Can't serialize binary object")
    SQLException canNotSerializeBinaryObject(@Cause Exception e);

    @Message(id = 10799, value = "Unable to load EJBObject back from Handle")
    SQLException couldNotLoadEjbFromHandle(@Cause Exception e);

    @Message(id = 18500, value = "Unable to load to deserialize result")
    SQLException couldNotDeserializeResult(@Cause Exception e);

    @Message(id = 18501, value = "Failed to read long as String")
    SQLException failedToReadLongString(@Cause Exception e);

    @Message(id = 18502, value = "Failed to read byte array")
    SQLException failedToReadByteArray(@Cause Exception e);

    @Message(id = 18503, value = "Parameter type longbinary not supported")
    UnsupportedOperationException longBinaryNotSupported();

    @Message(id = 18504, value = "Loaded NULL value for a field of a primitive type.")
    IllegalStateException loadedNullFromPrimitive();

    @Message(id = 18505, value = "Parameter string is empty")
    IllegalArgumentException parameterStringIsEmpty();

    @Message(id = 18506, value = "The parameter must begin with a number")
    IllegalArgumentException parameterMustBeginWithNumber();

    @Message(id = 18507, value = "When the parameter is an ejb a field name must be supplied.")
    IllegalArgumentException fieldNameMustBeProvided();

    @Message(id = 18508, value = "The specified field must be a primary key field")
    IllegalArgumentException fieldMustBePrimaryKey();

    @Message(id = 18509, value = "Preloaded value not found")
    IllegalStateException preloadedValueNotFound();

    @Message(id = 18510, value = "list-cache-max is negative: %d")
    IllegalArgumentException listCacheMaxIsNegative(int max);

    @Message(id = 18511, value = "Entity primary key is null")
    IllegalArgumentException entityPrimaryKeyIsNull();

    @Message(id = 18512, value = "Object is null")
    IllegalArgumentException objectIsNull();

    @Message(id = 18513, value = "Error: cmrFields are of wrong type")
    EJBException cmrFieldsWrongType();

    @Message(id = 18514, value = "Error while fixing table name")
    RuntimeException errorFixingTableName(@Cause Exception e);

    @Message(id = 18515, value = "PK and FK have different number of columns")
    IllegalArgumentException pkAndFkWrongNumberOfColumns();

    @Message(id = 18516, value = "Mismatched quote in table name: %s")
    RuntimeException mismatchedQuoteTableName(String tableName);

    @Message(id = 18517, value = "Error while checking if table already exists %s")
    RuntimeException errorCheckingIfTableExists(String tableName, @Cause Exception e);

    @Message(id = 18518, value = "Error while geting column names")
    RuntimeException errorGettingColumnNames(@Cause Exception e);

    @Message(id = 18519, value = "Failed to get datasource connection")
    RuntimeException failedToGetDataSourceConnection(@Cause Exception e);

    @Message(id = 18520, value = "Error while droping table %s")
    RuntimeException errorDroppingTable(String tableName, @Cause Exception e);

    @Message(id = 18521, value = "Field is read-only: fieldName=%s")
    EJBException fieldIsReadOnly(String fieldName);

    @Message(id = 18522, value = "Internal error setting parameters for field %s")
    EJBException errorSettingParameterForField(String fieldName, @Cause Exception e);

    @Message(id = 18523, value = "Internal error getting results for field %s")
    EJBException errorGettingParameterForField(String fieldName, @Cause Exception e);

    @Message(id = 18524, value = "getPrimaryKeyValue not supported")
    UnsupportedOperationException getPrimaryKeyValueNotSupported();

    @Message(id = 18525, value = "CMP 1.1 field not loaded: %s")
    EJBException cmpFieldNotLoaded(String fieldName);

    @Message(id = 18526, value = "Optimistic locking is not supported in CMP1.1.")
    UnsupportedOperationException optimisticLockingNotSupported();

    @Message(id = 18527, value = "Could not load field value: %s")
    EJBException couldNotLoadField(String fieldName);

    @Message(id = 18528, value = "Related CMR field not found in %s  for relationship from %s.%s to %s.%s")
    RuntimeException relatedCmrFieldNotFound(String relatedEntityName, String entityName, String fieldName, String relatedEntityName2, String relatedFieldName);

    @Message(id = 18529, value = "Primary key not found for key-field %s")
    RuntimeException pkNotFoundForKeyField(String fieldName);

    @Message(id = 18530, value = "Primary key %s is not mapped.")
    RuntimeException primaryKeyNotMapped(String fieldName);

    @Message(id = 18531, value = "Error getting instance value")
    EJBException errorGettingInstanceValue(@Cause Exception e);

    @Message(id = 18532, value = "Instance was already removed")
    IllegalArgumentException instanceAlreadyRemoved(@Cause Exception e);

    @Message(id = 18533, value = "Error in scheduleForCascadeDelete")
    EJBException errorInScheduleCascadeDelete(@Cause Exception e);

    @Message(id = 18534, value = "Error in scheduleForBatchCascadeDelete")
    EJBException errorInScheduleBatchCascadeDelete(@Cause Exception e);

    @Message(id = 18535, value = "Error in getRelatedId")
    EJBException errorGetRelatedId(@Cause Exception e);

    @Message(id = 18536, value = "Error in addRelation")
    EJBException errorAddRelation(@Cause Exception e);

    @Message(id = 18537, value = "Error in removeRelation")
    EJBException errorRemoveRelation(@Cause Exception e);

    @Message(id = 18538, value = "getRelatedId may only be called on a cmr-field with a multiplicity of one.")
    EJBException getRelatedOnlyForMuliOfOne();

    @Message(id = 18539, value = "Data contains multiple values, but this cmr field is single valued: %s")
    EJBException singleValuedFieldHasMultipleValues(Collection<?> values);

    @Message(id = 18540, value = "%s CMR field does not have a foreign key to set.")
    EJBException cmrFieldNoForeignKeySet(String fieldName);

    @Message(id = 18541, value = "Internal error setting foreign-key field %s")
    EJBException errorSettingFk(String fieldName, @Cause Exception e);

    @Message(id = 18542, value = "setClean method not supported")
    UnsupportedOperationException setCleanNotSupported();

    @Message(id = 18543, value = "Error getting transaction from the transaction manager")
    EJBException errorGettingTxFromManager(@Cause Exception e);

    @Message(id = 18544, value = "No such object for related FK")
    IllegalArgumentException noSuchRelatedObject(@Cause Exception e);

    @Message(id = 18545, value = "CMR field value not loaded yet")
    EJBException cmrFieldNotLoaded();

    @Message(id = 18546, value = "CMR field value is already loaded")
    EJBException cmrFieldAlreadyLoaded();

    @Message(id = 18547, value = "Error while creating RelationSet")
    EJBException errorCreatingRelationSet(@Cause Exception e);

    @Message(id = 18548, value = "getColumnValue not implemented")
    UnsupportedOperationException getColumnValueNotSupported();

    @Message(id = 18549, value = "setColumnValue not implemented")
    UnsupportedOperationException setColumnValueNotSupported();

    @Message(id = 18550, value = "hasMapper is not implemented.")
    UnsupportedOperationException hasMapperNotSupported();

    @Message(id = 18551, value = "isSearchable is not implemented.")
    UnsupportedOperationException isSearchableNotSupported();

    @Message(id = 18552, value = "getResultSetReaders not implemented")
    UnsupportedOperationException getResultSetReadersNotSupported();

    @Message(id = 18553, value = "getParamSetter not implemented")
    UnsupportedOperationException getParamSetterNotSupported();

    @Message(id = 18554, value = "fieldState is null")
    IllegalArgumentException fieldStateIsNull();

    @Message(id = 18555, value = "getRelationDate not implemented")
    UnsupportedOperationException getRelationDataNotSupported();

    @Message(id = 18556, value = "Error creating primary key instance: ")
    EJBException errorCreatingPKInstance(@Cause Exception e);

    @Message(id = 18557, value = "Load group '%s' is not defined. Defined load groups: %s")
    IllegalStateException loadGroupNotDefined(String groupName, Set<String> loadGroups);

    @Message(id = 18558, value = "Entity state is null.")
    IllegalStateException entityStateIsNull();

    @Message(id = 18559, value = "No such field")
    NoSuchElementException noSuchField();

    @Message(id = 18560, value = "Empty field iterator is immutable")
    UnsupportedOperationException emptyFieldIteratorImmutable();

    @Message(id = 18561, value = "Could not create KeyGenerator instance.")
    RuntimeException failedToInitKeyGenerator(@Cause Exception e);

    @Message(id = 18562, value = "Error in %s")
    EJBException errorInSelector(String selectorName, @Cause Exception e);

    @Message(id = 18563, value = "Single valued selector has no value")
    ObjectNotFoundException noValueForSingleValuedSelector();

    @Message(id = 18564, value = "This collection is a read-only snapshot")
    EJBException collectionIsReadOnlySnapshot();

    @Message(id = 18565, value = "Object must be an instance of %s , but is an instance of [%s]")
    IllegalArgumentException incorrectInterface(String relatedInterfaceName, String interfaces);

    @Message(id = 18566, value = "Underlying collection has been modified")
    IllegalStateException collectionHasBeenModified(@Cause ConcurrentModificationException e);

    @Message(id = 18567, value = "No such local object")
    IllegalArgumentException noSuchLocalObject(@Cause Exception e);

    @Message(id = 18568, value = "Security Context is null")
    IllegalStateException securityContextIsNull();

    @Message(id = 18569, value = "getGeneratedKeys returned an empty ResultSet")
    EJBException getGeneratedKeysEmptyResultSet();

    @Message(id = 18570, value = "identity_val_local() returned an empty ResultSet")
    EJBException identityValLocalReturnedEmptyResultsSet();

    @Message(id = 18571, value = "Error extracting identity_val_local()")
    EJBException errorExtractingIdentityValLocal(@Cause Exception e);

    @Message(id = 18572, value = "Could not load driver class: %s")
    RuntimeException failedToLoadDriverClass(String className, @Cause Exception e);

    @Message(id = 18573, value = "Driver %s does not have method: %s()")
    RuntimeException driverDoesNotHaveMethod(String driverName, String methodName);

    @Message(id = 18574, value = "Could not load org.jboss.resource.adapter.jdbc.StatementAccess")
    RuntimeException couldNotLoadStatementAccess(@Cause Exception e);

    @Message(id = 18575, value = "StatementAccess.getUnderlyingStatement not found")
    RuntimeException getUnderlyingStatementNotFound(@Cause Exception e);

    @Message(id = 18576, value = "key-generator-factory attribute must be set for entity %s")
    RuntimeException entityMustHaveKeyFactory(String entityName);

    @Message(id = 18577, value = "Invalid key generator name; not found: %s")
    IllegalArgumentException invalidKeyFactory(String factoryName);

    @Message(id = 18578, value = "Error: can't create key generator instance; key generator factory: %s")
    RuntimeException errorCreatingKeyFactory(String factoryName, @Cause Exception e);

    @Message(id = 18579, value = "Sequence must be specified")
    RuntimeException sequenceMustBeSpecified();

    @Message(id = 18580, value = "sequence_name attribute must be specified inside <entity-command>")
    RuntimeException sequenceNameRequired();

    @Message(id = 18581, value = "pk-sql attribute must be set for entity %s")
    RuntimeException pkSqlMustBeSet(String entityName);

    @Message(id = 18582, value = "Error fetching next primary key value: result set contains no rows")
    CreateException errorFetchingNextPk();

    @Message(id = 18583, value = "Error fetching the next primary key value:")
    CreateException errorFetchingPkValue(@Cause Exception e);

    @Message(id = 18584, value = "sequence sql returned an empty ResultSet")
    EJBException sequenceSqlReturnedEmptyResultSet();

    @Message(id = 18585, value = "Expected updateCount of 1, got %d")
    EJBException expectedSingleRowButReceivedMore(int rows);

    @Message(id = 18586, value = "Expected ResultSet but got an updateCount. Is NOCOUNT set for all triggers?")
    EJBException expectedResultSetReceivedUpdateCount();

    @Message(id = 18587, value = "primkey-field must be the same type as prim-key-class")
    RuntimeException pkFieldWrongType();

    @Message(id = 18589, value = "Field %s in prim-key-class must be of the same type.")
    RuntimeException pkFieldWrongType(String fieldName);

    @Message(id = 18590, value = "Failed to load entity class")
    RuntimeException failedToLoadEntityClass(@Cause Exception e);

    @Message(id = 18591, value = "Failed to load primary key class")
    RuntimeException failedToLoadPkClass(@Cause Exception e);

    @Message(id = 18592, value = "Failed to load home class")
    RuntimeException failedToLoadHomeClass(@Cause Exception e);

    @Message(id = 18593, value = "Failed to load remote class")
    RuntimeException failedToLoadRemoteClass(@Cause Exception e);

    @Message(id = 18594, value = "Failed to load local home class")
    RuntimeException failedToLoadLocalHomeClass(@Cause Exception e);

    @Message(id = 18595, value = "Failed to load local class")
    RuntimeException failedToLoadLocalClass(@Cause Exception e);

    @Message(id = 18596, value = "Entity must have at least a home or local home: %s")
    RuntimeException entityMustHaveHome(String entityName);

    @Message(id = 18597, value = "Negative value for read ahead " + "list-cache-max '%d'.")
    RuntimeException negativeListCacheMax(int listMax);

    @Message(id = 18598, value = "Negative value for fetch size " + "fetch-size '%d'.")
    RuntimeException negativeFetchSize(int fetchSize);

    @Message(id = 18599, value = "CMP field not found : fieldName=%s, entity=%s")
    RuntimeException cmpFieldNotFound(String fieldName, String entityName);

    @Message(id = 18600, value = "Unknown load group: name=%s")
    RuntimeException unknownLoadGroup(String name);

    @Message(id = 18601, value = "Invalid parameter in function-sql: %s")
    RuntimeException invalidParameterInFunction(String sql);

    @Message(id = 18602, value = "Invalid parameter number in function-sql: number=%d sql=%s")
    RuntimeException invalidParameterNumberInFunction(String number, String sql);

    @Message(id = 18603, value = "Error parsing function-sql: %s")
    RuntimeException errorParsingFunction(String sql);

    @Message(id = 18604, value = "Unexpected error: entity %s has unknown/incorrect optimistic locking configuration. -- %s")
    RuntimeException unknownLockingStrategy(String entityName, String lockingStrategy);

    @Message(id = 18605, value = "Failed to convert method params to class instances: %s")
    RuntimeException failedToConvertMethodParamsToClasses(List<String> methodParams);

    @Message(id = 18606, value = "Error in query specification for method %s")
    RuntimeException errorInQueryForMethod(String methodName);

    @Message(id = 18607, value = "Query method not found: %s(%s)")
    RuntimeException queryMethodNotFound(String methodName, String parameters);

    @Message(id = 18608, value = "Unknown read ahead strategy '%s'")
    RuntimeException unknownReadAheadStrategy(String strategy);

    @Message(id = 18609, value = "Negative value for read ahead page-size '%d'.")
    RuntimeException negativePageSize(int pageSize);

    @Message(id = 18610, value = "Error in jbosscmp-jdbc.xml : datasource-mapping %s not found")
    RuntimeException datasourceMappingNotFound(String dsName);

    @Message(id = 18611, value = "No ejb-relationship-role-name element found")
    IllegalArgumentException noEjbRelationRoleNameElement();

    @Message(id = 18612, value = "An ejb-relationship-role element was not found for role '%s'")
    IllegalArgumentException noEjbRelationshipRole(String roleName);

    @Message(id = 18613, value = "Specified role is not the left or right role. role=%s")
    IllegalArgumentException roleNotLeftOrRightRole(String roleName);

    @Message(id = 18614, value = "%s/%s has batch-cascade-delete in jbosscmp-jdbc.xml but has no cascade-delete in ejb-jar.xml")
    RuntimeException cascadeDeleteInJbossXmlButNoEjbJar(String relationName, String relationshipRoleName);

    @Message(id = 18615, value = "Relationship %s with relation-table-mapping style was setup for batch cascade-delete. Batch cascade-delete supported only for foreign key mapping style.")
    RuntimeException batchCascadeDeleteOnlyForFkMapping(String relationName);

    @Message(id = 18616, value = "Role '%s' on Entity Bean '%s' : CMP field for key not found: field name='%s'")
    RuntimeException cmpFieldNotFoundForRole(String relationshipRoleName, String entityName, String fieldName);

    @Message(id = 18617, value = "Function %s is not defined for %s")
    IllegalStateException functionNotDefinedForType(String functionName, String typeName);

    @Message(id = 18618, value = "Invalid number format in %s - %s")
    IllegalArgumentException invalidNumberFormat(String elementName, String value);

    @Message(id = 18619, value = "Failed to load field type: %s")
    RuntimeException failedToLoadFieldType(String type, @Cause Exception e);

    @Message(id = 18620, value = "Failed to load entity command class: %s")
    RuntimeException failedToLoadEntityCommand(String command, @Cause Exception e);

    @Message(id = 18621, value = "Failed to load compiler implementation: %s")
    RuntimeException failedToLoadCompiler(String qlCompiler, @Cause Exception e);

    @Message(id = 18622, value = "Failed to load value class: %s")
    RuntimeException failedToLoadValueClass(String valueClass, @Cause Exception e);

    @Message(id = 18623, value = "jdbc-type cannot be null")
    IllegalArgumentException jdbcTypeCanNotBeNull();

    @Message(id = 18624, value = "Unable to get next element: ")
    IllegalStateException unableToGetNextElement(@Cause Exception e);

    @Message(id = 18625, value = "Remove not supported")
    UnsupportedOperationException removeNotSupported();

    @Message(id = 18626, value = "More than one instance matches the single-object finder criteria: %s")
    FinderException moreThanOneInstanceForSingleValueFinder(List<?> list);

    @Message(id = 18627, value = "Unique key violation or invalid foreign key value: pk=%s")
    DuplicateKeyException uniqueKeyViolation(Object pk);

    @Message(id = 18628, value = "Invalid parameter - missing closing '}' : %s")
    RuntimeException missingClosingCurlyBrace(String sql);

    @Message(id = 18629, value = "Error compiling JBossQL statement '%s'")
    FinderException errorCompilingJbossQlStatement(Object arg, @Cause Throwable t);

    @Message(id = 18630, value = "Parameter[%d] is null")
    FinderException parameterIsNull(int i);

    @Message(id = 18631, value = "Error compiling EJBQL statement '%s'")
    RuntimeException errorCompilingEjbQlStatement(String statement, @Cause Throwable t);

    @Message(id = 18632, value = "Row locking template is not defined for mapping: %s")
    RuntimeException noRowLockingTemplateForMapping(String typeMapping);

    @Message(id = 18633, value = "Null argument for findByPrimaryKey")
    IllegalArgumentException nullArgumentForFindByPrimaryKey();

    @Message(id = 18634, value = "Instance not found: entity=%s, pk=%s")
    ObjectNotFoundException instanceNotFound(String entityName, Object pk);

    @Message(id = 18635, value = "Error compiling JBossQL statement '%s'")
    RuntimeException errorCompilingJbossQlStatementRuntime(String statement, @Cause Throwable t);

    @Message(id = 18636, value = "Failed to init store manager")
    RuntimeException failedToInitStoreManager(@Cause Exception e);

    @Message(id = 18637, value = "Failed to start store manager")
    RuntimeException failedToStartStoreManagerRuntime(@Cause Exception e);

    @Message(id = 18638, value = "Entity command %s does not implement %s")
    RuntimeException entityCommandNotValidClass(String cmdClass, String commandClassBase);

    @Message(id = 18639, value = "Getter was found but, no setter was found for field: %s")
    RuntimeException getterButNoSetterForField(String fieldName);

    @Message(id = 18640, value = "Setter was found but, no getter was found for field: %s")
    RuntimeException setterButNoGetterForField(String fieldName);

    @Message(id = 18641, value = "Primary key is null!")
    IllegalArgumentException cannotSetNullPk();

    @Message(id = 18642, value = "This entity-command cannot be used with composite primary keys!")
    RuntimeException entityCommandCanNotBeUsedWithCompositePk();

    @Message(id = 18643, value = "pk-sql attribute must be set for entity %s")
    RuntimeException pkSqlAttributeNotSet(String entityName);

    @Message(id = 18644, value = "pk-sql %s returned no results!")
    CreateException pkSqlReturnedNoResults(String pkSql);

    @Message(id = 18645, value = "Unique key violation or invalid foreign key value: pk=%s")
    DuplicateKeyException uniqueKeyViolationInvalidFk(Object pk);

    @Message(id = 18646, value = "Unexpected return type: %d")
    IllegalStateException unexpectedReturnType(byte returnType);

    @Message(id = 18647, value = "Internal error setting instance field %s")
    EJBException errorSettingInstanceField(String fieldName, @Cause Exception e);

    @Message(id = 18648, value = "Complex types are not supported.")
    IllegalStateException complexTypesNotSupported();

    @Message(id = 18649, value = "Internal error getting results for field member %s")
    EJBException errorGettingResultsForField(String fieldName, @Cause Exception e);

    @Message(id = 18650, value = "Method not supported")
    UnsupportedOperationException methodNotSupported();

    @Message(id = 18651, value = "Internal error getting primary key field member %s")
    EJBException errorGettingPk(String fieldName, @Cause Exception e);

    @Message(id = 18652, value = "This implementation does not support null members.")
    IllegalArgumentException nullNumbersNotSupported();

    @Message(id = 18653, value = "Argument must be of type %s")
    IllegalArgumentException invalidArgumentType(String interfaceName);

    @Message(id = 18654, value = "Persistence context is not available! Make sure the CMR collection is accessed in the transaction it was obtained.")
    EJBException persistenceContextNotAvailable();

    @Message(id = 18655, value = "Only version-column optimistic locking strategy is supported at the moment.")
    RuntimeException onlyVersionLockingSupported();

    @Message(id = 18656, value = "Internal error extracting primary key from instance")
    EJBException errorExtractingPk(@Cause Exception e);

    @Message(id = 18657, value = "Field %s not found in entity %s")
    IllegalStateException fieldNotFound(String fieldName, String entityName);

    @Message(id = 18658, value = "Filed to lookup: %s")
    RuntimeException failedToLookupDatasource(String datSourceName, @Cause Exception e);

    @Message(id = 18660, value = "There are still rows to delete!")
    IllegalStateException stillRowsToDelete();

    @Message(id = 18661, value = "row not found: pk=%s")
    IllegalStateException rowNotFound(Object pk);

    @Message(id = 18662, value = "Affected %s rows while expected just one")
    EJBException tooManyRowsAffected(int affected);

    @Message(id = 18663, value = "The row is already deleted: pk=%s")
    IllegalStateException rowAlreadyRemoved(Object pk);

    @Message(id = 18664, value = "Can't update to state: %d")
    IllegalStateException canNotUpdateState(byte state);

    @Message(id = 18665, value = "One of the commands in the batch failed to execute")
    EJBException batchCommandFailedExecute();

    @Message(id = 18666, value = "Each command in the batch should update exactly 1 row but one of the commands updated %d rows.")
    EJBException batchUpdatedTooManyRows(int rows);

    @Message(id = 18667, value = "Expected one updated row but got: %d")
    EJBException expectedOneRow(int rows);

    @Message(id = 18668, value = "Failed to delete many-to-many relationships")
    EJBException failedToDeleteManyToMany(@Cause Exception e);

    @Message(id = 18669, value = "Failed to delete instances")
    EJBException failedToDeleteInstance(@Cause Exception e);

    @Message(id = 18670, value = "Failed to create instances")
    EJBException failedToCreateInstance(@Cause Exception e);

    @Message(id = 18671, value = "Failed to update instances")
    EJBException failedToUpdateInstance(@Cause Exception e);

    @Message(id = 18672, value = "Failed to create many-to-many relationships")
    EJBException failedToCreateManyToMany(@Cause Exception e);

    @Message(id = 18673, value = "The instance is not locked!")
    IllegalStateException instanceIsLocked();

    @Message(id = 18674, value = "removal of %s rejected for %s the entry could not be found")
    org.jboss.as.cmp.jdbc2.schema.Cache.RemoveException removeRejected(Object pk, Transaction tx);

    @Message(id = 18675, value = "removal of %s rejected for %s the entry is locked for update by %s")
    org.jboss.as.cmp.jdbc2.schema.Cache.RemoveException removeRejected(Object pk, Transaction tx, Transaction locker);

    @Message(id = 18676, value = "Failed to suspend current transaction.")
    IllegalStateException failedToSuspendTx(@Cause SystemException e);

    @Message(id = 18677, value = "Failed to begin a new transaction.")
    IllegalStateException failedToBeginTx(@Cause Exception e);

    @Message(id = 18678, value = "Failed to resume transaction")
    IllegalStateException failedToResumeTx(@Cause Exception e);

    @Message(id = 18679, value = "The sequence has not been initialized in the service start phase!")
    IllegalStateException sequenceNotInitialized();

    @Message(id = 18680, value = "Failed to start HiLoKeyGeneratorFactory")
    StartException failedTOStartHiLoKeyGen(@Cause Exception e);

    @Message(id = 18681, value = "Failed to stop HiLoKeyGeneratorFactory")
    IllegalStateException failedTOStopHiLoKeyGen(@Cause Exception e);

    @Message(id = 18682, value = "Failed to create instance: pk=%s")
    CreateException failedToCreateInstance(Object primaryKey, @Cause SQLException e);

    @Message(id = 18683, value = "Entity command is null")
    RuntimeException entityCommandIsNull();

    @Message(id = 18684, value = "Generation only supported with single PK field")
    RuntimeException generationOnlySupportedWithSinglePK();

    @Message(id = 18685, value = "Creation is not allowed because a primary key field is read only.")
    CreateException creationNotAllowedPKReadOnly();

    @Message(id = 18686, value = "An error occured while getting the transaction associated with the current thread")
    IllegalStateException errorGettingCurrentTransaction(@Cause SystemException e);

    @Message(id = 18687, value = "The instance of %s with pk=%s was not stored to prevent potential inconsistency of data in the database:" +
            " the instance was evicted from the cache during the transaction" +
            " and the database was possibly updated by another process.")
    EJBException instanceEvictedBeforeSync(String componentName, Object primaryKey);

    @Message(id = 18688, value = "Exception in store of entity: %s")
    EJBException failedToStoreEntity(String pk, @Cause Exception causeByException);

    @Message(id = 18689, value = "EJB home methods are not allowed to access CMP or CMR fields: methodName= %s")
    EJBException homeMethodsCanNotAccessCmpFields(String methodName);

    @Message(id = 18690, value = "Batch cascade-delete was setup for the role with a foreign key: relationship %s, role %s. " +
            "Batch cascade-delete supported only for roles with no foreign keys.")
    RuntimeException batchCascadeDeleteForRoleWithFk(String relationName, String relationshipRoleName);

    @Message(id = 18691, value = "Internal error: Found unknown node type in EJB-QL abstract syntax tree: node=%s")
    RuntimeException unknownNodeType(SimpleNode node);

    @Message(id = 18692, value = "Member must be if the same type as the collection, got: member=%s, collection=%s")
    IllegalStateException memberMustBeOfCollectionType(String memberName, String collectionEntityName);

    @Message(id = 18693, value = "There should be collection valued path expression, not identification variable.")
    IllegalStateException shouldBeCollectionValuedPathExpression();

    @Message(id = 18694, value = "Only like types can be compared: from CMP field=%s to parameter=%s")
    IllegalStateException onlyLikeTypesCanBeCompared(String fieldType, String parameterType);

    @Message(id = 18695, value = "Can only visit cmp valued path node. Should have been handled at a higher level.")
    IllegalStateException canOnlyVisitCmpNodes();

    @Message(id = 18696, value = "Can not visit multi-column path node. Should have been handled at a higher level.")
    IllegalStateException canNotVisitMultiColumnPath();

    @Message(id = 18697, value = "Can not visit multi-column parameter node. Should have been handled at a higher level.")
    IllegalStateException canNotVisitMultiColumnParameter();

    @Message(id = 18698, value = "Can not visit abstract schema node.  Should have been handled at a higher level.")
    IllegalStateException canNotVisitAbstractNode();

    @Message(id = 18699, value = "Expected one affected row but update returned %s for id=%s")
    CreateException expectedOneRow(int rowsAffected, Object primaryKey);

    @Message(id = 18800, value = "Could not create entity")
    CreateException couldNotCreateEntity(@Cause SQLException e);

    @Message(id = 18801, value = "Path list and field list must have the same size: pathList.size=%s fieldList.size=%s")
    IllegalStateException pathAndFieldListMustBeSameSize(int size, int size1);

    @Message(id = 18802, value = "Failed to execute pk sql")
    CreateException failedToExecutePkSql(@Cause SQLException e);

    @Message(id = 18803, value = "Element %s of list is not an instance of QueryParameter, but %s")
    IllegalArgumentException elementNotQueryParam(int i, String name);

    @Message(id = 18804, value = "Finder %s defined on %s should return only instances of %s but the query results in instances of %s")
    RuntimeException finderReturnedWrongInstance(String finderName, String entityName, String expectedEntity, String actualEntity);

    @Message(id = 18805, value = "cmr-field in left-join was not found: cmr-field=%s, entity=%s")
    RuntimeException cmrFieldInJoinNotFound(String cmrField, String entityName);

    @Message(id = 18806, value = "Unable to access finder implementation: %s")
    FinderException unableToAccessFinder(String name);

    @Message(id = 18807, value = "Illegal arguments for finder implementation: %s")
    FinderException illegalFinderArgument(String name);

    @Message(id = 18808, value = "Error invoking custom finder %s")
    FinderException errorInvokingFinder(String name, @Cause InvocationTargetException e);

    @Message(id = 18809, value = "Could not delete relations from %s")
    EJBException couldNotDeleteRelations(String qualifiedTableName, @Cause Exception e);

    @Message(id = 18810, value = "Error compiling EJB-QL statement for EJB '%s': %s")
    RuntimeException errorCompilingEjbQl(String componentName, String ejbQl, @Cause Throwable t);

    @Message(id = 18811, value = "No finder for this method: %s")
    IllegalArgumentException noFinderForMethod(String finderName);

    @Message(id = 18812, value = "Could insert relations into %s")
    EJBException couldNotInsertRelations(String qualifiedTableName, @Cause Exception e);

    @Message(id = 18813, value = "row-locking is not allowed for this type of datastore")
    IllegalStateException rowLockingNotAllowed();

    @Message(id = 18814, value = "Local home interface does not have the method findByPrimaryKey(%s)")
    RuntimeException localHomeMissingFindByPrimaryKey(String name);

    @Message(id = 18815, value = "Got a %s[cl=%s + interfaces=%s, value=%s] while looking for a %s[cl=%s]")
    SQLException foundWrongClass(String className, ClassLoader cl, Object interfaces, Object value, String destinationName, ClassLoader destinationClassLoader);

    @Message(id = 18816, value = "Primary key constraint is not allowed for this type of data source")
    IllegalStateException pkNotAllowedForDatasource();

    @Message(id = 18817, value = "Update failed. Expected one affected row: rowsAffected=%s, id=%s")
    EJBException updateFailedTooManyRowsAffected(int rowsAffected, Object primaryKey);

    @Message(id = 18818, value = "%s does not have a property named %s")
    EJBException fieldDoesNotHaveProperty(String name, String propertyName);

    @Message(id = 18819, value = "Failed to find a ctor in %s that takes an instance of %s as an argument.")
    IllegalStateException failedToFindConstructor(Class<?> valueType, Class<?> argType);

    @Message(id = 18820, value = "Failed to create an instance of %s with the %s as a ctor argument")
    IllegalStateException failedToConstructWithArgument(Class<?> valueType, Object fieldValue);

    @Message(id = 18821, value = "Property %s in field %s is not a property of value object %s")
    EJBException invalidPropertyValue(String propertyName, String fieldName, String valueType);

    @Message(id = 18822, value = "Circular reference discovered at property: %s")
    EJBException circularReferenceForProperty(String propertyName);

    @Message(id = 18823, value = "Internal error getting instance field %s")
    EJBException errorGettingInstanceField(String fieldName, @Cause Exception e);

    @Message(id = 18824, value = "The parameter index is %s but the query method only has %s parameter(s)")
    IllegalArgumentException invalidParameterInQueryMethod(int argNum, int length);

    @Message(id = 18825, value = "Parameter is NOT a known dependent value class, so a properties cannot supplied.")
    IllegalArgumentException typePropertiesNotAllowed();

    @Message(id = 18826, value = "Parameter is a known dependent value class, so a property must be supplied")
    IllegalArgumentException typePropertyRequired();

    @Message(id = 18827, value = "Expected an instance of EJBObject or EJBLocalObject, but got an instance of %s")
    IllegalArgumentException expectedEjbObject(String type);

    @Message(id = 18828, value = "Entity not found in application catalog with interface=%s")
    IllegalArgumentException entityNotFoundInCatalog(String interfaceName);

    @Message(id = 18829, value = "Error creating theta join clause:pkField.size()=%s fkField.size()=%s")
    IllegalArgumentException errorCreatingJoin(int pkLength, int fkLength);

    @Message(id = 18830, value = "A CMP field that is a member of the primary key can only be set in ejbCreate [EJB 2.0 Spec. 10.3.5].")
    IllegalStateException primaryKeyMembersCanOnlyBeSetInCreate();

    @Message(id = 18831, value = "New value [%s] of a foreign key field %s changed the value of a primary key field %s[%s]")
    IllegalStateException foreignKeyChangedPrimaryKey(Object value, String fkFieldName, String pkFieldName1, Object pkValue);

    @Message(id = 18832, value = "Related entity not found: entity=%s, cmrField=%s, relatedEntity=%s")
    RuntimeException relatedEntityNotFound(String entityName, String fieldName, String relatedEntityName);

    @Message(id = 18833, value = "findByPrimaryKey(%s pk) was not found in %s")
    RuntimeException findByPrimaryKeyNotFound(String keyClass, String homeClass);

    @Message(id = 18834, value = "A CMR field cannot be set in ejbCreate; this should be done in the ejbPostCreate method instead [EJB 2.0 Spec. 10.5.2].")
    IllegalStateException cmrFieldCannotBeSetInEjbCreate();

    @Message(id = 18835, value = "null cannot be assigned to a collection-valued cmr-field [EJB 2.0 Spec. 10.3.8].")
    IllegalArgumentException cmrCollectionFieldsCanNotBeSetToNull();

    @Message(id = 18836, value = "Can't create relationship: CMR field %s.%s has foreign key fields mapped to the primary key columns. Primary key may only be set once in ejbCreate [EJB 2.0 Spec. 10.3.5]. primary key value is %s overriding value is %s")
    IllegalStateException cannotCreateRelationship(String entityName, String fieldName, Object currentValue, Object comingValue);

    @Message(id = 18837, value = "A CMR field cannot be set or added to a relationship in ejbCreate; this should be done in the ejbPostCreate method instead [EJB 2.0 Spec. 10.5.2].")
    IllegalStateException cmrFieldCannotBeSetOrAddedInEjbCreate();

    @Message(id = 18838, value = "The values of this field must be of type %s")
    IllegalArgumentException invalidCmrFieldValue(String type);

    @Message(id = 18839, value = "Only CMR fields that have a foreign-key may be a member of a load group: fieldName=%s")
    RuntimeException onlyCmrFieldsWithFkInLoadGroup(String fieldName);

    @Message(id = 18840, value = "Single valued selector %s returned %s objects")
    FinderException singleValuedSelectorMultipleValues(String selectorName, int size);

    @Message(id = 18841, value = "Returned NULL value from a selector with primitive return type %s.")
    FinderException returnedNullFromPrimitive(String type);

    @Message(id = 18842, value = "A CMR collection may only be used within the transaction in which it was created")
    IllegalStateException cmrFieldUsedOutSideOfCreatingTx();

    @Message(id = 18843, value = "Null cannot be added to a CMR relationship collection")
    IllegalArgumentException addedNullToCmrRelationship();

    @Message(id = 18844, value = "Object must be an instance of %s")
    IllegalArgumentException incorrectInterface(String name);

    @Message(id = 18845, value = "Underlying collection has been modified")
    IllegalStateException underlyingCollectionModified();

    @Message(id = 18846, value = "The iterator of a CMR collection may only be used within the transaction in which it was created")
    IllegalStateException iteratorUsedOutsideOfTx();

    @Message(id = 18847, value = "Configuration found in jbosscmp-jdbc.xml for entity %s but bean is not a jbosscmp-jdbc-managed cmp entity in ejb-jar.xml")
    RuntimeException entityNotFoundInEjbJarXml(String ejbName);

    @Message(id = 18848, value = "Configuration found in jbosscmp-jdbc.xml for relation %s but relation is not a jbosscmp-jdbc-managed relation in ejb-jar.xml")
    RuntimeException relationNotFoundInEjbJarXml(String relationName);

    @Message(id = 18849, value = "No abstract accessors for field named '%s' found in entity class %s")
    RuntimeException noAbstractAccessor(String fieldName, String className);

    @Message(id = 18850, value = "The %s element of declared-sql select is only allowed for ejbSelect queries.")
    RuntimeException declaredSqlElementNotAllowed(String elementName);

    @Message(id = 18851, value = "Dynamic-ql method must have two parameters of type String and Object[].")
    RuntimeException dynamicQlInvalidParameters();

    @Message(id = 18852, value = "Eager load group not found: eager-load-group=%s")
    RuntimeException eagerLoadGroupNotFound(String eagerLoadGroup);

    @Message(id = 18853, value = "type-mapping is not initialized: %s was not deployed or type-mapping was not configured.")
    RuntimeException typeMappingNotInitialized(String dataSourceName);

    @Message(id = 18854, value = "field-type is not allowed for %s column. It is implicitly set to %s.")
    RuntimeException fieldTypeNotAllowedForColumn(String columnName, String implicitType);

    @Message(id = 18855, value = "At least one role of a foreign-key mapped relationship must have key fields " + "(or <primkey-field> is missing from ejb-jar.xml): ejb-relation-name=%s")
    RuntimeException atLeastOneRelationshipRoleMustHaveField(String relationName);

    @Message(id = 18856, value = "Both roles of a relation-table mapped relationship must have key fields: ejb-relation-name=%s")
    RuntimeException bothRolesMustHaveFields(String relationName);

    @Message(id = 18857, value = "Entity: %s not found for relation: %s")
    IllegalArgumentException entityNotFoundForRelation(String ejbName, String ejbRelationName);

    @Message(id = 18858, value = "Unable to find getter for property %s on dependent value class %s")
    RuntimeException getterNotFoundForValue(String propertyName, String className);

    @Message(id = 18859, value = "Unable to find setter for property %s on dependent value class %s")
    RuntimeException setterNotFoundForValue(String propertyName, String className);

    @Message(id = 18860, value = "Can't set collection-valued CMR field to null: %s.%s")
    IllegalArgumentException cannotSetCmrCollectionToNull(String entityName, String fieldName);

    @Message(id = 18861, value = "Role: %s with multiplicity many using foreign-key mapping is not allowed to have key-fields")
    RuntimeException relationshipRoleCanNotHaveKeyFields(String relationshipRoleName);

    @Message(id = 18862, value = "%s/%s has batch-cascade-delete in jbosscmp-jdbc.xml but has no cascade-delete in ejb-jar.xml")
    RuntimeException invalidCascadeDeleteForRelation(String relationName, String relationshipRoleName);

    @Message(id = 18863, value = "Mappings were not provided for all fields: unmapped fields=%s in role=%s")
    RuntimeException mappingsNotProvidedForAllFieldsForRole(Set<String> strings, String relationshipRoleName);

    @Message(id = 18864, value = "Finder failed")
    FinderException finderFailed(@Cause Exception e);

    @Message(id = 18865, value = "Failed to load instance of %s with pk=%s")
    EJBException failedToLoadEntity(String entityName, Object primaryKey, @Cause Exception e);

    @Message(id = 18866, value = "entity-command class name is not specified for entity %s")
    RuntimeException entityCommandClassNotSpecified(String entityName);

    @Message(id = 18867, value = "Home interface %s does not contain findByPrimaryKey(%s)")
    RuntimeException homeInterfaceNoPKMethod(String name, String name1);

    @Message(id = 18868, value = "Local home interface %s does not contain findByPrimaryKey(%s)")
    RuntimeException localHomeInterfaceNoPKMethod(String name, String name1);

    @Message(id = 18869, value = "Unsupported query metadata: method=%s, metadata=%s")
    RuntimeException unsupportedQueryMetadata(String name, JDBCQueryMetaData q);

    @Message(id = 18870, value = "Cannot return null as a value of primitive type %s")
    FinderException cannotReturnNullForPrimitive(String name);

    @Message(id = 18871, value = "Attempt to modify a primary key field through a foreign key field mapped to it: %s.%s -> %s.%s, current value=%s, new value=%s")
    IllegalStateException attemptToModifyPkThroughFk(String entityName, String fieldName, String qualifiedTableName, String columnName, Object curValue, Object value);

    @Message(id = 18872, value = "%s.%s single-valued CMR field is already loaded. Check the database for consistency.  current value=%s, loaded value=%s")
    IllegalStateException singleValuedCmrFieldAlreadyLoaded(String entityName, String fieldName, Object value, Object pk);

    @Message(id = 18873, value = "%s.%s collection-valued CMR field is already loaded. Check the database for consistency.  current value=%s, loaded value=%s")
    IllegalStateException collectionValuedCmrFieldAlreadyLoaded(String entityName, String fieldName, Set value, Object pk);

    @Message(id = 18874, value = "Failed to load related role: ejb-name=%s, cmr-field=%s")
    EJBException failedToLoadRelatedRole(String entityName, String fieldName, @Cause SQLException e);

    @Message(id = 18875, value = "Unexpected row state: table=%s, pk=%s, state=%s")
    IllegalStateException unexpectedRowState(String qualifiedTableName, Object pk, byte state);

    @Message(id = 18876, value = "Failed to load field %s.%s")
    EJBException failedToLoadField(String entityName, String fieldName, @Cause SQLException e);

    @Message(id = 18877, value = "lock acquisition rejected for %s, the entry is locked for update by %s, id=%s")
    Exception lockAcquisitionRejected(Transaction tx, Transaction locker, Object pk);

    @Message(id = 18878, value = "rejected to release lock for %s, the entry is locked for update by %s, id=%s")
    Exception lockReleaseRejected(Transaction tx, Transaction locker, Object pk);

    @Message(id = 18879, value = "Failed to load row: table=%s, pk=%s")
    SQLException failedToLoadRow(String tableName, Object id);

    @Message(id = 18880, value = "Failed to delete view")
    SQLException failedToDeleteView(@Cause SQLException e);

    @Message(id = 18881, value = "Failed to update: table=%s")
    SQLException failedToUpdateTable(String tableName, @Cause SQLException e);

    @Message(id = 18882, value = "Failed to insert new rows")
    SQLException failedToInsertNewRows(@Cause SQLException e);


    @Message(id = 18895, value = "Instance was already removed: id=%s")
    NoSuchObjectLocalException instanceAlreadyRemovedLocal(Object pk);
}
