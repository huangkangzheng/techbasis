# Kafka Config Details

## 1.Broker

### 1.1. Producer

|                config                 |                            means                             |      default       |
| :-----------------------------------: | :----------------------------------------------------------: | :----------------: |
|                 acks                  |                         produce acks                         |         1          |
|             buffer.memory             |                    max buffer size (byte)                    |   33554432 (32M)   |
|                retries                |          greater than zero means will resend record          |     2147483647     |
| max.in.flight.requests.per.connection | max number of unchecked request per connection (to prevent disordered producing records need to be set as 1 when 'retries' is not zero ) |         5          |
|              batch.size               | batch records (sent to the same partitions) together into fewer requests |    16384 (16k)     |
|        connection.max.idle.ms         |                   idle connections timeout                   |   540000 (540s)    |
|          delivery.timeout.ms          |     an upper bound on the time to report send() returns      |   120000 (120s)    |
|               linger.ms               |    delay after batch records before request transmission     |    0 (no delay)    |
|             max.block.ms              |       control how long KafkaProducer.send() will block       |    60000 (60s)     |
|           max.request.size            |              maximun size of a request in bytes              |    1048576 (1M)    |
|           partitioner.class           | partitioner class that implements the org.apache.kafka.clients.producer.Partitioner interface | DefaultPartitioner |
|         receive.buffer.bytes          |              the size of the TCP receive buffer              |    32768 (32k)     |
|          request.timeout.ms           |                     max request timeout                      |    30000 (30s)     |
|           send.buffer.bytes           |               the size of the TCP send buffer                |   131072 (128k)    |
|          enable.idempotence           |      'true' means the producer will ensure exactly once      |       false        |
|        transaction.timeout.ms         | max timeout that the transaction coordinator wait for a transaction status update from the producer |       60000        |
|            transaction.id             | used for transactional delivery (need to set idempotence as true) |        null        |

### 1.2 Consumer

|     config      |                            means                             | default  |
| :-------------: | :----------------------------------------------------------: | :------: |
| fetch.min.bytes | the minimun amount of data the server should return for a fetch request | 1 (byte) |
|    group.id     | a unique string that identifies the consumer group this consumer belongs to |          |
|                 |                                                              |          |

