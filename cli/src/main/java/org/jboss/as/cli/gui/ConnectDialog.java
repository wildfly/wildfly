/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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
package org.jboss.as.cli.gui;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.NORTH;
import static java.awt.BorderLayout.SOUTH;
import static sun.tools.jconsole.Utilities.ensureContrast;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.beans.PropertyVetoException;
import java.net.URL;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.jboss.as.cli.CliInitializationException;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandContextFactory;
import org.jboss.as.cli.CommandLineException;

/**
 * Dialog which is used to connect to server in case local vm has been chosen. In such case there is no
 * JMX Connection open in JConsole so this dialog is used to open dedicated one.
 *
 * @author baranowb
 */
public class ConnectDialog extends JInternalFrame {

    static final String DEFAULT_REMOTE = "http-remoting://localhost:9990"; // TODO - Can this sync up with config somehow?
    // NOTE: CLI has no Message IDs assigned, hence Resources.getText(...);
    // This will probably requirean i18n
    static final String HINT_CONNECT = "<protocol>://<hostname>:<port> OR empty";
    static final String HINT_CONNECT_BUTTON = "Connect to server CLI";
    static final String TEXT_CONNECT = "Connect";
    static final String TEXT_CANCEL = "Cancel";
    static final String TEXT_USERNAME = "Username: ";
    static final String TEXT_PASSWORD = "Password: ";
    static boolean IS_WIN;
    static boolean IS_GTK;
    static {
        String lafName = UIManager.getLookAndFeel().getClass().getName();
        IS_GTK = lafName.equals("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
        IS_WIN = lafName.equals("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
    }
    // private JDesktopPane desktop = new JDesktopPane();

    private JTextField tfUserName, tfPassword;
    private JTextField tfURL;


    private Action actionConnect, actionCancel;

    private Color hintTextColor;
    private JLabel statusBar;
    private final JConsoleCLIPlugin plugin;
    private final JPanel targetDisplay;
    private final JDesktopPane display = new JDesktopPane();
    private boolean started = false;

    public ConnectDialog(final JConsoleCLIPlugin jConsoleCLIPlugin, final JPanel targetDisplay) {
        super();
        this.plugin = jConsoleCLIPlugin;
        this.targetDisplay = targetDisplay;
        this.display.add(this);
        setVisible(false);
        createHelpers();
        createActions();
        createContent();

        this.setSize(this.getPreferredSize());
    }

    public void start() {
        if(started){
            return;
        }
        // to update GUI...
        // SwingUtilities... does not work.
        new Thread(new Runnable() {
            public void run() {
                inner_start();
            }
        }).start();
    }

    private void inner_start(){
        //HACK, location needs to be set twice...
        targetDisplay.setVisible(false);
        targetDisplay.add(display);
        setLocation((display.getWidth() - getWidth()) / 2, (display.getHeight() - getHeight()) / 2);
        tfURL.setText(DEFAULT_REMOTE);

        try {
            // Bring to front of other dialogs
            setSelected(true);
        } catch (PropertyVetoException e) {
        }
        targetDisplay.setVisible(true);
        targetDisplay.revalidate();
        targetDisplay.repaint();
        setVisible(true);
        setLocation((display.getWidth() - getWidth()) / 2, (display.getHeight() - getHeight()) / 2);
        started = true;
    }

    public void stop() {
        this.targetDisplay.remove(this.display);
        this.started = false;
    }

    public boolean isStarted() {
        return this.started;
    }

    private void createHelpers() {
        hintTextColor = ensureContrast(UIManager.getColor("Label.disabledForeground"), UIManager.getColor("Panel.background"));
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setResizable(false);
    }

    private void createActions() {
        actionConnect = new AbstractAction(TEXT_CONNECT) {

            public void actionPerformed(ActionEvent ev) {
                if (!isEnabled() || !isVisible()) {
                    return;
                }

                String controller = null;
                String user = null;
                String password = null;
                if (tfURL.getText().length() > 0) {
                    controller = tfURL.getText();

                    if (tfUserName.getText().length() > 0) {
                        user = tfUserName.getText();
                        password = tfPassword.getText();
                    }
                }

                try {
                    final CommandContext cmdCtx;
                    if (user == null) {
                        cmdCtx = CommandContextFactory.getInstance().newCommandContext();
                    } else {
                        cmdCtx = CommandContextFactory.getInstance().newCommandContext(user, password.toCharArray());
                    }
                    cmdCtx.connectController(controller);
                    plugin.init(cmdCtx);
                } catch (CliInitializationException e) {
                    statusBar.setText(e.getMessage());
                    e.printStackTrace();
                    return;
                } catch (NumberFormatException e) {
                    statusBar.setText(e.getMessage());
                    e.printStackTrace();
                    return;
                } catch (CommandLineException e) {
                    e.printStackTrace();
                    statusBar.setText(e.getMessage());
                    return;
                }
                setVisible(false);
                clearStatus();
            }
        };
        actionCancel = new AbstractAction(TEXT_CANCEL) {

            public void actionPerformed(ActionEvent ev) {
                clearStatus();
                tfURL.setText(DEFAULT_REMOTE);
                tfPassword.setText("");
                tfUserName.setText("");
            }
        };
    }

    private void createContent() {
        Container cp = (JComponent) getContentPane();

        final JPanel urlPanel = new JPanel(new BorderLayout(0, 12));
        urlPanel.setBorder(new EmptyBorder(6, 12, 12, 12));
        final JPanel bottomPanel = new JPanel(new BorderLayout());

        statusBar = new JLabel(" ", JLabel.CENTER);

        final Font normalLabelFont = statusBar.getFont();
        Font boldLabelFont = normalLabelFont.deriveFont(Font.BOLD);
        final Font smallLabelFont = normalLabelFont.deriveFont(normalLabelFont.getSize2D() - 1);
        //TODO: is this fine or should it be padded like original?
        final URL iconURL = GuiMain.class.getResource("/icon/wildfly_logo.png");
        final Image logo = Toolkit.getDefaultToolkit().getImage(iconURL);
        final Icon icon = new ImageIcon(logo);
        final JLabel mastheadLabel = new JLabel(icon);

        cp.add(mastheadLabel, NORTH);
        cp.add(urlPanel, CENTER);
        cp.add(bottomPanel, SOUTH);

        tfURL = new JTextField();
        tfURL.getDocument().addDocumentListener(new UrlDocumentListener(tfURL));

        tfURL.setPreferredSize(tfURL.getPreferredSize());

        final JPanel tfPanel = new JPanel(new BorderLayout());
        urlPanel.add(tfPanel, CENTER);

        tfPanel.add(tfURL, NORTH);

        final JLabel remoteMessageLabel = new JLabel(HINT_CONNECT);
        remoteMessageLabel.setFont(smallLabelFont);
        remoteMessageLabel.setForeground(hintTextColor);
        tfPanel.add(remoteMessageLabel, CENTER);

        final JPanel userPwdPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        userPwdPanel.setBorder(new EmptyBorder(12, 0, 0, 0)); // top padding

        int tfWidth = IS_WIN ? 12 : 8;

        tfUserName = new JTextField(tfWidth);


        JPanel lc;
        lc = new Labeled(TEXT_USERNAME,boldLabelFont, tfUserName);
        userPwdPanel.add(lc);

        tfPassword = new JPasswordField(tfWidth);
        // Heights differ, so fix here
        tfPassword.setPreferredSize(tfUserName.getPreferredSize());

        lc = new Labeled(TEXT_PASSWORD, boldLabelFont, tfPassword);
        lc.setBorder(new EmptyBorder(0, 12, 0, 0)); // Left padding
        lc.setFont(boldLabelFont);
        userPwdPanel.add(lc);

        tfPanel.add(userPwdPanel, SOUTH);

        final JButton connectButton = new JButton(this.actionConnect);
        connectButton.setToolTipText(HINT_CONNECT_BUTTON);

        final JButton cancelButton = new JButton(actionCancel);

        final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        buttonPanel.setBorder(new EmptyBorder(12, 12, 2, 12));
        if (IS_GTK) {
            buttonPanel.add(cancelButton);
            buttonPanel.add(connectButton);
        } else {
            buttonPanel.add(connectButton);
            buttonPanel.add(cancelButton);
        }
        bottomPanel.add(buttonPanel, NORTH);
        bottomPanel.add(statusBar, SOUTH);
        this.pack();
    }

    private void clearStatus(){
        statusBar.setText(" ");
    }

    private class UrlDocumentListener implements DocumentListener{

        private static final String RX_PROTOCOL = "[A-Za-z\\-]+://";
        private static final String RX_HOST = "[1-9A-Za-z\\.]+";
        private static final String RX_PORT = ":\\d+";

        private static final String REGEXP = "(" + RX_HOST + ")|(" + RX_HOST + RX_PORT + ")|(" + RX_PROTOCOL + RX_HOST + ")|(" + RX_PROTOCOL + RX_HOST + RX_PORT + ")";

        private final JTextField textField;

        public UrlDocumentListener(JTextField textField) {
            super();
            this.textField = textField;
        }

        @Override
        public void changedUpdate(DocumentEvent documentEvent) {
        }

        @Override
        public void insertUpdate(DocumentEvent documentEvent) {
            //documentEvent.getDocument().getText() return trash...
            validateURL();
        }

        @Override
        public void removeUpdate(DocumentEvent documentEvent) {
            validateURL();
        }

        private void validateURL(){
            //more?
            final String text = textField.getText();
            if(text == null || text.length() == 0){
                clearStatus();
                return;
            }
            if(Pattern.matches(REGEXP, text)){
                clearStatus();
            } else {
                statusBar.setText("Connection url is not correct.");
            }
        }
    }
    private class Labeled extends JPanel{
        private final Component comp;
        private final JLabel leftLabel;

        public Labeled(final String label, final Font font, final Component toBeLabeled){
            this.comp = toBeLabeled;
            this.leftLabel = new JLabel(label);
            if(font!=null)
                this.leftLabel.setFont(font);

            super.setLayout(new BorderLayout(6,6));
            super.add(leftLabel,BorderLayout.WEST);
            super.add(comp,BorderLayout.CENTER);
        }
    }
}