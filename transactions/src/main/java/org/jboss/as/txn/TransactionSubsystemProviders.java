/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.txn;

import java.util.Locale;

import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.dmr.ModelNode;

/**
 * @author Emanuel Muckenhuber
 * @author Scott Stark (sstark@redhat.com) (C) 2011 Red Hat Inc.
 */
class TransactionSubsystemProviders {


    static final DescriptionProvider SUBSYSTEM = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            return Descriptions.getSubsystem(locale);
        }
    };

    static final DescriptionProvider SUBSYSTEM_ADD = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            return Descriptions.getSubsystemAdd(locale);
        }
    };

    static final DescriptionProvider SUBSYSTEM_REMOVE = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            return Descriptions.getSubsystemRemove(locale);
        }
    };

    static final DescriptionProvider RECOVERY_ENVIRONMENT_DESC = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            return Descriptions.getRecoveryEnvironmentDescription(locale);
        }
    };

    static final DescriptionProvider ADD_RECOVERY_ENVIRONMENT_DESC = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            return Descriptions.getRecoveryEnvironmentAddDescription(locale);
        }
    };

    static final DescriptionProvider REMOVE_RECOVERY_ENVIRONMENT_DESC = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            return Descriptions.getRecoveryEnvironmentRemoveDescription(locale);
        }
    };

    static final DescriptionProvider CORE_ENVIRONMENT_DESC = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            return Descriptions.getCoreEnvironmentDescription(locale);
        }
    };

    static final DescriptionProvider ADD_CORE_ENVIRONMENT_DESC = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            return Descriptions.getCoreEnvironmentAddDescription(locale);
        }
    };

    static final DescriptionProvider REMOVE_CORE_ENVIRONMENT_DESC = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            return Descriptions.getCoreEnvironmentRemoveDescription(locale);
        }
    };

    static final DescriptionProvider COORDINATOR_ENVIRONMENT_DESC = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            return Descriptions.getCoordinatorEnvironmentDescription(locale);
        }
    };

    static final DescriptionProvider ADD_COORDINATOR_ENVIRONMENT_DESC = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            return Descriptions.getCoordinatorEnvironmentAddDescription(locale);
        }
    };

    static final DescriptionProvider REMOVE_COORDINATOR_ENVIRONMENT_DESC = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            return Descriptions.getCoordinatorEnvironmentRemoveDescription(locale);
        }
    };

    static final DescriptionProvider OBJECT_STORE_DESC = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            return Descriptions.getObjectStoreDescription(locale);
        }
    };

    static final DescriptionProvider ADD_OBJECT_STORE_DESC = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            return Descriptions.getObjectStoreAddDescription(locale);
        }
    };

    static final DescriptionProvider REMOVE_OBJECT_STORE_DESC = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            return Descriptions.getObjectStoreRemoveDescription(locale);
        }
    };


}
