​        50个线程*1000个请求调用postText，实际上成功发送至kafka的请求数只有2000条左右，其它的请求全都抛出异常 exxpire 312 records 37 ms has passed since batch creation plus linger time

定位这类问题，我们得先追溯源码，了解kafka client发送请求到kafka broker的逻辑，才能明白服务为何会抛超时异常

​        经过查阅源码发现，从kafkaTemplate.send(topic, record)往里走，kafka client发送源码的核心逻辑为：

​      （1）client选定该topic的partition（实现Partitioner的类，默认为defaultPartitioner），生成一个topicpartition对象tp

```java
int partition = partition(record, serializedKey, serializedValue, cluster);
tp = new TopicPartition(record.topic(), partition);
```

​      （2）client把往同一个tp发送的消息打包成一个batch

```java
RecordAccumulator.RecordAppendResult result = accumulator.append(tp, timestamp, serializedKey,
                    serializedValue, headers, interceptCallback, remainingWaitMs);
```

​      （3）如果batch满了或者batch是新建立的，则唤醒sender线程

```java
if (result.batchIsFull || result.newBatchCreated) {
                log.trace("Waking up the sender since topic {} partition {} is either full or getting a new batch", record.topic(), partition);
                this.sender.wakeup();
            }
```

​      （4）sender线程发送请求到broker

​      （5）查看sender（Sender.java类）线程的run方法，入参是个当前时间戳

```java
void run(long now) {}
```

​      （6）实际的逻辑是sendProducerData(long now) 方法中，该方法的的核心逻辑为：

this.accumulator.drain()方法是拿出往同一个broker发送的batch放入map中，key为brokerId

this.accumulator.expiredBatches(this.requestTimeout, now)方法是抛弃到超出request.timeout.ms的batch，也就是我们遇到的报错

将往同一个broker发送的batch打包成一个request，然后去真正发送请求，代码为

```java
// create produce requests
Map<Integer, List<ProducerBatch>> batches = this.accumulator.drain(cluster, result.readyNodes,
                this.maxRequestSize, now);
        if (guaranteeMessageOrder) {
            // Mute all the partitions drained
            for (List<ProducerBatch> batchList : batches.values()) {
                for (ProducerBatch batch : batchList)
                    this.accumulator.mutePartition(batch.topicPartition);
            }
        }

        List<ProducerBatch> expiredBatches = this.accumulator.expiredBatches(this.requestTimeout, now);
        // Reset the producer id if an expired batch has previously been sent to the broker. Also update the metrics
        // for expired batches. see the documentation of @TransactionState.resetProducerId to understand why
        // we need to reset the producer id here.
        if (!expiredBatches.isEmpty())
            log.trace("Expired {} batches in accumulator", expiredBatches.size());
        for (ProducerBatch expiredBatch : expiredBatches) {
            failBatch(expiredBatch, -1, NO_TIMESTAMP, expiredBatch.timeoutException(), false);
            if (transactionManager != null && expiredBatch.inRetry()) {
                // This ensures that no new batches are drained until the current in flight batches are fully resolved.
                transactionManager.markSequenceUnresolved(expiredBatch.topicPartition);
            }
        }
```

​        查找我们的报错信息，实际上是产生于this.accumulator.expiredBatches(this.requestTimeout, now)方法中的，如下所示：

```java
    /**
     * A batch whose metadata is not available should be expired if one of the following is true:
     * <ol>
     *     <li> the batch is not in retry AND request timeout has elapsed after it is ready (full or linger.ms has reached).
     *     <li> the batch is in retry AND request timeout has elapsed after the backoff period ended.
     * </ol>
     * This methods closes this batch and sets {@code expiryErrorMessage} if the batch has timed out.
     */
    boolean maybeExpire(int requestTimeoutMs, long retryBackoffMs, long now, long lingerMs, boolean isFull) {
        if (!this.inRetry() && isFull && requestTimeoutMs < (now - this.lastAppendTime))
            expiryErrorMessage = (now - this.lastAppendTime) + " ms has passed since last append";
        else if (!this.inRetry() && requestTimeoutMs < (createdTimeMs(now) - lingerMs))
            expiryErrorMessage = (createdTimeMs(now) - lingerMs) + " ms has passed since batch creation plus linger time";
        else if (this.inRetry() && requestTimeoutMs < (waitedTimeMs(now) - retryBackoffMs))
            expiryErrorMessage = (waitedTimeMs(now) - retryBackoffMs) + " ms has passed since last attempt plus backoff time";

        boolean expired = expiryErrorMessage != null;
        if (expired)
            abortRecordAppends();
        return expired;
    }
```

​        我们可以得出结论，在往broker发送实际的请求之前，我们的client会标识当前这个batch已经过期了，并且抛出了requesttimeout异常，这是什么原因呢

​        概括起来无非就是有2方面的原因：

（1）由于producer是需要接收ack的，broker处理速度不够快，导致后面的请求被堆积着触发了超时

（2）client端需要发送的batch过多，导致client发送不过来触发超时

​        沿着这个思路，排除原因一broker的配置不足，原因二，我们能做的优化也就是：加大batchsize，增大maxrequestsize，增大requesttimeout参数等等

​        但是修改了参数之后，发现同样是50个线程*1000个请求调用postText，成功的请求数实际上也就是变成了5000条左右而已

​        接着排查......

​        我们通过查看日志，查看运行时线程等，发现kafka的发送者的线程只有一个kafka-client-producer-1，也就是说，kafka发送是单线程发送的，且服务所在的机器的cpu情况还是良好的

​        沿着这个思路可以考虑，单个服务对应多个producer线程，扩容确实能解决问题，但不符合扩展性和我们的需求，本质问题还是没有解决

​        后面发现在kafkaproducer.send()方法中，官方说明有一个说明

```
* Note that callbacks will generally execute in the I/O thread of the producer and so should be reasonably fast or
* they will delay the sending of messages from other threads. If you want to execute blocking or computationally
* expensive callbacks it is recommended to use your own {@link java.util.concurrent.Executor} in the callback body
* to parallelize processing.
```

​        我们采用的是kafkaTemplate.send()的异步发送的实现，也就是说，虽然是异步的，但是异步的callback的逻辑是由kafka-client-producer-1线程执行的

​        而我们的发送的逻辑的callback中，参杂了db的操作（做状态回滚，异常处理等等），这部分导致了callback的处理过慢，阻塞了发送的逻辑，真正导致了发送的异常

​        经过优化，将回调的db操作等逻辑放入线程池中异步进行，不阻塞发送，问题解决。