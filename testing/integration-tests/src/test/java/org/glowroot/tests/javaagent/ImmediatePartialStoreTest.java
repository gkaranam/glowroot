/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.glowroot.tests.javaagent;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.glowroot.container.AppUnderTest;
import org.glowroot.container.Container;
import org.glowroot.container.TempDirs;
import org.glowroot.container.TraceMarker;
import org.glowroot.container.config.AdvancedConfig;
import org.glowroot.container.impl.JavaagentContainer;
import org.glowroot.container.impl.LocalContainer;
import org.glowroot.container.trace.Trace;

import static org.assertj.core.api.Assertions.assertThat;

public class ImmediatePartialStoreTest {

    private static File dataDir;
    private static Container container;

    @BeforeClass
    public static void setUp() throws Exception {
        dataDir = TempDirs.createTempDir("glowroot-test-datadir");
        container = JavaagentContainer.createWithFileDb(dataDir);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        container.close();
        TempDirs.deleteRecursively(dataDir);
    }

    @After
    public void afterEachTest() throws Exception {
        container.checkAndReset();
    }

    @Test
    public void shouldReadImmediatePartialStoreTrace() throws Exception {
        // given
        AdvancedConfig advancedConfig = container.getConfigService().getAdvancedConfig();
        advancedConfig.setImmediatePartialStoreThresholdSeconds(1);
        container.getConfigService().updateAdvancedConfig(advancedConfig);
        // when
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                try {
                    container.executeAppUnderTest(ShouldGenerateActiveTrace.class);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                return null;
            }
        });
        // give time for partial store to occur
        // (this has been source of sporadic failures on slow travis builds)
        Thread.sleep(5000);
        // interrupt trace which will then call System.exit() to kill jvm without completing trace
        container.interruptAppUnderTest();
        ((JavaagentContainer) container).cleanup();
        // give jvm a second to shut down
        Thread.sleep(1000);
        container = LocalContainer.createWithFileDb(dataDir);
        // then
        Trace trace = container.getTraceService().getLastTrace();
        assertThat(trace).isNotNull();
        assertThat(trace.isActive()).isFalse();
        assertThat(trace.isPartial()).isTrue();
        // cleanup
        executorService.shutdown();
    }

    public static class ShouldGenerateActiveTrace implements AppUnderTest, TraceMarker {
        @Override
        public void executeApp() throws InterruptedException {
            traceMarker();
        }
        @Override
        public void traceMarker() throws InterruptedException {
            try {
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException e) {
                System.exit(123);
            }
        }
    }
}
