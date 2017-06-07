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

import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.util.containers.ContainerUtilRt;
import com.microsoft.azure.hdinsight.projects.util.ProjectSampleUtil;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.jetbrains.sbt.project.SbtProjectSystem;
import org.jetbrains.sbt.project.settings.SbtProjectSettings;
import org.jetbrains.sbt.settings.SbtSystemSettings;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;

public class SbtProjectGenerator {
    private Module module;
    private HDInsightTemplatesType templatesType;
    private String sparkVersion;
    private String scalaVersion;
    private String sbtVersion;

    public SbtProjectGenerator(@NotNull Module module,
                               @NotNull HDInsightTemplatesType templatesType,
                               @NotNull SparkVersion sparkVersion,
                               @NotNull String sbtVersion) {
        this.module = module;
        this.templatesType = templatesType;
        this.sparkVersion = sparkVersion.getSparkVersion();
        this.scalaVersion = sparkVersion.getScalaVersion();
        this.sbtVersion = sbtVersion;
    }

    public void generate() {
        String root = ProjectSampleUtil.getRootOrSourceFolder(this.module, false);
        try {
            createDirectories(root);
            copySamples(root);
            generateSbt(root);
            importSbtProject(root);
        } catch (Exception e) {
            DefaultLoader.getUIHelper().showError("Failed to create project", "Create Sample Project");
        }
    }

    private void createDirectories(String root) throws IOException {
        switch (this.templatesType) {
            case JavaLocalSample:
            case Java:
                VfsUtil.createDirectories(root + "/src/main/java/sample");
                VfsUtil.createDirectories(root + "/src/main/resources");
                VfsUtil.createDirectories(root + "/src/test/java");
                break;
            case Scala:
            case ScalaLocalSample:
            case ScalaClusterSample:
                VfsUtil.createDirectories(root + "/src/main/scala/sample");
                VfsUtil.createDirectories(root + "/src/main/resources");
                VfsUtil.createDirectories(root + "/src/test/scala");
                break;
        }
    }

    private void copySamples(String root) throws Exception {
        switch (this.templatesType) {
            case JavaLocalSample:
                ProjectSampleUtil.copyFileToPath(new String[]{
                        "/hdinsight/templates/java/JavaSparkPi.java"
                }, root + "/src/main/java/sample");
                break;
            case ScalaClusterSample:
                ProjectSampleUtil.copyFileToPath(new String[]{
                        "/hdinsight/templates/scala/scala_cluster_run/SparkCore_WasbIOTest.scala",
                        "/hdinsight/templates/scala/scala_cluster_run/SparkStreaming_HdfsWordCount.scala",
                        "/hdinsight/templates/scala/scala_cluster_run/SparkSQL_RDDRelation.scala"
                }, root + "/src/main/scala/sample");
                break;
            case ScalaLocalSample:
                ProjectSampleUtil.copyFileToPath(new String[]{
                        "/hdinsight/templates/scala/scala_local_run/LogQuery.scala",
                        "/hdinsight/templates/scala/scala_local_run/SparkML_RankingMetricsExample.scala"
                }, root + "/src/main/scala/sample");

                ProjectSampleUtil.copyFileToPath(new String[]{
                        "/hdinsight/templates/scala/scala_local_run/data/sample_movielens_data.txt"
                }, root + "/data");
                break;
        }
    }

    private void generateSbt(String root) throws IOException {
        File sbt = new File(root + File.separator + "build.sbt");
        FileUtil.writeToFile(sbt, generateSbtFileContent());
    }

    private String generateSbtFileContent() {
        return "balabala";
    }

    private void importSbtProject(String root) {
        Project project = this.module.getProject();

        ExternalSystemUtil.refreshProject(project,
                SbtProjectSystem.Id(), root,
                false, ProgressExecutionMode.IN_BACKGROUND_ASYNC);

        SbtProjectSettings sbtProjectSettings = new SbtProjectSettings();
        sbtProjectSettings.setExternalProjectPath(root);

        SbtSystemSettings sbtSystemSettings = SbtSystemSettings.getInstance(project);
        HashSet<SbtProjectSettings> projects = ContainerUtilRt.newHashSet(sbtSystemSettings.getLinkedProjectsSettings());
        projects.add(sbtProjectSettings);
        sbtSystemSettings.setLinkedProjectsSettings(projects);
    }
}
