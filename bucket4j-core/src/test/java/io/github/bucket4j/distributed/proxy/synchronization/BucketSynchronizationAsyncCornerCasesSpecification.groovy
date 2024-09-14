package io.github.bucket4j.distributed.proxy.synchronization

import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.distributed.AsyncBucketProxy
import io.github.bucket4j.distributed.proxy.AsyncProxyManagerConfig
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.*
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.delay.DelayBucketSynchronization
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.manual.ManuallySyncingBucketSynchronization
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.predictive.PredictiveBucketSynchronization
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.skiponzero.SkipSyncOnZeroBucketSynchronization
import io.github.bucket4j.mock.AsyncCompareAndSwapBasedProxyManagerMock
import io.github.bucket4j.mock.AsyncProxyManagerMock
import io.github.bucket4j.mock.TimeMeterMock
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration
import java.util.concurrent.CompletableFuture

class BucketSynchronizationAsyncCornerCasesSpecification extends Specification {

    private static final DelayParameters delayParameters = new DelayParameters(1, Duration.ofNanos(1));

    @Shared
    private TimeMeterMock clock = new TimeMeterMock()


    // https://github.com/bucket4j/bucket4j/issues/398
    @Unroll
    def "should correctly handle exceptions when synchronization is used #testNumber ProxyManagerMock"(int testNumber, BucketSynchronization synchronization) {
        setup:
            AsyncProxyManagerMock proxyManagerMock = new AsyncProxyManagerMock(clock)
            BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit({it.capacity(10).refillGreedy(10, Duration.ofSeconds (1))})
                .build()

            AsyncBucketProxy bucket = proxyManagerMock.builder()
                    .withSynchronization(synchronization)
                    .build("66", {CompletableFuture.completedFuture(configuration)})
        when:
            bucket.getAvailableTokens().get() == 10
            for (int i = 0; i < 5; i++) {
                assert bucket.tryConsume(1).get()
            }
            proxyManagerMock.removeProxy("66")

        then:
            bucket.forceAddTokens(90).get() == null
            bucket.getAvailableTokens().get() == 100

        when:
            proxyManagerMock.removeProxy("66")
        then:
            bucket.asVerbose().forceAddTokens(90).get()
            bucket.asVerbose().getAvailableTokens().get().getValue() == 100

        where:
            [testNumber, synchronization] << [
                    [1, BucketSynchronizations.batching()],
                    [2, new DelayBucketSynchronization(delayParameters, NopeBucketSynchronizationListener.INSTANCE, clock)],
                    [3, new PredictiveBucketSynchronization(PredictionParameters.createDefault(delayParameters), delayParameters, NopeBucketSynchronizationListener.INSTANCE, clock)],
                    [4, new SkipSyncOnZeroBucketSynchronization(NopeBucketSynchronizationListener.INSTANCE, clock)],
                    [5, new ManuallySyncingBucketSynchronization(NopeBucketSynchronizationListener.INSTANCE, clock)]
        ]
    }

    // https://github.com/bucket4j/bucket4j/issues/398
    @Unroll
    def "should correctly handle exceptions when synchronization is used #testNumber CompareAndSwapBasedProxyManagerMock"(int testNumber, BucketSynchronization synchronization) {
        setup:
            AsyncCompareAndSwapBasedProxyManagerMock proxyManagerMock = new AsyncCompareAndSwapBasedProxyManagerMock(AsyncProxyManagerConfig.default.withClientClock(clock))
            BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit({it.capacity(10).refillGreedy(10, Duration.ofSeconds (1))})
                .build()

            AsyncBucketProxy bucket = proxyManagerMock.builder()
                    .withSynchronization(synchronization)
                    .build("66", () -> CompletableFuture.completedFuture(configuration))
        when:
            bucket.getAvailableTokens().get() == 10
            for (int i = 0; i < 5; i++) {
                assert bucket.tryConsume(1).get()
            }
            proxyManagerMock.removeProxy("66")

        then:
            bucket.forceAddTokens(90).get() == null
            bucket.getAvailableTokens().get() == 100

        when:
            proxyManagerMock.removeProxy("66")
        then:
            bucket.asVerbose().forceAddTokens(90).get()
            bucket.asVerbose().getAvailableTokens().get().getValue() == 100

        where:
        [testNumber, synchronization] << [
                [1, BucketSynchronizations.batching()],
                [2, new DelayBucketSynchronization(delayParameters, NopeBucketSynchronizationListener.INSTANCE, clock)],
                [3, new PredictiveBucketSynchronization(PredictionParameters.createDefault(delayParameters), delayParameters, NopeBucketSynchronizationListener.INSTANCE, clock)],
                [4, new SkipSyncOnZeroBucketSynchronization(NopeBucketSynchronizationListener.INSTANCE, clock)],
                [5, new ManuallySyncingBucketSynchronization(NopeBucketSynchronizationListener.INSTANCE, clock)]
        ]
    }

}
