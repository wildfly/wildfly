/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.cli.gui.component;

import javax.swing.JLabel;

/**
 * This is a JLabel whose text will wrap at the given width.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class WordWrapLabel extends JLabel {

    private String text;
    private int width;

    public WordWrapLabel(String text, int width) {
        super(htmlText(text, width));
        this.text = text;
        this.width = width;
    }

    private static String htmlText(String text, int width) {
        return "<html><table><td width='" + width + "'>" + text + "</td></table></html>";
    }

    @Override
    public void setText(String text) {
        this.text = text;
        super.setText(htmlText(this.text, this.width));
    }

    @Override
    public String getText() {
        return this.text;
    }
}
