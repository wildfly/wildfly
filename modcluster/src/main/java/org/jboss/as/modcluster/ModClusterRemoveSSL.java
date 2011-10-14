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

package org.jboss.as.web;

import static org.jboss.as.web.Constants.FLAGS;
import static org.jboss.as.web.Constants.PATTERN;
import static org.jboss.as.web.Constants.TEST;

import java.util.Locale;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.dmr.ModelNode;

/**
 * {@code OperationHandler} responsible for defining the accesslog entry.
 *
 * @author Jean-Frederic Clere
 */
class WebReWriteConditionAdd extends AbstractAddStepHandler implements DescriptionProvider {

    static final WebReWriteConditionAdd INSTANCE = new WebReWriteConditionAdd();

    private WebReWriteConditionAdd() {
        //
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return WebSubsystemDescriptions.getReWriteConditionAdd(locale);
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        if (operation.hasDefined(TEST)) model.get(TEST).set(operation.get(TEST));
        if (operation.hasDefined(PATTERN)) model.get(PATTERN).set(operation.get(PATTERN));
        if (operation.hasDefined(FLAGS)) model.get(FLAGS).set(operation.get(FLAGS));
    }
}
