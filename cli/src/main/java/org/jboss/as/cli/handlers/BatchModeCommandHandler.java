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
package org.jboss.as.cli.handlers;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;


/**
 *
 * @author Alexey Loubyansky
 */
public abstract class BatchModeCommandHandler extends BaseOperationCommand {

    public BatchModeCommandHandler(CommandContext ctx, String command, boolean connectionRequired) {
        super(ctx, command, connectionRequired);
    }

    @Override
    public boolean isBatchMode(CommandContext ctx) {
        try {
            if(this.helpArg.isPresent(ctx.getParsedCommandLine())) {
                return false;
            }
        } catch (CommandFormatException e) {
            // this is not nice...
            // but if it failed here it won't be added to the batch,
            // will be executed immediately and will fail with the same exception
            return false;
        }
        return true;
    }
}
