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

package org.jboss.as.logging;

/**
 * @author Emanuel Muckenhuber
 */
interface CommonAttributes {

    String ACCEPT ="accept";
    String ALL ="all";
    String ANY ="any";
    String APPEND ="append";
    String ASYNC_HANDLER ="async-handler";
    String AUTOFLUSH ="autoflush";
    String CATEGORY ="category";
    String CHANGE_LEVEL ="change-level";
    String CONSOLE_HANDLER ="console-handler";
    String DENY ="deny";
    String ENCODING ="encoding";
    String FILE ="file";
    String FILE_HANDLER ="file-handler";
    String FILE_NAME ="file-name";
    String FILTER ="filter";
    String FORMATTER ="formatter";
    String HANDLER ="handler";
    String HANDLERS ="handlers";
    String LEVEL ="level";
    String LEVEL_RANGE ="level-range";
    String LOGGER ="logger";
    String MATCH ="match";
    String MAX_BACKUP_INDEX ="max-backup-index";
    String MAX_INCLUSIVE ="max-inclusive";
    String MAX_LEVEL ="max-level";
    String MIN_INCLUSIVE ="min-inclusive";
    String MIN_LEVEL ="min-level";
    String NAME ="name";
    String NEW_LEVEL ="new-level";
    String NOT ="not";
    String OVERFLOW_ACTION ="overflow-action";
    String PATH ="path";
    String PATTERN ="pattern";
    String PATTERN_FORMATTER ="pattern-formatter";
    String PERIODIC_ROTATING_FILE_HANDLER ="periodic-rotating-file-handler";
    String PROPERTIES ="properties";
    String QUEUE_LENGTH ="queue-length";
    String RELATIVE_TO ="relative-to";
    String REPLACE ="replace";
    String REPLACEMENT ="replacement";
    String REPLACE_ALL ="replace-all";
    String ROOT_LOGGER ="root-logger";
    String ROTATE_SIZE ="rotate-size";
    String SIZE_ROTATING_FILE_HANDLER ="size-rotating-file-handler";
    String SUBHANDLERS ="subhandlers";
    String SUFFIX ="suffix";
    String TARGET ="target";
    String USE_PARENT_HANDLERS ="use-parent-handlers";
    String VALUE ="value";

}
