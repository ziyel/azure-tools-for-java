/*
 * Copyright (c) Microsoft Corporation
 *   <p/>
 *  All rights reserved.
 *   <p/>
 *  MIT License
 *   <p/>
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 *  documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 *  to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *  <p/>
 *  The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 *  the Software.
 *   <p/>
 *  THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 *  THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 *  TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package com.microsoft.azuretools.ijidea.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.ijidea.actions.SelectSubscriptionsAction;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.util.List;

public class SubscriptionsDialog extends AzureDialogWrapper {
    private static final Logger LOGGER = Logger.getInstance(SubscriptionsDialog.class);

    private JPanel contentPane;
    private JPanel panelTable;
    private JTable table;
    private List<SubscriptionDetail> sdl;

    private Project project;

    private static class SubscriptionTableModel extends DefaultTableModel {
        final Class[] columnClass = new Class[]{
                Boolean.class, String.class, String.class
        };

        @Override
        public boolean isCellEditable(int row, int col) {
            return (col == 0);
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnClass[columnIndex];
        }
    }

    ;

    public List<SubscriptionDetail> getSubscriptionDetails() {
        return sdl;
    }

    public static SubscriptionsDialog go(List<SubscriptionDetail> sdl, Project project) throws Exception {
        SubscriptionsDialog d = new SubscriptionsDialog(sdl, project);
        d.show();
        if (d.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
            return d;
        }

        return null;
    }

    private SubscriptionsDialog(List<SubscriptionDetail> sdl, Project project) {
        super(project, true, IdeModalityType.PROJECT);
        this.sdl = sdl;
        this.project = project;
        setModal(true);
        setTitle("Select Subscriptions");
        setOKButtonText("Select");

        setSubscriptions();

        init();
    }

    @Override
    protected Action[] createActions() {
        return new Action[]{this.getOKAction(), this.getCancelAction()};
    }

    private void refreshSubscriptions() {
        try {
            AzureManager manager = AuthMethodManager.getInstance().getAzureManager();
            if (manager == null) {
                return;
            }
            final SubscriptionManager subscriptionManager = manager.getSubscriptionManager();
            subscriptionManager.cleanSubscriptions();

            DefaultTableModel dm = (DefaultTableModel)table.getModel();
            dm.getDataVector().removeAllElements();
            dm.fireTableDataChanged();

            SelectSubscriptionsAction.updateSubscriptionWithProgressDialog(subscriptionManager, project);

            //System.out.println("refreshSubscriptions: calling getSubscriptionDetails()");
            sdl = subscriptionManager.getSubscriptionDetails();
            setSubscriptions();
            // to notify subscribers
            subscriptionManager.setSubscriptionDetails(sdl);

        } catch (Exception ex) {
            ex.printStackTrace();
            //LOGGER.error("refreshSubscriptions", ex);
            ErrorWindow.show(project, ex.getMessage(), "Refresh Subscriptions Error");
        }
    }

    private void setSubscriptions() {
        DefaultTableModel model = (DefaultTableModel)table.getModel();
        for (SubscriptionDetail sd : sdl) {
            model.addRow(new Object[] {sd.isSelected(), sd.getSubscriptionName(), sd.getSubscriptionId()});
        }
        model.fireTableDataChanged();
    }

    private void createUIComponents() {
        DefaultTableModel model = new SubscriptionTableModel();
        model.addColumn("");
        model.addColumn("Subscription name");
        model.addColumn("Subscription ID");

        table = new JBTable();
        table.setModel(model);
        TableColumn column = table.getColumnModel().getColumn(0);
        column.setMinWidth(23);
        column.setMaxWidth(23);
        table.setRowSelectionAllowed(false);
        table.setCellSelectionEnabled(false);

        AnActionButton refreshAction = new AnActionButton("Refresh", AllIcons.Actions.Refresh) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                AppInsightsClient.createByType(AppInsightsClient.EventType.Subscription, "", "Refresh", null);
                refreshSubscriptions();
            }
        };

        ToolbarDecorator tableToolbarDecorator =
            ToolbarDecorator.createDecorator(table)
                .disableUpDownActions()
                .addExtraActions(refreshAction);

        panelTable = tableToolbarDecorator.createPanel();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    @Override
    protected void doOKAction() {
        DefaultTableModel model = (DefaultTableModel)table.getModel();
        int rc = model.getRowCount();
        int unselectedCount = 0;
        for (int ri = 0; ri < rc; ++ri) {
            boolean selected = (boolean)model.getValueAt(ri, 0);
            if (!selected) unselectedCount++;
        }

        if (unselectedCount == rc) {
            JOptionPane.showMessageDialog(contentPane,
                    "Please select at least one subscription",
                    "Subscription dialog info",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        for (int ri = 0; ri < rc; ++ri) {
            boolean selected = (boolean)model.getValueAt(ri, 0);
            this.sdl.get(ri).setSelected(selected);
        }
        super.doOKAction();
    }

    @Nullable
    @Override
    protected String getDimensionServiceKey() {
        return "SubscriptionsDialog";
    }

}
