# Infinispan integration
Before use ``bucket4j-infinispan`` module please read [bucket4j-jcache documentation](jcache-usage.md),
because ``bucket4j-infinispan`` is just a follow-up of ``bucket4j-jcache``.

**Question:** Bucket4j already supports JCache since version ``1.2``. Why it was needed to introduce direct support for ``Infinispan``?  
**Answer:** When you want to use Bucket4j together with Infinispan, you must always use ``bucket4j-infinispan`` module instead of ``bucket4j-jcache``,   
because Infinispan does not provide mutual exclusion for entry-processors. Any attempt to use Infinispan via ``bucket4j-jcache`` will be failed with UnsupportedOperationException exception
at bucket construction time.


## Dependencies
To use ``bucket4j-infinispan`` with ``Infinispan 9.x, 10.x`` extension you need to add following dependency:
``xml
<dependency>
    <groupId>com.github.vladimir-bukhtoyarov</groupId>
    <artifactId>bucket4j-infinispan</artifactId>
    <version>${bucket4j.version}</version>
</dependency>
``
If you are using legacy version of Infinispan ``8.x`` then you need to add following dependency:
``xml
<dependency>
    <groupId>com.github.vladimir-bukhtoyarov</groupId>
    <artifactId>bucket4j-infinispan-8</artifactId>
    <version>${bucket4j.version}</version>
</dependency>
``
## General compatibility matrix principles::
* Bucket4j authors do not perform continues monitoring of new Infinispan releases. So, there is can be case when there is no one version of Bucket4j which is compatible with newly released Infinispan,
just log issue to [bug tracker](https://github.com/vladimir-bukhtoyarov/bucket4j/issues) in this case, adding support to new version of Infinispan is usually easy exercise. 
* Integrations with legacy versions of Infinispan are not removed without a clear reason. Hence You are in safety, even you are working in a big enterprise company that does not update its infrastructure frequently because You still get new Bucket4j's features even for legacy Infinispan's releases.


## Special notes for Infinispan 10.0+
As mentioned in the [Infinispan Marshalling documentation](https://infinispan.org/docs/dev/titles/developing/developing.html#marshalling), since release ``10.0.0`` Infinispan does not allow deserialization of custom payloads into Java classes. 
If you do not configure serialization(as described bellow), you will get the error like this on any attempt to use Bucket4j with brand new Infinispan release:
``bash
Jan 02, 2020 4:57:56 PM org.infinispan.marshall.persistence.impl.PersistenceMarshallerImpl objectToBuffer
WARN: ISPN000559: Cannot marshall 'class io.github.bucket4j.grid.infinispan.SerializableFunctionAdapter'
java.lang.IllegalArgumentException: No marshaller registered for Java type io.github.bucket4j.grid.infinispan.SerializableFunctionAdapter
	at org.infinispan.protostream.impl.SerializationContextImpl.getMarshallerDelegate(SerializationContextImpl.java:279)
	at org.infinispan.protostream.WrappedMessage.writeMessage(WrappedMessage.java:240)
	at org.infinispan.protostream.ProtobufUtil.toWrappedStream(ProtobufUtil.java:196)
``
There are three options to solve this problem:
* Configure Jboss marshalling instead of default ProtoStream marshaller as described [there](https://infinispan.org/docs/dev/titles/developing/developing.html#jboss_marshalling).
* Configure Java Serialization Marshaller instead of default ProtoStream marshaller, as described [there](https://infinispan.org/docs/dev/titles/developing/developing.html#java_serialization_marshaller).
Do not forget to add ``io.github.bucket4j.*`` regexp to the whitelist if choosing this way.
* And last way(recommended) just register ``Bucket4j serialization context initializer`` in the serialization configuration. 
You can do it in both programmatically and declarative ways:

*Programmatic registration of Bucket4jProtobufContextInitializer*
``java
import io.github.bucket4j.grid.infinispan.serialization.Bucket4jProtobufContextInitializer;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
...
GlobalConfigurationBuilder builder = new GlobalConfigurationBuilder();
builder.serialization().addContextInitializer(new Bucket4jProtobufContextInitializer());
``

*Declarative registration of Bucket4jProtobufContextInitializer*
``xml
<serialization>
    <context-initializer class="io.github.bucket4j.grid.infinispan.serialization.Bucket4jProtobufContextInitializer"/>
</serialization>
``
And that is all. Just registering ``Bucket4jProtobufContextInitializer`` in any way is enough to make Bucket4j compatible with ProtoStream marshaller, you do not have to care about *.proto files, annotations, whitelist etc,
all neccessary Protobuffers configs generated by ``Bucket4jProtobufContextInitializer`` and registerd on the fly.

## Example of Bucket instantiation
``java
org.infinispan.functional.FunctionalMap.ReadWriteMap<K, GridBucketState> map = ...;
...

Bucket bucket = Bucket4j.extension(Infinispan.class).builder()
                   .addLimit(Bandwidth.simple(1_000, Duration.ofMinutes(1)))
                   .build(map, key, RecoveryStrategy.RECONSTRUCT);
``

## Example of ProxyManager instantiation
``java
org.infinispan.functional.FunctionalMap.ReadWriteMap<K, GridBucketState> map = ...;
...

ProxyManager proxyManager = Bucket4j.extension(Infinispan.class).proxyManagerForMap(map);
``