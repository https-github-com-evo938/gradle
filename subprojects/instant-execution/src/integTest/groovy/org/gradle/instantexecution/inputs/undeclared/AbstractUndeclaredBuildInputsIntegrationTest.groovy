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

package org.gradle.instantexecution.inputs.undeclared

import org.gradle.instantexecution.AbstractInstantExecutionIntegrationTest
import spock.lang.Unroll

abstract class AbstractUndeclaredBuildInputsIntegrationTest extends AbstractInstantExecutionIntegrationTest {
    abstract void buildLogicApplication(SystemPropertyRead read)

    @Unroll
    def "reports undeclared system property read using #propertyRead.groovyExpression prior to task execution from plugin"() {
        buildLogicApplication(propertyRead)
        def fixture = newInstantExecutionFixture()

        when:
        run("thing", "-DCI=true")

        then:
        outputContains("apply = true")
        outputContains("task = true")

        when:
        instantFails("thing", "-DCI=true")

        then:
        // TODO - use problems fixture, need to be able to tweak the problem matching as build script class name is included in the message and this is generated
        failure.assertThatDescription(containsNormalizedString("- unknown location: read system property 'CI' from '"))

        when:
        problems.withDoNotFailOnProblems()
        instantRun("thing", "-DCI=true")

        then:
        fixture.assertStateStored()
        // TODO - use problems fixture, need to be able to tweak the problem matching as build script class name is included in the message and this is generated
        outputContains("- unknown location: read system property 'CI' from '")
        outputContains("apply = true")
        outputContains("task = true")

        when:
        instantRun("thing", "-DCI=false")

        then:
        fixture.assertStateLoaded() // undeclared properties are not considered build inputs, but probably should be
        problems.assertResultHasProblems(result)
        outputContains("task = false")

        where:
        propertyRead << [
            SystemPropertyRead.systemGetProperty("CI"),
            SystemPropertyRead.systemGetPropertyWithDefault("CI", "default"),
            SystemPropertyRead.systemGetPropertiesGet("CI"),
            SystemPropertyRead.systemGetPropertiesGetProperty("CI"),
            SystemPropertyRead.systemGetPropertiesGetPropertyWithDefault("CI", "default")
        ]
    }
}
