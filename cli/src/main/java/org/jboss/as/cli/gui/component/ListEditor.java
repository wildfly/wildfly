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

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.jboss.dmr.ModelNode;

/**
 * Editor for parameters that are of ModelType.LIST.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2012 Red Hat Inc.
 */
public class ListEditor extends JPanel implements ListSelectionListener {

    private DefaultListModel listModel = new DefaultListModel();
    private JList list = new JList(listModel);

    private Dialog parent;

    private JButton addButton = new JButton("Add...");
    private JButton editButton = new JButton("Edit...");
    private JButton removeButton = new JButton("Remove");
    private JButton moveUpButton = new JButton("\u25B2"); // unicode for solid triangle
    private JButton moveDownButton = new JButton("\u25BC"); // unicode for solid inverted triangle

    public ListEditor(Dialog parent) {
        this.parent = parent;
        list.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setPrototypeCellValue("012345678901234567890123456789"); // about 30 characters wide
        list.addListSelectionListener(this);

        JPanel buttonColumn = makeButtonColumn();

        JScrollPane scroller = new JScrollPane(list);
        JPanel moveUpDownColumn = makeMoveUpDownColumn();


        setLayout(new GridBagLayout());
        GridBagConstraints gbConst = new GridBagConstraints();

        gbConst.gridx = 0;
        gbConst.weightx = 1.0;
        gbConst.weighty = 1.0;
        add(buttonColumn, gbConst);

        add(Box.createHorizontalStrut(5));

        gbConst.fill = GridBagConstraints.BOTH;
        gbConst.gridx = GridBagConstraints.RELATIVE;
        gbConst.weightx = 10.0;
        add(scroller, gbConst);

        add(Box.createHorizontalStrut(5));

        gbConst.fill = GridBagConstraints.NONE;
        gbConst.weightx = 1.0;
        add(moveUpDownColumn, gbConst);
    }

    private JPanel makeButtonColumn() {
        JPanel buttonColumn = new JPanel(new GridLayout(3, 1, 5, 5));

        addButton.setToolTipText("Add an item to the list.");
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                ItemEditor editor = new ItemEditor();
                editor.setVisible(true);
                list.setSelectedIndex(list.getLastVisibleIndex());
            }
        });

        editButton.setToolTipText("Edit selected item.");
        editButton.setEnabled(false);
        editButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                ItemEditor editor = new ItemEditor(list.getSelectedValue().toString());
                editor.setVisible(true);
            }
        });

        removeButton.setToolTipText("Remove selected item.");
        removeButton.setEnabled(false);
        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                listModel.remove(list.getSelectedIndex());
            }
        });

        buttonColumn.add(addButton);
        buttonColumn.add(editButton);
        buttonColumn.add(removeButton);
        return buttonColumn;
    }

    private JPanel makeMoveUpDownColumn() {
        JPanel buttonColumn = new JPanel(new GridLayout(2, 1, 5, 5));

        moveUpButton.setToolTipText("Move selected item up.");
        moveUpButton.setEnabled(false);
        moveUpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                int selectedIndex = list.getSelectedIndex();
                Object toBeMoved = listModel.remove(selectedIndex);
                listModel.add(selectedIndex - 1, toBeMoved);
                list.setSelectedIndex(selectedIndex - 1);
            }
        });

        moveDownButton.setToolTipText("Move selected item down.");
        moveDownButton.setEnabled(false);
        moveDownButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                int selectedIndex = list.getSelectedIndex();
                Object toBeMoved = listModel.remove(selectedIndex);
                listModel.add(selectedIndex + 1, toBeMoved);
                list.setSelectedIndex(selectedIndex + 1);
            }
        });

        buttonColumn.add(moveUpButton);
        buttonColumn.add(moveDownButton);
        return buttonColumn;
    }

    public ModelNode getValue() {
        ModelNode value = new ModelNode();
        for (Enumeration elements = listModel.elements(); elements.hasMoreElements(); ) {
            value.add(elements.nextElement().toString());
        }

        return value;
    }

    public void setValue(ModelNode value) {
        if (!value.isDefined()) return;
        for (ModelNode item : value.asList()) {
            listModel.addElement(item.asString());
        }
    }

    // implement ListSelectionListener
    public void valueChanged(ListSelectionEvent lse) {
        int selectedIndex = list.getSelectedIndex();
        if (selectedIndex == -1) {
            editButton.setEnabled(false);
            removeButton.setEnabled(false);
            moveUpButton.setEnabled(false);
            moveDownButton.setEnabled(false);
            return;
        }

        editButton.setEnabled(true);
        removeButton.setEnabled(true);
        moveUpButton.setEnabled(selectedIndex != 0);
        moveDownButton.setEnabled(selectedIndex != list.getLastVisibleIndex());
    }

    private class ItemEditor extends JDialog {

        private boolean isAddMode = true;
        private JTextField itemField = new JTextField(30);

        public ItemEditor() {
            this("Add Item", "");
        }

        public ItemEditor(String item) {
            this("Edit Item", item);
            isAddMode = false;
        }

        private ItemEditor(String label, String item) {
            super(parent, label, true);
            setLocationRelativeTo(parent);
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            itemField.setText(item);

            Container contentPane = getContentPane();
            contentPane.setLayout(new BorderLayout(10, 10));
            contentPane.add(itemField, BorderLayout.CENTER);
            contentPane.add(makeButtonPanel(), BorderLayout.SOUTH);
            pack();
            setResizable(false);
        }

        private JPanel makeButtonPanel() {
            JPanel buttonPanel = new JPanel();

            JButton ok = new JButton("OK");
            ok.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    String editedItem = itemField.getText();
                    if (isAddMode) {
                        listModel.addElement(editedItem);
                    } else {
                        listModel.set(list.getSelectedIndex(), editedItem);
                    }

                    ItemEditor.this.dispose();
                }
            });

            JButton cancel = new JButton("Cancel");
            cancel.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    ItemEditor.this.dispose();
                }
            });

            buttonPanel.add(ok);
            buttonPanel.add(cancel);
            return buttonPanel;
        }

    }
}
