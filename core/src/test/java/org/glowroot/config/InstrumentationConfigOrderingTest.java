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
package org.glowroot.config;

import com.google.common.collect.Ordering;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class InstrumentationConfigOrderingTest {

    private final InstrumentationConfig left = InstrumentationConfig.builder()
            .className("a")
            .methodName("n")
            .addMethodParameterTypes("java.lang.String")
            .methodReturnType("")
            .captureKind(CaptureKind.TIMER)
            .timerName("t")
            .traceEntryTemplate("")
            .traceEntryCaptureSelfNested(false)
            .transactionType("")
            .transactionNameTemplate("")
            .transactionUserTemplate("")
            .enabledProperty("")
            .traceEntryEnabledProperty("")
            .build();

    private final InstrumentationConfig right = InstrumentationConfig.builder()
            .className("b")
            .methodName("m")
            .methodReturnType("")
            .captureKind(CaptureKind.TIMER)
            .timerName("t")
            .traceEntryTemplate("")
            .traceEntryCaptureSelfNested(false)
            .transactionType("")
            .transactionNameTemplate("")
            .transactionUserTemplate("")
            .enabledProperty("")
            .traceEntryEnabledProperty("")
            .build();

    @Test
    public void testDifferentClassNames() {
        // given
        Ordering<InstrumentationConfig> ordering = InstrumentationConfigBase.ordering;
        // when
        int compare = ordering.compare(left, right);
        // then
        assertThat(compare).isNegative();
    }

    @Test
    public void testSameClassNames() {
        // given
        Ordering<InstrumentationConfig> ordering = InstrumentationConfigBase.ordering;
        // when
        int compare = ordering.compare(left, right.withClassName("a"));
        // then
        assertThat(compare).isPositive();
    }

    @Test
    public void testSameClassAndMethodNames() {
        // given
        Ordering<InstrumentationConfig> ordering = InstrumentationConfigBase.ordering;
        // when
        int compare = ordering.compare(left.withMethodName("m"), right.withClassName("a"));
        // then
        assertThat(compare).isPositive();
    }

    @Test
    public void testSameClassAndMethodNamesAndParamCount() {
        // given
        Ordering<InstrumentationConfig> ordering = InstrumentationConfigBase.ordering;
        // when
        int compare = ordering.compare(left.withMethodName("m"),
                right.withClassName("a").withMethodParameterTypes("java.lang.Throwable"));
        // then
        assertThat(compare).isNegative();
    }

    @Test
    public void testSameEverything() {
        // given
        Ordering<InstrumentationConfig> ordering = InstrumentationConfigBase.ordering;
        // when
        int compare = ordering.compare(left.withMethodName("m"),
                right.withClassName("a").withMethodParameterTypes("java.lang.String"));
        // then
        assertThat(compare).isZero();
    }
}
