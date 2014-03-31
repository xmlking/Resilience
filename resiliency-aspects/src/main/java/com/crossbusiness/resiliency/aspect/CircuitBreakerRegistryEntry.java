/**
 * The MIT License (MIT)
 *
 * Copyright (c)  2014 CrossBusiness, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.crossbusiness.resiliency.aspect;

import java.util.ArrayList;
import java.util.List;


/**
 * @author Sumanth Chinthagunta <xmlking@gmail.com>
 *
 */
public class CircuitBreakerRegistryEntry {

        private final String name;

        private final int failureThreshold;

        private final long failureThresholdTimeFrameMs;

        private final long retryAfterMs;
        
        private final List<Class<? extends Throwable>> failureIndications;

        private CircuitBreakerStatus status;

        private ArrayList<Long> failures;

        private long lastOpenedTime;

        // counter for the closed cycles. needed to detect if the failure corresponds to the current closed cycle or this failure is a result of 
        // a long running method started in previous closed cycle.
        private long closedCycleCounter = 0;

        public CircuitBreakerRegistryEntry(String name, int failureThreshold, long failureThresholdTimeFrameMs, long retryAfterMs, List<Class<? extends Throwable>> failureIndications) {
                this.name = name;
                this.failureThreshold = failureThreshold;
                this.failureThresholdTimeFrameMs = failureThresholdTimeFrameMs;
                this.retryAfterMs = retryAfterMs;
                this.failureIndications = failureIndications;
                this.failures = new  ArrayList<Long>();
                this.status = CircuitBreakerStatus.CLOSED;
        }

        public CircuitBreakerStatus getStatus() {
                return status;
        }

        public void setStatus(CircuitBreakerStatus status) {
                this.status = status;
        }

        public ArrayList<Long> getFailures() {
                return failures;
        }

        public void setFailures(ArrayList<Long> failures) {
                this.failures = failures;
        }

        public String getName() {
                return name;
        }

        public int getFailureThreshold() {
                return failureThreshold;
        }

        public long getFailureThresholdTimeFrameMs() {
                return failureThresholdTimeFrameMs;
        }

        public long getRetryAfterMs() {
                return retryAfterMs;
        }

        public List<Class<? extends Throwable>> getFailureIndications() {
                return failureIndications;
        }

        public void increaseClosedCycleCounter() {
                this.closedCycleCounter = closedCycleCounter + 1;
        }

        public long getClosedCycleCounter() {
                return closedCycleCounter;
        }

        public long getLastOpenedTime() {
                return lastOpenedTime;
        }

        public void setLastOpenedTime(long lastOpenedTime) {
                this.lastOpenedTime = lastOpenedTime;
        }

}