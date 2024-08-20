/*-
 * ========================LICENSE_START=================================
 * Bucket4j
 * %%
 * Copyright (C) 2015 - 2020 Vladimir Bukhtoyarov
 * %%
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
 * =========================LICENSE_END==================================
 */

package io.github.bucket4j.distributed.proxy.synchronization.per_bucket.batch;

import java.util.Objects;

import io.github.bucket4j.distributed.proxy.AsyncCommandExecutor;
import io.github.bucket4j.distributed.proxy.CommandExecutor;
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.BucketSynchronization;
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.BucketSynchronizationListener;

/**
 * Synchronization that combines independent requests to same bucket into batches in order to reduce request count to remote storage.
 */
public class BatchingBucketSynchronization implements BucketSynchronization {

    private final BucketSynchronizationListener listener;

    public BatchingBucketSynchronization(BucketSynchronizationListener listener) {
        this.listener = Objects.requireNonNull(listener);
    }

    @Override
    public BucketSynchronization withListener(BucketSynchronizationListener listener) {
        Objects.requireNonNull(listener);
        return new BatchingBucketSynchronization(listener);
    }

    @Override
    public CommandExecutor apply(CommandExecutor originalExecutor) {
        return new BatchingExecutor(originalExecutor, listener);
    }

    @Override
    public AsyncCommandExecutor apply(AsyncCommandExecutor originalExecutor) {
        return new AsyncBatchingExecutor(originalExecutor, listener);
    }

}
