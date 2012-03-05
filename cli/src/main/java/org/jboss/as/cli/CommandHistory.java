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
package org.jboss.as.cli;

import java.util.List;

/**
 * Represents the history of commands and operations.
 *
 * @author Alexey Loubyansky
 */
public interface CommandHistory {

    /**
     * Returns the history as a list of strings.
     * @return history as a list of strings.
     */
    List<String> asList();

    /**
     * Returns a boolean indicating whether the history is enabled or not.
     * @return  true in case the history is enabled, false otherwise.
     */
    boolean isUseHistory();

    /**
     * Enables or disables history.
     * @param useHistory true enables history, false disables it.
     */
    void setUseHistory(boolean useHistory);

    /**
     * Clears history.
     */
    void clear();

    /**
     * Sets the maximum length of the history log.
     *
     * @param maxSize  maximum length of the history log
     */
    void setMaxSize(int maxSize);

    /**
     * The maximum length of the history log.
     *
     * @return maximum length of the history log
     */
    int getMaxSize();
}
