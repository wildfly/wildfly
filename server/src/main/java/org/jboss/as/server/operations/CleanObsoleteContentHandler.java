/*
 * Copyright (C) 2014 Red Hat, inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package org.jboss.as.server.operations;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;

import java.util.Map;
import java.util.Set;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PrimitiveListAttributeDefinition;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.server.controller.descriptions.ServerDescriptions;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Handler to clean obsolete contents from the content repository.
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2014 Red Hat, inc.
 */
public class CleanObsoleteContentHandler implements OperationStepHandler {

    public static final String OPERATION_NAME = "clean-obsolete-content";

    private static final AttributeDefinition MARKED_CONTENT = new PrimitiveListAttributeDefinition.Builder(ContentRepository.MARKED_CONTENT, ModelType.STRING)
            .setAllowNull(true)
            .build();
    private static final AttributeDefinition DELETED_CONTENT = new PrimitiveListAttributeDefinition.Builder(ContentRepository.DELETED_CONTENT, ModelType.STRING)
            .setAllowNull(true)
            .build();

    public static final SimpleOperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(OPERATION_NAME,
                ServerDescriptions.getResourceDescriptionResolver(CONTENT))
                .setRuntimeOnly()
                .setReplyType(ModelType.OBJECT)
                .setReplyParameters(MARKED_CONTENT, DELETED_CONTENT)
                .build();

    private final ContentRepository contentRepository;

    public static final CleanObsoleteContentHandler createOperation(final ContentRepository contentRepository) {
        return new CleanObsoleteContentHandler(contentRepository);
    }

    private CleanObsoleteContentHandler(final ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        context.acquireControllerLock();
        Map<String, Set<String>> obsoleteContents = this.contentRepository.cleanObsoleteContent();
        if (!obsoleteContents.get(ContentRepository.MARKED_CONTENT).isEmpty()) {
            for (String obsoleteContent : obsoleteContents.get(ContentRepository.MARKED_CONTENT)) {
                context.getResult().get(ContentRepository.MARKED_CONTENT).add(obsoleteContent);
            }
        }
        if (!obsoleteContents.get(ContentRepository.DELETED_CONTENT).isEmpty()) {
            for (String obsoleteContent : obsoleteContents.get(ContentRepository.DELETED_CONTENT)) {
                context.getResult().get(ContentRepository.DELETED_CONTENT).add(obsoleteContent);
            }
        }
        context.stepCompleted();
    }

}
