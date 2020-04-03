# Kafka Config Details

## 1.Broker

### 1.1. Producer

|                  config                   |                            means                             |        default         |
| :---------------------------------------: | :----------------------------------------------------------: | :--------------------: |
|                 **acks**                  |                 **produce acks (0, 1, -1)**                  |         **1**          |
|             **buffer.memory**             |                  **max buffer size (byte)**                  |   **33554432 (32M)**   |
|                  retries                  |          greater than zero means will resend record          |       2147483647       |
| **max.in.flight.requests.per.connection** | **max number of unchecked request per connection (to prevent disordered producing records need to be set as 1 when 'retries' is not zero )** |         **5**          |
|              **batch.size**               | **batch records (sent to the same partitions) together into fewer requests** |    **16384 (16k)**     |
|          connection.max.idle.ms           |                   idle connections timeout                   |     540000 (540s)      |
|          **delivery.timeout.ms**          | **an upper bound on the time to report send() returns(should be greeater than request.timeout.ms + linger.ms)** |   **120000 (120s)**    |
|               **linger.ms**               |  **delay after batch records before request transmission**   |    **0 (no delay)**    |
|               max.block.ms                |       control how long KafkaProducer.send() will block       |      60000 (60s)       |
|           **max.request.size**            |            **maximun size of a request in bytes**            |    **1048576 (1M)**    |
|           **partitioner.class**           | **partitioner class that implements the org.apache.kafka.clients.producer.Partitioner interface** | **DefaultPartitioner** |
|           receive.buffer.bytes            |              the size of the TCP receive buffer              |      32768 (32k)       |
|          **request.timeout.ms**           |                   **max request timeout**                    |    **30000 (30s)**     |
|             send.buffer.bytes             |               the size of the TCP send buffer                |     131072 (128k)      |
|            enable.idempotence             |      'true' means the producer will ensure exactly once      |         false          |
|          transaction.timeout.ms           | max timeout that the transaction coordinator wait for a transaction status update from the producer |       60000(60s)       |
|              transaction.id               | used for transactional delivery (need to set idempotence as true) |          null          |

### 1.2 Consumer

|            config             |                            means                             |                           default                            |
| :---------------------------: | :----------------------------------------------------------: | :----------------------------------------------------------: |
|        fetch.min.bytes        | the minimun amount of data the server should return for a fetch request |                           1 (byte)                           |
|         **group.id**          | **a unique string that identifies the consumer group this consumer belongs to** |                                                              |
|     heartbeat.interval.ms     | The expected time between heartbeats to the consumer coordinator(controler, must lower than 'session.timeout.ms', typically no less than 1/3 of 'session.timeout.ms') |                             3000                             |
|      session.timeout.ms       |       session timeout between consumer and controller        |                            10000                             |
| **max.partition.fetch.bytes** | **The maximum amount of data per-partition the server will return(The maximum record batch size accepted by the broker is defined via `message.max.bytes` (broker config) or `max.message.bytes` (topic config))** |                       **1048576 (1M)**                       |
|    allow.auto.create.topic    | Allow automatic topic creation on the broker when subscribing to or assigning a topic |                             true                             |
|     **auto.offset.reset**     | **What to do when there is no initial offset in Kafka or if the current offset does not exist any more on the server (    earliest: automatically reset the offset to the earliest offset  latest: automatically reset the offset to the latest offset         none: throw exception to the consumer if no previous offset is found for the consumer's group                                        anything else: throw exception to the consumer)** |                          **latest**                          |
|    connections.max.idle.ms    | Close idle connections after the number of milliseconds specified by this config |                        540000 (540s)                         |
|    default.api.timeout.ms     | Specifies the timeout (in milliseconds) for consumer APIs that could block |                         60000 (60s)                          |
|    **enable.auto.commit**     | **If true the consumer's offset will be periodically committed in the background** |                           **true**                           |
|    exclude.internal.topics    | Whether internal topics matching a subscribed pattern should be excluded from the subscription |                             true                             |
|      **fetch.max.bytes**      | **The maximum amount of data the server should return for a fetch request ( The maximum record batch size accepted by the broker is defined via `message.max.bytes` (broker config) or `max.message.bytes` (topic config) )** |                      **52428800(50M)**                       |
|       group.instance.id       | A unique identifier of the consumer instance provided by the end user(default consumer will join the consumer group as a dynamic member). Used when we need a continuious consumer to avoid consumer reblances caused by transient unavailability |                             null                             |
|        isolation.level        |    Controls how to read messages written transactionally     |                       read_uncommitted                       |
|     max.poll.interval.ms      |       The maximum delay between invocations of poll()        |                        300000 (300s)                         |
|       max.poll.recirds        | The maximum number of records returned in a single call to poll() |                             500                              |
| partition.assignment.strategy | what strategy the client will use to distribute partition ownership amongst consumer instances | org.apache.kafka .clients.consumer. ConsumerPartitionAssignor |
|     receive.buffer.bytes      | The size of the TCP receive buffer (SO_RCVBUF) to use when reading data. If the value is -1, the OS default will be used |                         65536 (64k)                          |
|    **request.timeout.ms**     | **The configuration controls the maximum amount of time the client will wait for the response of a request** |                       **30000 (30s)**                        |
|       send.buffer.bytes       | The size of the TCP send buffer (SO_SNDBUF) to use when sending data. If the value is -1, the OS default will be used |                        131072 (128k)                         |
|  **auto.commit.interval.ms**  | **The frequency in milliseconds that the consumer offsets are auto-committed to Kafka** |                        **5000 (5s)**                         |
|     **fetch.max.wait.ms**     | **The maximum amount of time the server will block before answering the fetch request if there isn't sufficient data to immediately satisfy the requirement given by fetch.min.bytes** |                        **500 (0.5s)**                        |
|    **interceptor.classes**    | **A list of classes to use as interceptors. Implementing the `org.apache.kafka.clients.consumer.ConsumerInterceptor` interface allows you to intercept (and possibly mutate) records received by the consumer. By default, there are no interceptors** |                            **""**                            |
|      metadata.max.age.ms      | The period of time in milliseconds after which we force a refresh of metadata |                        300000 (300s)                         |
|     **metric.reporters**      | **A list of classes to use as metrics reporters. Implementing the `org.apache.kafka.common.metrics.MetricsReporter` interface allows plugging in classes that will be notified of new metric creation** |                            **""**                            |
|       retry.backoff.ms        | The amount of time to wait before attempting to retry a failed request to a given topic partition |                          100 (0.1s)                          |

