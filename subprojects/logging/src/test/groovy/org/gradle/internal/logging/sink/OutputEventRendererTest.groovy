/*
 * Copyright 2016 the original author or authors.
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


package org.gradle.internal.logging.sink

import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.StandardOutputListener
import org.gradle.internal.logging.OutputSpecification
import org.gradle.internal.logging.console.ConsoleStub
import org.gradle.internal.logging.console.ThrottlingOutputEventListener
import org.gradle.internal.logging.events.LogEvent
import org.gradle.internal.logging.events.LogLevelChangeEvent
import org.gradle.internal.logging.events.OutputEventListener
import org.gradle.internal.logging.events.UpdateNowEvent
import org.gradle.internal.nativeintegration.console.ConsoleMetaData
import org.gradle.internal.progress.BuildOperationCategory
import org.gradle.test.fixtures.ConcurrentTestUtil
import org.gradle.util.RedirectStdOutAndErr
import org.junit.Rule
import spock.lang.Unroll

class OutputEventRendererTest extends OutputSpecification {
    @Rule public final RedirectStdOutAndErr outputs = new RedirectStdOutAndErr()
    private final ConsoleStub console = new ConsoleStub()
    private final ConsoleMetaData metaData = Mock()
    private OutputEventRenderer renderer

    def setup() {
        renderer = new OutputEventRenderer()
        renderer.configure(LogLevel.INFO)
    }

    def rendersLogEventsToStdOut() {
        when:
        renderer.attachSystemOutAndErr()
        renderer.onOutput(event('message', LogLevel.INFO))

        then:
        ConcurrentTestUtil.poll(1, 0.1) {
            assert outputs.stdOut.readLines() == ['message']
            assert outputs.stdErr == ''
        }
    }

    def rendersErrorLogEventsToStdErr() {
        when:
        renderer.attachSystemOutAndErr()
        renderer.onOutput(event('message', LogLevel.ERROR))

        then:
        ConcurrentTestUtil.poll(1, 0.1) {
            assert outputs.stdOut == ''
            assert outputs.stdErr.readLines() == ['message']
        }
    }

    def rendersLogEventsWhenLogLevelIsDebug() {
        def listener = new TestListener()

        when:
        renderer.configure(LogLevel.DEBUG)
        renderer.addStandardOutputListener(listener)
        renderer.onOutput(event(tenAm, 'message', LogLevel.INFO))

        then:
        ConcurrentTestUtil.poll(1, 0.1) {
            assert listener.value.readLines() == ['10:00:00.000 [INFO] [category] message']
        }
    }

    def rendersLogEventsToStdOutandStdErrWhenLogLevelIsDebug() {
        when:
        renderer.configure(LogLevel.DEBUG)
        renderer.attachSystemOutAndErr()
        renderer.onOutput(event(tenAm, 'info', LogLevel.INFO))
        renderer.onOutput(event(tenAm, 'error', LogLevel.ERROR))

        then:
        ConcurrentTestUtil.poll(1, 0.1) {
            assert outputs.stdOut.readLines() == ['10:00:00.000 [INFO] [category] info']
            assert outputs.stdErr.readLines() == ['10:00:00.000 [ERROR] [category] error']
        }
    }

    def rendersLogEventsToStdOutListener() {
        def listener = new TestListener()

        when:
        renderer.addStandardOutputListener(listener)
        renderer.onOutput(event('info', LogLevel.INFO))
        renderer.onOutput(event('error', LogLevel.ERROR))

        then:
        ConcurrentTestUtil.poll(1, 0.1) {
            assert listener.value.readLines() == ['info']
        }
    }

    def doesNotRenderLogEventsToRemovedStdOutListener() {
        def listener = new TestListener()

        when:
        renderer.addStandardOutputListener(listener)
        renderer.removeStandardOutputListener(listener)
        renderer.onOutput(event('info', LogLevel.INFO))
        renderer.onOutput(event('error', LogLevel.ERROR))

        then:
        ConcurrentTestUtil.poll(1, 0.1) {
            assert listener.value == ''
        }
    }

    def rendersLogEventsToStdOutListenerWhenLogLevelIsDebug() {
        def listener = new TestListener()

        when:
        renderer.configure(LogLevel.DEBUG)
        renderer.addStandardOutputListener(listener)
        renderer.onOutput(event(tenAm, 'message', LogLevel.INFO))

        then:
        ConcurrentTestUtil.poll(1, 0.1) {
            assert listener.value.readLines() == ['10:00:00.000 [INFO] [category] message']
        }
    }

    def rendersErrorLogEventsToStdErrListener() {
        def listener = new TestListener()

        when:
        renderer.addStandardErrorListener(listener)
        renderer.onOutput(event('info', LogLevel.INFO))
        renderer.onOutput(event('error', LogLevel.ERROR))

        then:
        ConcurrentTestUtil.poll(1, 0.1) {
            assert listener.value.readLines() == ['error']
        }
    }

    def doesNotRenderLogEventsToRemovedStdErrListener() {
        def listener = new TestListener()

        when:
        renderer.addStandardErrorListener(listener)
        renderer.removeStandardErrorListener(listener)
        renderer.onOutput(event('info', LogLevel.INFO))
        renderer.onOutput(event('error', LogLevel.ERROR))

        then:
        ConcurrentTestUtil.poll(1, 0.1) {
            assert listener.value == ''
        }
    }

    def rendersLogEventsToStdErrListenerWhenLogLevelIsDebug() {
        def listener = new TestListener()

        when:
        renderer.configure(LogLevel.DEBUG)
        renderer.addStandardErrorListener(listener)
        renderer.onOutput(event(tenAm, 'message', LogLevel.ERROR))

        then:
        ConcurrentTestUtil.poll(1, 0.1) {
            assert listener.value.readLines() == ['10:00:00.000 [ERROR] [category] message']
        }
    }

    def forwardsOutputEventsToListener() {
        OutputEventListener listener = Mock()
        LogEvent ignored = event('ignored', LogLevel.DEBUG)
        LogEvent event = event('message', LogLevel.INFO)

        when:
        renderer.configure(LogLevel.INFO)
        renderer.addOutputEventListener(listener)
        renderer.onOutput(ignored)
        renderer.onOutput(event)
        awaitEventQueueFlush()

        then:
        _ * listener.onOutput(_ as LogLevelChangeEvent)
        _ * listener.onOutput(_ as UpdateNowEvent)
        1 * listener.onOutput(event)
        0 * listener._
    }

    @Unroll("forward progress events to listener for #logLevel log level")
    def forwardsProgressEventsToListenerRegardlessOfTheLogLevel() {
        OutputEventListener listener = Mock()
        def start = start('start')
        def progress = progress('progress')
        def complete = complete('complete')

        when:
        renderer.configure(logLevel)
        renderer.addOutputEventListener(listener)
        renderer.onOutput(start)
        renderer.onOutput(progress)
        renderer.onOutput(complete)
        awaitEventQueueFlush()

        then:
        _ * listener.onOutput(_ as LogLevelChangeEvent)
        _ * listener.onOutput(_ as UpdateNowEvent)
        1 * listener.onOutput(start)
        1 * listener.onOutput(progress)
        1 * listener.onOutput(complete)
        0 * listener._

        where:
        logLevel << [LogLevel.ERROR, LogLevel.QUIET, LogLevel.WARN, LogLevel.LIFECYCLE, LogLevel.INFO, LogLevel.DEBUG]
    }

    def doesNotForwardOutputEventsToRemovedListener() {
        OutputEventListener listener = Mock()
        LogEvent event = event('message', LogLevel.INFO)

        when:
        renderer.configure(LogLevel.INFO)
        renderer.addOutputEventListener(listener)
        renderer.removeOutputEventListener(listener)
        renderer.onOutput(event)

        then:
        0 * listener._
    }

    def restoresLogLevelWhenChangedSinceSnapshotWasTaken() {
        def listener = new TestListener()

        given:
        renderer.addStandardOutputListener(listener)
        renderer.configure(LogLevel.INFO)
        def snapshot = renderer.snapshot()
        renderer.configure(LogLevel.DEBUG)

        when:
        renderer.restore(snapshot)
        renderer.onOutput(event('info', LogLevel.INFO))
        renderer.onOutput(event('debug', LogLevel.DEBUG))

        then:
        ConcurrentTestUtil.poll(1, 0.1) {
            assert listener.value.readLines() == ['info']
        }
    }

    def rendersProgressEvents() {
        when:
        renderer.attachSystemOutAndErr()
        renderer.onOutput(start(loggingHeader: 'description', buildOperationId: 1L, buildOperationCategory: BuildOperationCategory.TASK))
        renderer.onOutput(complete('status'))

        then:
        ConcurrentTestUtil.poll(1, 0.1) {
            assert outputs.stdOut.readLines() == ['description status']
            assert outputs.stdErr == ''
        }
    }

    def doesNotRendersProgressEventsForLogLevelQuiet() {
        when:
        renderer.attachSystemOutAndErr()
        renderer.configure(LogLevel.QUIET)
        renderer.onOutput(start('description'))
        renderer.onOutput(complete('status'))

        then:
        ConcurrentTestUtil.poll(1, 0.1) {
            assert outputs.stdOut == ''
            assert outputs.stdErr == ''
        }
    }

    def rendersLogEventsWhenStdOutAndStdErrAreConsole() {
        def snapshot = renderer.snapshot()
        renderer.addConsole(console, true, true, metaData)

        when:
        renderer.onOutput(start(loggingHeader: 'description', buildOperationId: 1L, buildOperationCategory: BuildOperationCategory.TASK))
        renderer.onOutput(event('info', LogLevel.INFO, 1L))
        renderer.onOutput(event('error', LogLevel.ERROR, 1L))
        renderer.onOutput(complete('status'))
        renderer.restore(snapshot) // close console to flush

        then:
        console.buildOutputArea.toString().readLines() == ['', '{header}> description{normal}', 'info', '{error}error', '{normal}']
    }

    def rendersLogEventsWhenOnlyStdOutIsConsole() {
        def snapshot = renderer.snapshot()
        renderer.addConsole(console, true, false, metaData)

        when:
        renderer.onOutput(start(loggingHeader: 'description', buildOperationId: 1L, buildOperationCategory: BuildOperationCategory.TASK))
        renderer.onOutput(event('info', LogLevel.INFO, 1L))
        renderer.onOutput(event('error', LogLevel.ERROR, 1L))
        renderer.onOutput(complete('status'))
        renderer.restore(snapshot) // close console to flush

        then:
        console.buildOutputArea.toString().readLines() == ['', '{header}> description{normal}', 'info']
    }

    def rendersLogEventsWhenOnlyStdErrIsConsole() {
        def snapshot = renderer.snapshot()
        renderer.addConsole(console, false, true, metaData)

        when:
        renderer.onOutput(start('description'))
        renderer.onOutput(event('info', LogLevel.INFO))
        renderer.onOutput(event('error', LogLevel.ERROR))
        renderer.onOutput(complete('status'))
        renderer.restore(snapshot) // close console to flush

        then:
        console.buildOutputArea.toString().readLines() == ['{error}error', '{normal}']
    }

    def rendersLogEventsInConsoleWhenLogLevelIsDebug() {
        renderer.configure(LogLevel.DEBUG)
        def snapshot = renderer.snapshot()
        renderer.addConsole(console, true, true, metaData)

        when:
        renderer.onOutput(event(tenAm, 'info', LogLevel.INFO))
        renderer.onOutput(event(tenAm, 'error', LogLevel.ERROR))
        renderer.restore(snapshot) // close console to flush

        then:
        console.buildOutputArea.toString().readLines() == ['10:00:00.000 [INFO] [category] info', '{error}10:00:00.000 [ERROR] [category] error', '{normal}']
    }

    def attachesConsoleWhenStdOutAndStdErrAreAttachedToConsole() {
        when:
        renderer.attachSystemOutAndErr()
        def snapshot = renderer.snapshot()
        renderer.addConsole(console, true, true, metaData)
        renderer.onOutput(event('info', LogLevel.INFO))
        renderer.onOutput(event('error', LogLevel.ERROR))
        renderer.restore(snapshot) // close console to flush

        then:
        console.buildOutputArea.toString().readLines() == ['info', '{error}error', '{normal}']
        ConcurrentTestUtil.poll(1, 0.1) {
            assert outputs.stdOut == ''
            assert outputs.stdErr == ''
        }
    }

    def attachesConsoleWhenOnlyStdOutIsAttachedToConsole() {
        when:
        renderer.attachSystemOutAndErr()
        def snapshot = renderer.snapshot()
        renderer.addConsole(console, true, false, metaData)
        renderer.onOutput(event('info', LogLevel.INFO))
        renderer.onOutput(event('error', LogLevel.ERROR))
        renderer.restore(snapshot) // close console to flush

        then:
        console.buildOutputArea.toString().readLines() == ['info']
        ConcurrentTestUtil.poll(1, 0.1) {
            assert outputs.stdOut == ''
            assert outputs.stdErr.readLines() == ['error']
        }
    }

    def attachesConsoleWhenOnlyStdErrIsAttachedToConsole() {
        when:
        renderer.attachSystemOutAndErr()
        def snapshot = renderer.snapshot()
        renderer.addConsole(console, false, true, metaData)
        renderer.onOutput(event('info', LogLevel.INFO))
        renderer.onOutput(event('error', LogLevel.ERROR))
        renderer.restore(snapshot) // close console to flush

        then:
        console.buildOutputArea.toString().readLines() == ['{error}error', '{normal}']
        ConcurrentTestUtil.poll(1, 0.1) {
            assert outputs.stdOut.readLines() == ['info']
            assert outputs.stdErr == ''
        }
    }

    private static void awaitEventQueueFlush() {
        Thread.sleep(ThrottlingOutputEventListener.EVENT_QUEUE_FLUSH_PERIOD_MS * 2)
    }
}

class TestListener implements StandardOutputListener {
    private final StringWriter writer = new StringWriter();

    def getValue() {
        return writer.toString()
    }

    public void onOutput(CharSequence output) {
        writer.append(output);
    }
}

