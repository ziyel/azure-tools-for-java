/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.intellij.ui;


import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ValidationInfo;
import com.microsoft.azuretools.adauth.StringUtils;
import com.microsoft.azuretools.azurecommons.util.GetHashMac;
import com.microsoft.azuretools.azurecommons.util.ParserXMLUtility;
import com.microsoft.azuretools.azurecommons.xmlhandling.DataOperations;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.intellij.util.PluginHelper;
import com.microsoft.intellij.util.PluginUtil;
import org.w3c.dom.Document;

import javax.swing.*;
import java.io.File;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;


public class AzurePanel implements AzureAbstractConfigurablePanel {
    private static final String DISPLAY_NAME = "Azure";

    private JCheckBox checkBox1;
    private JTextPane textPane1;
    private JPanel contentPane;
    String dataFile = PluginHelper.getTemplateFile(message("dataFileName"));

    public AzurePanel() {
    }

    public void init() {
        if (!AzurePlugin.IS_ANDROID_STUDIO && AzurePlugin.IS_WINDOWS) {
            Messages.configureMessagePaneUi(textPane1, message("preferenceLinkMsg"));
            if (new File(dataFile).exists()) {
                String prefValue = DataOperations.getProperty(dataFile, message("prefVal"));
                if (prefValue != null && !prefValue.isEmpty()) {
                    if (prefValue.equals("true")) {
                        checkBox1.setSelected(true);
                    }
                }
            }
        }
    }

    public JComponent getPanel() {
        return contentPane;
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public boolean doOKAction() {
        try {
            if (new File(dataFile).exists()) {
                Document doc = ParserXMLUtility.parseXMLFile(dataFile);
                String oldPrefVal = DataOperations.getProperty(dataFile, message("prefVal"));
                DataOperations.updatePropertyValue(doc, message("prefVal"), String.valueOf(checkBox1.isSelected()));
                String version = DataOperations.getProperty(dataFile, message("pluginVersion"));
                if (version == null || version.isEmpty()) {
                    DataOperations.updatePropertyValue(doc, message("pluginVersion"), AzurePlugin.PLUGIN_VERSION);
                } else if (!AzurePlugin.PLUGIN_VERSION.equalsIgnoreCase(version)) {
                    DataOperations.updatePropertyValue(doc, message("pluginVersion"), AzurePlugin.PLUGIN_VERSION);
                    AppInsightsClient.createByType(AppInsightsClient.EventType.Plugin, "", AppInsightsConstants.Upgrade, null, true);
                }
                String instID = DataOperations.getProperty(dataFile, message("instID"));
                if (instID == null || instID.isEmpty() || !GetHashMac.IsValidHashMacFormat(instID)) {
                    DataOperations.updatePropertyValue(doc, message("instID"), GetHashMac.GetHashMac());
                    AppInsightsClient.createByType(AppInsightsClient.EventType.Plugin, "", AppInsightsConstants.Install, null, true);
                }
                ParserXMLUtility.saveXMLFile(dataFile, doc);
                // Its necessary to call application insights custom create event after saving data.xml
                final boolean acceptTelemetry = checkBox1.isSelected();
                if (StringUtils.isNullOrEmpty(oldPrefVal) || Boolean.valueOf(oldPrefVal) != acceptTelemetry) {
                    // Boolean.valueOf(oldPrefVal) != acceptTelemetry means user changes his mind.
                    // Either from Agree to Deny, or from Deny to Agree.
                    final String action = acceptTelemetry ? AppInsightsConstants.Allow : AppInsightsConstants.Deny;
                    AppInsightsClient.createByType(AppInsightsClient.EventType.Telemetry, "", action, null, true);
                }
            } else {
                AzurePlugin.copyResourceFile(message("dataFileName"), dataFile);
                setValues(dataFile);
            }
        } catch (Exception ex) {
            AzurePlugin.log(ex.getMessage(), ex);
            PluginUtil.displayErrorDialog(message("errTtl"), message("updateErrMsg"));
            return false;
        }
        return true;
    }

    private void setValues(String dataFile) throws Exception {
        Document doc = ParserXMLUtility.parseXMLFile(dataFile);
        DataOperations.updatePropertyValue(doc, message("pluginVersion"), AzurePlugin.PLUGIN_VERSION);
        DataOperations.updatePropertyValue(doc, message("instID"), GetHashMac.GetHashMac());
        DataOperations.updatePropertyValue(doc, message("prefVal"), String.valueOf(checkBox1.isSelected()));
        ParserXMLUtility.saveXMLFile(dataFile, doc);
    }

    @Override
    public String getSelectedValue() {
        return null;
    }

    public ValidationInfo doValidate() {
        return null;
    }

    @Override
    public String getHelpTopic() {
        return null;
    }

    @Override
    public boolean isModified() {
        return true;
    }

    @Override
    public void reset() {
    }
}
