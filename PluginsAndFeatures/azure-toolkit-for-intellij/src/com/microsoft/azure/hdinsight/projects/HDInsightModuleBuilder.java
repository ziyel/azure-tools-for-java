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
package com.microsoft.azure.hdinsight.projects;

import com.intellij.facet.impl.ui.libraries.LibraryCompositionSettings;
import com.intellij.ide.util.projectWizard.JavaModuleBuilder;
import com.intellij.ide.util.projectWizard.ModuleBuilderListener;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.SettingsStep;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainer;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainerFactory;
import com.intellij.openapi.util.IconLoader;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.packaging.elements.CompositePackagingElement;
import com.intellij.packaging.elements.PackagingElementFactory;
import com.intellij.packaging.impl.artifacts.JarArtifactType;
import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.projects.samples.MavenProjectGenerator;
import com.microsoft.azure.hdinsight.projects.template.CustomHDInsightTemplateItem;
import com.microsoft.azure.hdinsight.projects.template.CustomModuleWizardSetup;
import com.microsoft.azure.hdinsight.projects.template.CustomTemplateInfo;
import com.microsoft.azure.hdinsight.projects.template.TemplatesUtil;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.intellij.hdinsight.messages.HDInsightBundle;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

public class HDInsightModuleBuilder extends JavaModuleBuilder implements ModuleBuilderListener {
    private HDInsightTemplateItem selectedTemplate;
    private LibrariesContainer librariesContainer;
    private LibraryCompositionSettings scalaLibraryCompositionSettings;
    private SparkVersion sparkVersion;

    public static final String UniqueKeyName = "UniqueKey";
    public static final String UniqueKeyValue = "HDInsightTool";

    public HDInsightModuleBuilder() {
        this.addListener(this);
    }

    public void setSelectedTemplate(HDInsightTemplateItem selectedTemplate) {
        this.selectedTemplate = selectedTemplate;
    }

    public void setScalaLibraryCompositionSettings(LibraryCompositionSettings scalaLibraryCompositionSettings) {
        this.scalaLibraryCompositionSettings = scalaLibraryCompositionSettings;
    }

    public void setSparkVersion(SparkVersion sparkVersion) {
        this.sparkVersion = sparkVersion;
    }

    @Override
    public String getBuilderId() {
        return "HDInsight";
    }

    @Override
    public Icon getBigIcon() {
        return null;
    }

    @Override
    public Icon getNodeIcon() {
        return IconLoader.getIcon(CommonConst.ProductIConPath);
    }

    @Override
    public String getPresentableName() {
        return "HDInsight";
    }

    @Override
    public String getGroupName() {
        return "HDInsight Tools";
    }

    @Override
    public ModuleType getModuleType() {
        return HDInsightModuleType.getInstance();
    }

    @Override
    public ModuleWizardStep modifySettingsStep(SettingsStep settingsStep) {
        this.librariesContainer = LibrariesContainerFactory.createContainer(settingsStep.getContext().getProject());

        if (this.selectedTemplate.getType() == HDInsightTemplatesType.CustomTemplate) {
            return new CustomModuleWizardSetup(this, settingsStep, librariesContainer, ((CustomHDInsightTemplateItem) this.selectedTemplate).getTemplateInfo());
        } else if (this.selectedTemplate.getType() == HDInsightTemplatesType.Scala ||
                this.selectedTemplate.getType() == HDInsightTemplatesType.ScalaClusterSample ||
                this.selectedTemplate.getType() == HDInsightTemplatesType.ScalaLocalSample) {
            return new SparkScalaSettingsStep(this, settingsStep);
        } else {
            return new SparkJavaSettingsStep(this, settingsStep);
        }
    }

    @Override
    public void moduleCreated(@NotNull Module module) {
        HDInsightTemplatesType templatesType = this.selectedTemplate.getType();

        if(templatesType == HDInsightTemplatesType.CustomTemplate) {
            customTemplateModuleCreated(module, ((CustomHDInsightTemplateItem)this.selectedTemplate).getTemplateInfo());
        } else {
            module.setOption(UniqueKeyName, UniqueKeyValue);
            createDefaultArtifact(module);
            new MavenProjectGenerator(module, templatesType, sparkVersion).generate();
        }

        addTelemetry(templatesType, sparkVersion);
    }

    private void customTemplateModuleCreated(Module module, CustomTemplateInfo info) {
        if(info.isSparkProject()) {
            module.setOption(UniqueKeyName, UniqueKeyValue);
        }
        TemplatesUtil.createTemplateSampleFiles(module, info);
    }

    private void addTelemetry(HDInsightTemplatesType templatesType, SparkVersion sparkVersion){
        Map<String, String> hdiProperties = new HashMap<String, String>();
        hdiProperties.put("Spark Version", sparkVersion.toString());

        if(templatesType == HDInsightTemplatesType.Java){
            AppInsightsClient.create(HDInsightBundle.message("SparkProjectSystemJavaCreation"), null, hdiProperties);
        }else if(templatesType == HDInsightTemplatesType.JavaLocalSample){
            AppInsightsClient.create(HDInsightBundle.message("SparkProjectSystemJavaSampleCreation"), null, hdiProperties);
        }else if(templatesType == HDInsightTemplatesType.Scala) {
            AppInsightsClient.create(HDInsightBundle.message("SparkProjectSystemScalaCreation"), null, hdiProperties);
        }else if(templatesType == HDInsightTemplatesType.ScalaClusterSample || templatesType == HDInsightTemplatesType.ScalaLocalSample){
            AppInsightsClient.create(HDInsightBundle.message("SparkProjectSystemScalaSampleCreation"), null, hdiProperties);
        }
    }

    private void createDefaultArtifact(final Module module) {
        final Project project = module.getProject();
        final JarArtifactType type = new JarArtifactType();
        final PackagingElementFactory factory = PackagingElementFactory.getInstance();
        CompositePackagingElement root = factory.createArchive("default_artifact.jar");
        root.addOrFindChild(factory.createModuleOutput(module));
        ArtifactManager.getInstance(project).addArtifact(module.getName() + "_DefaultArtifact", type, root);
    }
}
