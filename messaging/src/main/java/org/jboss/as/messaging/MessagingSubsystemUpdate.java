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

package org.jboss.as.messaging;

import org.hornetq.core.config.Configuration;
import org.hornetq.core.server.JournalType;
import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandler;

/**
 * General messaging subsystem update.
 *
 * @author Emanuel Muckenhuber
 */
public class MessagingSubsystemUpdate extends AbstractMessagingSubsystemUpdate<Void> {

    private static final long serialVersionUID = -1306547303259739030L;

    private String bindingsDirectory;
    private String journalDirectory;
    private String largeMessagesDirectory;
    private String pagingDirectory;
    private Boolean clustered;
    private int journalMinFiles = -1;
    private int journalFileSize = -1;
    private JournalType journalType;

    /** {@inheritDoc} */
    AbstractSubsystemUpdate<MessagingSubsystemElement, ?> getCompensatingUpdate(Configuration original) {
        final MessagingSubsystemUpdate update = new MessagingSubsystemUpdate();
        update.setBindingsDirectory(original.getBindingsDirectory());
        update.setClustered(original.isClustered());
        update.setJournalDirectory(original.getJournalDirectory());
        update.setLargeMessagesDirectory(original.getLargeMessagesDirectory());
        update.setPagingDirectory(original.getPagingDirectory());
        update.setJournalFileSize(original.getJournalFileSize());
        update.setJournalMinFiles(original.getJournalMinFiles());
        update.setJournalType(original.getJournalType());
        return update;
    }

    /** {@inheritDoc} */
    void applyUpdate(Configuration configuration) throws UpdateFailedException {
        if(bindingsDirectory != null) configuration.setBindingsDirectory(bindingsDirectory);
        if(journalDirectory != null) configuration.setJournalDirectory(journalDirectory);
        if(largeMessagesDirectory != null) configuration.setLargeMessagesDirectory(largeMessagesDirectory);
        if(pagingDirectory != null) configuration.setPagingDirectory(pagingDirectory);
        if(clustered != null) configuration.setClustered(clustered);
        if(journalMinFiles != -1) configuration.setJournalMinFiles(journalMinFiles);
        if(journalFileSize != -1) configuration.setJournalFileSize(journalFileSize);
        if(journalType != null) configuration.setJournalType(journalType);
    }

    /** {@inheritDoc} */
    protected <P> void applyUpdate(UpdateContext updateContext, UpdateResultHandler<? super Void, P> resultHandler, P param) {
        //
    }

    /** {@inheritDoc} */
    protected void applyUpdateBootAction(UpdateContext updateContext) {
        // TODO register hornetQ service here ?
    }

    public String getBindingsDirectory() {
        return bindingsDirectory;
    }

    public void setBindingsDirectory(String bindingDirectory) {
        this.bindingsDirectory = bindingDirectory;
    }

    public String getJournalDirectory() {
        return journalDirectory;
    }

    public void setJournalDirectory(String journalDirectory) {
        this.journalDirectory = journalDirectory;
    }

    public String getLargeMessagesDirectory() {
        return largeMessagesDirectory;
    }

    public void setLargeMessagesDirectory(String largeMessagesDirectory) {
        this.largeMessagesDirectory = largeMessagesDirectory;
    }

    public String getPagingDirectory() {
        return pagingDirectory;
    }

    public void setPagingDirectory(String pagingDirectory) {
        this.pagingDirectory = pagingDirectory;
    }

    public Boolean getClustered() {
        return clustered;
    }

    public void setClustered(Boolean clustered) {
        this.clustered = clustered;
    }

    public int getJournalMinFiles() {
        return journalMinFiles;
    }

    public void setJournalMinFiles(int journalMinFiles) {
        this.journalMinFiles = journalMinFiles;
    }

    public int getJournalFileSize() {
        return journalFileSize;
    }

    public void setJournalFileSize(int journalFileSize) {
        this.journalFileSize = journalFileSize;
    }

    public JournalType getJournalType() {
        return journalType;
    }

    public void setJournalType(JournalType journalType) {
        this.journalType = journalType;
    }

}
