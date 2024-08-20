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

package io.github.bucket4j.grid.ignite.thin.compute;

import java.util.concurrent.CompletableFuture;

import org.apache.ignite.client.ClientCache;
import org.apache.ignite.client.ClientCompute;
import org.apache.ignite.client.IgniteClientFuture;

import io.github.bucket4j.distributed.proxy.AbstractAsyncProxyManager;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.Request;
import io.github.bucket4j.distributed.versioning.Version;
import io.github.bucket4j.grid.ignite.thin.Bucket4jIgniteThin.IgniteAsyncThinClientComputeProxyManagerBuilder;
import io.github.bucket4j.grid.ignite.thin.ThinClientUtils;

import static io.github.bucket4j.distributed.serialization.InternalSerializationHelper.deserializeResult;


/**
 * The extension of Bucket4j library addressed to support <a href="https://ignite.apache.org/">Apache ignite</a> in-memory computing platform.
 */
public class IgniteAsyncThinClientProxyManager<K> extends AbstractAsyncProxyManager<K> {

    private final ClientCache<K, byte[]> cache;
    private final ClientCompute clientCompute;

    public IgniteAsyncThinClientProxyManager(IgniteAsyncThinClientComputeProxyManagerBuilder<K> builder) {
        super(builder.getProxyManagerConfig());
        cache = builder.getCache();
        clientCompute = builder.getClientCompute();
    }

    @Override
    public <T> CompletableFuture<CommandResult<T>> executeAsync(K key, Request<T> request) {
        IgniteEntryProcessor<K> entryProcessor = new IgniteEntryProcessor<>(request);
        Bucket4jComputeTaskParams<K> taskParams = new Bucket4jComputeTaskParams<>(cache.getName(), key, entryProcessor);

        IgniteClientFuture<byte[]> igniteFuture = clientCompute.executeAsync2(Bucket4jComputeTask.JOB_NAME, taskParams);
        CompletableFuture<byte[]> completableFuture = ThinClientUtils.convertFuture(igniteFuture);
        Version backwardCompatibilityVersion = request.getBackwardCompatibilityVersion();
        return completableFuture.thenApply((byte[] resultBytes) -> deserializeResult(resultBytes, backwardCompatibilityVersion));
    }

    @Override
    public boolean isExpireAfterWriteSupported() {
        return false;
    }

    @Override
    protected CompletableFuture<Void> removeAsync(K key) {
        IgniteClientFuture<Boolean> igniteFuture = cache.removeAsync(key);
        return ThinClientUtils.convertFuture(igniteFuture).thenApply(result -> null);
    }

}
