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
package com.microsoft.intellij;

import com.google.gson.Gson;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.PlatformUtils;
import com.microsoft.azure.hdinsight.common.HDInsightHelperImpl;
import com.microsoft.azure.hdinsight.common.HDInsightLoader;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.ijidea.ui.UIFactory;
import com.microsoft.intellij.common.CommonConst;
import com.microsoft.intellij.helpers.IDEHelperImpl;
import com.microsoft.intellij.helpers.UIHelperImpl;
import com.microsoft.intellij.serviceexplorer.NodeActionsMap;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.components.PluginComponent;
import com.microsoft.tooling.msservices.components.PluginSettings;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import rx.internal.util.PlatformDependent;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class AzureActionsComponent implements ApplicationComponent, PluginComponent {
    public static final String PLUGIN_ID = CommonConst.PLUGIN_ID;
    private static final Logger LOG = Logger.getInstance(AzureActionsComponent.class);
    private static FileHandler logFileHandler = null;

    private PluginSettings settings;

    public AzureActionsComponent() {
        DefaultLoader.setPluginComponent(this);
        DefaultLoader.setUiHelper(new UIHelperImpl());
        DefaultLoader.setIdeHelper(new IDEHelperImpl());
        Node.setNode2Actions(NodeActionsMap.node2Actions);

        HDInsightLoader.setHHDInsightHelper(new HDInsightHelperImpl());
        try {
            loadPluginSettings();
        } catch (IOException e) {
            PluginUtil.displayErrorDialogAndLog(message("errTtl"), "An error occurred while attempting to load settings", e);
        }
    }

    @NotNull
    public String getComponentName() {
        return this.getClass().getName();
    }

    public void initComponent() {
        if (!AzurePlugin.IS_ANDROID_STUDIO) {
            initAuthManage();
            ActionManager am = ActionManager.getInstance();
            DefaultActionGroup toolbarGroup = (DefaultActionGroup) am.getAction(IdeActions.GROUP_MAIN_TOOLBAR);
            toolbarGroup.addAll((DefaultActionGroup) am.getAction("AzureToolbarGroup"));
            DefaultActionGroup popupGroup = (DefaultActionGroup) am.getAction(IdeActions.GROUP_PROJECT_VIEW_POPUP);
            popupGroup.add(am.getAction("AzurePopupGroup"));
            if (PlatformUtils.isIdeaUltimate()) {
                ActionManager actionManager = ActionManager.getInstance();
                DefaultActionGroup actionGroup = (DefaultActionGroup) actionManager.getAction("PublishGroup");
                if (actionGroup != null)
                    actionGroup.addAll((ActionGroup) actionManager.getAction("AzureWebDeployGroup"));
            }
        }
        try {
            PlatformDependent.isAndroid();
        } catch (Throwable ignored ) {
            DefaultLoader.getUIHelper().showError("A problem with your Android Support plugin setup is preventing the Azure Toolkit from functioning correctly (Retrofit2 and RxJava failed to initialize).\n" +
                "To fix this issue, try disabling the Android Support plugin or installing the Android SDK", "Azure Toolkit for IntelliJ");
//            DefaultLoader.getUIHelper().showException("Android Support Error: isAndroid() throws " + ignored.getMessage(), ignored, "Error Android", true, false);
        }
    }

    private void initAuthManage() {
        if (CommonSettings.getUiFactory() == null) {
            CommonSettings.setUiFactory(new UIFactory());
        }
        String wd = "AzureToolsForIntelliJ";
        Path dirPath = Paths.get(System.getProperty("user.home"), wd);
        try {
            if (!Files.exists(dirPath)) {
                Files.createDirectory(dirPath);
            }
            CommonSettings.settingsBaseDir = dirPath.toString();
            initLoggerFileHandler();
        } catch (IOException ex) {
            LOG.error("initAuthManage()", ex);
        }
    }

    private void initLoggerFileHandler() {
        try {
            String loggerFilePath = Paths.get(CommonSettings.settingsBaseDir, "corelibs.log").toString();
            System.out.println("Logger path:" + loggerFilePath);
            logFileHandler = new FileHandler(loggerFilePath, false);
            java.util.logging.Logger l = java.util.logging.Logger.getLogger("");
            logFileHandler.setFormatter(new SimpleFormatter());
            l.addHandler(logFileHandler);
            // FIXME: use environment variable to set level
            l.setLevel(Level.INFO);
            l.info("=== Log session started ===");
        } catch (IOException e) {
            e.printStackTrace();
            LOG.error("initLoggerFileHandler()", e);
        }
    }


    public void disposeComponent() {
    }

    @Override
    public PluginSettings getSettings() {
        return settings;
    }

    @Override
    public String getPluginId() {
        return PLUGIN_ID;
    }

    private void loadPluginSettings() throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(AzureActionsComponent.class.getResourceAsStream("/settings.json")));
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            Gson gson = new Gson();
            settings = gson.fromJson(sb.toString(), PluginSettings.class);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}