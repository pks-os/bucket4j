package io.github.bucket4j.distributed.proxy.synchronization

import io.github.bucket4j.Bucket
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.TokensInheritanceStrategy
import io.github.bucket4j.distributed.AsyncBucketProxy
import io.github.bucket4j.distributed.BucketProxy
import io.github.bucket4j.distributed.proxy.AsyncCommandExecutor
import io.github.bucket4j.distributed.proxy.CommandExecutor
import io.github.bucket4j.distributed.proxy.ProxyManagerConfig
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.*
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.delay.DelayBucketSynchronization
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.manual.ManuallySyncingBucketSynchronization
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.predictive.PredictiveBucketSynchronization
import io.github.bucket4j.distributed.proxy.synchronization.per_bucket.skiponzero.SkipSyncOnZeroBucketSynchronization
import io.github.bucket4j.distributed.remote.CommandResult
import io.github.bucket4j.distributed.remote.RemoteCommand
import io.github.bucket4j.distributed.remote.Request
import io.github.bucket4j.distributed.remote.commands.CheckConfigurationVersionAndExecuteCommand
import io.github.bucket4j.distributed.remote.commands.CreateInitialStateWithVersionOrReplaceConfigurationAndExecuteCommand
import io.github.bucket4j.distributed.remote.commands.GetAvailableTokensCommand
import io.github.bucket4j.distributed.remote.commands.SyncCommand
import io.github.bucket4j.distributed.versioning.Versions
import io.github.bucket4j.mock.*
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier

class BucketSynchronizationCornerCasesSpecification extends Specification {

    private static final DelayParameters delayParameters = new DelayParameters(1, Duration.ofNanos(1))

    @Shared
    private TimeMeterMock clock = new TimeMeterMock()


    // https://github.com/bucket4j/bucket4j/issues/398
    @Unroll
    def "should correctly handle exceptions when synchronization is used #testNumber ProxyManagerMock"(int testNumber, BucketSynchronization synchronization) {
        setup:
            ProxyManagerMock proxyManagerMock = new ProxyManagerMock(clock)
            BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit({it.capacity(10).refillGreedy(10, Duration.ofSeconds (1))})
                .build()

            BucketProxy bucket = proxyManagerMock.builder()
                    .withSynchronization(synchronization)
                    .build("66", () -> configuration)
        when:
            bucket.getAvailableTokens() == 10
            for (int i = 0; i < 5; i++) {
                assert bucket.tryConsume(1)
            }
            proxyManagerMock.removeProxy("66")

        then:
            bucket.forceAddTokens(90)
            bucket.getAvailableTokens() == 100

        when:
            proxyManagerMock.removeProxy("66")
        then:
            bucket.asVerbose().forceAddTokens(90)
            bucket.asVerbose().getAvailableTokens().getValue() == 100

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
            CompareAndSwapBasedProxyManagerMock proxyManagerMock = new CompareAndSwapBasedProxyManagerMock(ProxyManagerConfig.default.withClientClock(clock))
            BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit({it.capacity(10).refillGreedy(10, Duration.ofSeconds (1))})
                .build()

            BucketProxy bucket = proxyManagerMock.builder()
                    .withSynchronization(synchronization)
                    .build("66", () -> configuration)
        when:
            bucket.getAvailableTokens() == 10
            for (int i = 0; i < 5; i++) {
                assert bucket.tryConsume(1)
            }
            proxyManagerMock.removeProxy("66")

        then:
            bucket.forceAddTokens(90)
            bucket.getAvailableTokens() == 100

        when:
            proxyManagerMock.removeProxy("66")
        then:
            bucket.asVerbose().forceAddTokens(90)
            bucket.asVerbose().getAvailableTokens().getValue() == 100

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
    def "should correctly handle exceptions when synchronization is used #testNumber LockBasedProxyManagerMock"(int testNumber, BucketSynchronization synchronization) {
        setup:
            LockBasedProxyManagerMock proxyManagerMock = new LockBasedProxyManagerMock(ProxyManagerConfig.default.withClientClock(clock))
            BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit({it.capacity(10).refillGreedy(10, Duration.ofSeconds (1))})
                .build()

            BucketProxy bucket = proxyManagerMock.builder()
                    .withSynchronization(synchronization)
                    .build("66", () -> configuration)
        when:
            bucket.getAvailableTokens() == 10
            for (int i = 0; i < 5; i++) {
                assert bucket.tryConsume(1)
            }
            proxyManagerMock.removeProxy("66")

        then:
            bucket.forceAddTokens(90)
            bucket.getAvailableTokens() == 100

        when:
            proxyManagerMock.removeProxy("66")
        then:
            bucket.asVerbose().forceAddTokens(90)
            bucket.asVerbose().getAvailableTokens().getValue() == 100

        where:
            [testNumber, synchronization] << [
                    [1, BucketSynchronizations.batching()],
                    [2, new DelayBucketSynchronization(delayParameters, NopeBucketSynchronizationListener.INSTANCE, clock)],
                    [3, new PredictiveBucketSynchronization(PredictionParameters.createDefault(delayParameters), delayParameters, NopeBucketSynchronizationListener.INSTANCE, clock)],
                    [4, new SkipSyncOnZeroBucketSynchronization(NopeBucketSynchronizationListener.INSTANCE, clock)],
                    [5, new ManuallySyncingBucketSynchronization(NopeBucketSynchronizationListener.INSTANCE, clock)]
            ]
    }

    @Unroll
    def "should correctly handle exceptions when synchronization is used #testNumber SelectForUpdateBasedProxyManagerMock"(int testNumber, BucketSynchronization synchronization) {
        setup:
            SelectForUpdateBasedProxyManagerMock proxyManagerMock = new SelectForUpdateBasedProxyManagerMock(ProxyManagerConfig.default.withClientClock(clock))
            BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit({it.capacity(10).refillGreedy(10, Duration.ofSeconds (1))})
                .build()

            BucketProxy bucket = proxyManagerMock.builder()
                .withSynchronization(synchronization)
                .build("66", () -> configuration)
        when:
            bucket.getAvailableTokens() == 10
            for (int i = 0; i < 5; i++) {
                assert bucket.tryConsume(1)
            }
            proxyManagerMock.removeProxy("66")

        then:
            bucket.forceAddTokens(90)
            bucket.getAvailableTokens() == 100

        when:
            proxyManagerMock.removeProxy("66")
        then:
            bucket.asVerbose().forceAddTokens(90)
            bucket.asVerbose().getAvailableTokens().getValue() == 100

        where:
            [testNumber, synchronization] << [
                    [1, BucketSynchronizations.batching()],
                    [2, new DelayBucketSynchronization(delayParameters, NopeBucketSynchronizationListener.INSTANCE, clock)],
                    [3, new PredictiveBucketSynchronization(PredictionParameters.createDefault(delayParameters), delayParameters, NopeBucketSynchronizationListener.INSTANCE, clock)],
                    [4, new SkipSyncOnZeroBucketSynchronization(NopeBucketSynchronizationListener.INSTANCE, clock)],
                    [5, new ManuallySyncingBucketSynchronization(NopeBucketSynchronizationListener.INSTANCE, clock)]
            ]
    }

    @Unroll
    def "#testNumber implicit configuration replacement case for version increment"(int testNumber, BucketSynchronization synchronization) {
        when:
            ProxyManagerMock proxyManagerMock = new ProxyManagerMock(clock)

            int KEY = 42
            int PREVIOUS_VERSION = 1
            Bucket bucket10 = proxyManagerMock.builder()
                .withSynchronization(synchronization)
                .withImplicitConfigurationReplacement(PREVIOUS_VERSION, TokensInheritanceStrategy.RESET)
                .build(KEY, () -> BucketConfiguration.builder()
                    .addLimit({limit -> limit.capacity(10).refillGreedy(10, Duration.ofSeconds(1))})
                    .build())

            // persist bucket with previous version
            bucket10.tryConsumeAsMuchAsPossible()

            CommandExecutor executor = synchronization.apply(new CommandExecutor() {
                @Override
                CommandResult execute(RemoteCommand command) {
                    Request request = new Request(command, Versions.latest, clock.currentTimeNanos(), null)
                    return proxyManagerMock.execute(KEY, request)
                }
            })
            // emulate case where two command in parallel detects that config needs to be replaced
            RemoteCommand getTokensCommand = new GetAvailableTokensCommand()
            CommandResult getTokensResult = executor.execute(new CheckConfigurationVersionAndExecuteCommand<>(getTokensCommand, PREVIOUS_VERSION + 1))
            RemoteCommand syncCommand = new SyncCommand(1, 1_000_000)
            CommandResult syncResult = executor.execute(new CheckConfigurationVersionAndExecuteCommand<>(syncCommand, PREVIOUS_VERSION + 1))

            // then wrap original commands by CreateInitialStateWithVersionOrReplaceConfigurationAndExecuteCommand and repeat
            BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit({limit -> limit.capacity(100).refillGreedy(10, Duration.ofSeconds(1))})
                .build()
            CommandResult syncResult2 = executor.execute(new CreateInitialStateWithVersionOrReplaceConfigurationAndExecuteCommand<>(configuration, syncCommand, PREVIOUS_VERSION + 1, TokensInheritanceStrategy.RESET))
            CommandResult getTokensResult2 = executor.execute(new CreateInitialStateWithVersionOrReplaceConfigurationAndExecuteCommand<>(configuration, getTokensCommand, PREVIOUS_VERSION + 1, TokensInheritanceStrategy.RESET))
        then:
            getTokensResult.isConfigurationNeedToBeReplaced()
            syncResult.isConfigurationNeedToBeReplaced()
            !syncResult2.isError()
            getTokensResult2.getData() == 100L

        where:
            [testNumber, synchronization] << [
                    [1, BucketSynchronizations.batching()],
                    [2, new DelayBucketSynchronization(delayParameters, NopeBucketSynchronizationListener.INSTANCE, clock)],
                    [3, new PredictiveBucketSynchronization(PredictionParameters.createDefault(delayParameters), delayParameters, NopeBucketSynchronizationListener.INSTANCE, clock)],
                    [4, new SkipSyncOnZeroBucketSynchronization(NopeBucketSynchronizationListener.INSTANCE, clock)],
                    [5, new ManuallySyncingBucketSynchronization(NopeBucketSynchronizationListener.INSTANCE, clock)]
            ]
    }

    @Unroll
    def "#testNumber implicit configuration replacement case for version increment - Async"(int testNumber, BucketSynchronization synchronization) {
        when:
            AsyncProxyManagerMock proxyManagerMock = new AsyncProxyManagerMock(clock)

            int KEY = 42
            int PREVIOUS_VERSION = 1
            Supplier<CompletableFuture<BucketConfiguration>> configSupplier = () -> CompletableFuture.completedFuture(
                BucketConfiguration.builder()
                    .addLimit({limit -> limit.capacity(10).refillGreedy(10, Duration.ofSeconds(1))})
                    .build()
            )
            AsyncBucketProxy bucket10 = proxyManagerMock.builder()
                    .withSynchronization(synchronization)
                    .withImplicitConfigurationReplacement(PREVIOUS_VERSION, TokensInheritanceStrategy.RESET)
                    .build(KEY, configSupplier)

            // persist bucket with previous version
            bucket10.tryConsumeAsMuchAsPossible().get()

            AsyncCommandExecutor executor = synchronization.apply (new AsyncCommandExecutor() {
                @Override
                CompletableFuture<CommandResult> executeAsync(RemoteCommand command) {
                    Request request = new Request(command, Versions.latest, clock.currentTimeNanos(), null)
                    return proxyManagerMock.executeAsync(KEY, request)
                }
            })
            // emulate case where two command in parallel detects that config needs to be replaced
            RemoteCommand getTokensCommand = new GetAvailableTokensCommand()
            CommandResult getTokensResult = executor.executeAsync(new CheckConfigurationVersionAndExecuteCommand<>(getTokensCommand, PREVIOUS_VERSION + 1)).get()
            RemoteCommand syncCommand = new SyncCommand(1, 1_000_000)
            CommandResult syncResult = executor.executeAsync(new CheckConfigurationVersionAndExecuteCommand<>(syncCommand, PREVIOUS_VERSION + 1)).get()

            // then wrap original commands by CreateInitialStateWithVersionOrReplaceConfigurationAndExecuteCommand and repeat
            BucketConfiguration configuration = BucketConfiguration.builder()
                    .addLimit({limit -> limit.capacity(100).refillGreedy(10, Duration.ofSeconds(1))})
                    .build()
            CommandResult syncResult2 = executor.executeAsync(new CreateInitialStateWithVersionOrReplaceConfigurationAndExecuteCommand<>(configuration, syncCommand, PREVIOUS_VERSION + 1, TokensInheritanceStrategy.RESET)).get()
            CommandResult getTokensResult2 = executor.executeAsync(new CreateInitialStateWithVersionOrReplaceConfigurationAndExecuteCommand<>(configuration, getTokensCommand, PREVIOUS_VERSION + 1, TokensInheritanceStrategy.RESET)).get()
        then:
            getTokensResult.isConfigurationNeedToBeReplaced()
            syncResult.isConfigurationNeedToBeReplaced()
            !syncResult2.isError()
            getTokensResult2.getData() == 100L

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
