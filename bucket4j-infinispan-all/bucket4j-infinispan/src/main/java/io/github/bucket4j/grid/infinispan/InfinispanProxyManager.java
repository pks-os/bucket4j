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
/*
 *
 * Copyright 2015-2018 Vladimir Bukhtoyarov
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */

package io.github.bucket4j.grid.infinispan;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.infinispan.functional.FunctionalMap.ReadWriteMap;

import io.github.bucket4j.BucketExceptions;
import io.github.bucket4j.distributed.proxy.AbstractProxyManager;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.Request;

import static io.github.bucket4j.distributed.serialization.InternalSerializationHelper.deserializeResult;

/**
 * The extension of Bucket4j library addressed to support <a href="https://infinispan.org/">Infinispan</a> in-memory computing platform.
 */
public class InfinispanProxyManager<K> extends AbstractProxyManager<K> {

    private final InfinispanProcessor<K, Void> REMOVE_BUCKET_ENTRY_PROCESSOR = new InfinispanProcessor<>(new byte[0]);
    private final ReadWriteMap<K, byte[]> readWriteMap;

    InfinispanProxyManager(Bucket4jInfinispan.InfinispanProxyManagerBuilder<K> builder) {
        super(builder.getProxyManagerConfig());
        this.readWriteMap = builder.readWriteMap;
    }

    @Override
    public <T> CommandResult<T> execute(K key, Request<T> request) {
        InfinispanProcessor<K, T> entryProcessor = new InfinispanProcessor<>(request);
        try {
            CompletableFuture<byte[]> resultFuture = readWriteMap.eval(key, entryProcessor);
            return (CommandResult<T>) resultFuture.thenApply(resultBytes -> deserializeResult(resultBytes, request.getBackwardCompatibilityVersion())).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new BucketExceptions.BucketExecutionException(e);
        }
    }

    @Override
    public void removeProxy(K key) {
        try {
            readWriteMap.eval(key, REMOVE_BUCKET_ENTRY_PROCESSOR).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new BucketExceptions.BucketExecutionException(e);
        }
    }

    @Override
    public boolean isExpireAfterWriteSupported() {
        return true;
    }

}
