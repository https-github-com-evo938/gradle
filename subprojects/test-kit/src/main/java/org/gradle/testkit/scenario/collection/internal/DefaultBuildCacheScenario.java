/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.testkit.scenario.collection.internal;

import org.gradle.api.Action;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.gradle.testkit.scenario.GradleScenarioSteps;
import org.gradle.testkit.scenario.ScenarioResult;
import org.gradle.testkit.scenario.collection.BuildCacheScenario;
import org.gradle.testkit.scenario.internal.DefaultGradleScenario;
import org.gradle.util.GFileUtils;

import java.io.File;
import java.util.function.Supplier;

import static org.gradle.testkit.scenario.internal.GradleScenarioUtilInternal.appendArguments;
import static org.gradle.testkit.scenario.internal.GradleScenarioUtilInternal.assertTaskOutcomes;
import static org.gradle.util.TextUtil.normaliseFileSeparators;


public class DefaultBuildCacheScenario extends DefaultGradleScenario implements BuildCacheScenario {

    private String[] taskPaths;
    private boolean withoutRelocatabilityTest;

    @Override
    public BuildCacheScenario withTaskPaths(String... taskPaths) {
        this.taskPaths = taskPaths;
        return this;
    }

    @Override
    public BuildCacheScenario withoutRelocatabilityTest() {
        this.withoutRelocatabilityTest = true;
        return this;
    }

    @Override
    public ScenarioResult run() {

        Action<GradleRunner> isolatedBuildCache = isolateLocalBuildCache();

        withSteps(steps -> {

            steps.named(Steps.CLEAN_BUILD)
                .withRunnerAction(isolatedBuildCache)
                .withArguments(taskPaths)
                .withResult(result -> assertTaskOutcomes(Steps.CLEAN_BUILD, taskPaths, result, TaskOutcome.SUCCESS));

            steps.named(Steps.FROM_CACHE_BUILD)
                .withRunnerAction(isolatedBuildCache)
                .withCleanWorkspace()
                .withArguments(taskPaths)
                .withResult(result -> assertTaskOutcomes(Steps.FROM_CACHE_BUILD, taskPaths, result, TaskOutcome.FROM_CACHE));

            if (!withoutRelocatabilityTest) {
                steps.named(Steps.FROM_CACHE_RELOCATED_BUILD)
                    .withRunnerAction(isolatedBuildCache)
                    .withCleanWorkspace()
                    .withRelocatedWorkspace()
                    .withArguments(taskPaths)
                    .withResult(result -> assertTaskOutcomes(Steps.FROM_CACHE_RELOCATED_BUILD, taskPaths, result, TaskOutcome.FROM_CACHE));
            }
        });

        return super.run();
    }

    private Action<GradleRunner> isolateLocalBuildCache() {

        File scenarioFilesDir = new File(baseDirectory, "build-cache-scenario");
        File localBuildCacheInitScript = new File(scenarioFilesDir, "local-cache.init.gradle");
        withBaseDirectoryAction(baseDirectory -> {
            File localBuildCacheDir = new File(scenarioFilesDir, "local-cache");
            String localBuildCacheInitScriptContent = "\n" +
                "settingsEvaluated { settings ->\n" +
                "  settings.buildCache {\n" +
                "    local {\n" +
                "      directory('" + normaliseFileSeparators(localBuildCacheDir.getAbsolutePath()) + "')\n" +
                "    }\n" +
                "  }\n" +
                "}\n";
            GFileUtils.writeFile(localBuildCacheInitScriptContent, localBuildCacheInitScript);
        });

        return runner -> appendArguments(
            runner,
            "--build-cache",
            "-I",
            localBuildCacheInitScript.getAbsolutePath()
        );
    }

    @Override
    public BuildCacheScenario withBaseDirectory(File baseDirectory) {
        super.withBaseDirectory(baseDirectory);
        return this;
    }

    @Override
    public BuildCacheScenario withWorkspace(Action<File> workspaceBuilder) {
        super.withWorkspace(workspaceBuilder);
        return this;
    }

    @Override
    public BuildCacheScenario withRunnerFactory(Supplier<GradleRunner> runnerFactory) {
        super.withRunnerFactory(runnerFactory);
        return this;
    }

    @Override
    public BuildCacheScenario withRunnerAction(Action<GradleRunner> runnerAction) {
        super.withRunnerAction(runnerAction);
        return this;
    }

    @Override
    public BuildCacheScenario withSteps(Action<GradleScenarioSteps> steps) {
        super.withSteps(steps);
        return this;
    }
}
