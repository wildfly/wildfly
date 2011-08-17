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

package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import java.util.Locale;
import java.util.ResourceBundle;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEFAULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.CORE_THREADS;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.LITE;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.MAX_THREADS;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.PATH;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.RELATIVE_TO;

/**
 * @author Emanuel Muckenhuber
 */
class EJB3SubsystemProviders {
    static final String RESOURCE_NAME = EJB3SubsystemProviders.class.getPackage().getName() + ".LocalDescriptions";

    static final DescriptionProvider SUBSYSTEM = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode subsystem = new ModelNode();
            subsystem.get(DESCRIPTION).set(bundle.getString("ejb3"));
            subsystem.get(HEAD_COMMENT_ALLOWED).set(true);
            subsystem.get(TAIL_COMMENT_ALLOWED).set(true);
            subsystem.get(NAMESPACE).set(EJB3Extension.NAMESPACE_1_1);
            subsystem.get(LITE, TYPE).set(ModelType.BOOLEAN);
            subsystem.get(LITE, DESCRIPTION).set(bundle.getString("ejb3.lite"));
            subsystem.get(LITE, DEFAULT).set(false);
            subsystem.get(LITE, REQUIRED).set(false);

            return subsystem;
        }
    };

    static final DescriptionProvider SUBSYSTEM_ADD = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode op = new ModelNode();
            op.get(OPERATION_NAME).set(ADD);
            op.get(DESCRIPTION).set(bundle.getString("ejb3.add"));
            op.get(LITE, TYPE).set(ModelType.BOOLEAN);
            op.get(LITE, DESCRIPTION).set(bundle.getString("ejb3.lite"));
            op.get(LITE, DEFAULT).set(false);
            op.get(LITE, REQUIRED).set(false);

            return op;
        }
    };

    /**
     * Description provider for the strict-max-pool add operation
     */
    public static DescriptionProvider TIMER_SERVICE_ADD_DESCRIPTION = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode description = new ModelNode();
            // set the description of this operation
            description.get(ModelDescriptionConstants.DESCRIPTION).set(bundle.getString("ejb3.timerservice.add"));

            // setup the "max-pool-size" param description
            description.get(REQUEST_PROPERTIES, MAX_THREADS, DESCRIPTION).set(bundle.getString("ejb3.timerservice.maxThreads"));
            description.get(REQUEST_PROPERTIES, MAX_THREADS, TYPE).set(ModelType.INT);
            description.get(REQUEST_PROPERTIES, MAX_THREADS, REQUIRED).set(false);
            description.get(REQUEST_PROPERTIES, MAX_THREADS, DEFAULT).set(Runtime.getRuntime().availableProcessors());


            description.get(REQUEST_PROPERTIES, CORE_THREADS, DESCRIPTION).set(bundle.getString("ejb3.timerservice.coreThreads"));
            description.get(REQUEST_PROPERTIES, CORE_THREADS, TYPE).set(ModelType.INT);
            description.get(REQUEST_PROPERTIES, CORE_THREADS, REQUIRED).set(false);
            description.get(REQUEST_PROPERTIES, CORE_THREADS, DEFAULT).set(0);

            description.get(REQUEST_PROPERTIES, PATH, DESCRIPTION).set(bundle.getString("ejb3.timerservice.path"));
            description.get(REQUEST_PROPERTIES, PATH, TYPE).set(ModelType.STRING);
            description.get(REQUEST_PROPERTIES, PATH, REQUIRED).set(false);

            description.get(REQUEST_PROPERTIES, RELATIVE_TO, DESCRIPTION).set(bundle.getString("ejb3.timerservice.relativeTo"));
            description.get(REQUEST_PROPERTIES, RELATIVE_TO, TYPE).set(ModelType.STRING);
            description.get(REQUEST_PROPERTIES, RELATIVE_TO, REQUIRED).set(false);

            return description;
        }
    };

    public static DescriptionProvider TIMER_SERVICE_REMOVE_DESCRIPTION = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode description = new ModelNode();
            // setup the description
            description.get(DESCRIPTION).set(bundle.getString("ejb3.timerservice.remove"));
            return description;
        }
    };

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }
}
