## Spring Kafka 实战分析

黄康正

[TOC]

### 1.Spring Kafka Producer端原理分析

#### 1.1.源码分析

![](..\..\resources\kafka\spring-kafka-producer.png)

（1）kafkaTemplate.send(topic, record)

（2）拦截器拦截处理record

```java
ProducerRecord<K, V> interceptedRecord = this.interceptors == null ? record : this.interceptors.onSend(record);
```

（3）序列化数据

```java
serializedKey = keySerializer.serialize(record.topic(), record.headers(), record.key());
serializedValue = valueSerializer.serialize(record.topic(), record.headers(), record.value());
```

（4）选择分区（计算分区），partion算法默认是轮询算法，详细可以查看org.apache.kafka.clients.producer.KafkaProducer.partition()，可以通过实现org.apache.kafka.clients.producer.Partitioner接口覆盖默认的partition算法

```java
int partition = partition(record, serializedKey, serializedValue, cluster);
tp = new TopicPartition(record.topic(), partition);
```

（5）组装RecordBatch，核心逻辑是将往同一个topicPartition发送的消息放入同一个ProducerBatch，将ProducerBatch追加入Deque

```java
RecordAccumulator.RecordAppendResult result = accumulator.append(tp, timestamp, serializedKey,
                    serializedValue, headers, interceptCallback, remainingWaitMs);
```

（6）当batchIsFull Or newBatchCreated时，唤醒Sender线程

```java
if (result.batchIsFull || result.newBatchCreated) {
	log.trace("Waking up the sender since topic {} partition {} is either full or getting a new batch", record.topic(), partition);
	this.sender.wakeup();
}
```

（7）到第六步，我们了解到了Spring-Kafka对于Record的包装处理，此时还未进行消息发送的逻辑，org.apache.kafka.clients.producer.internals.Sender中的run方法才是真正的发送者线程的逻辑

（8）Sender.run(long now)  timestamp用于控制超时逻辑

（9）核心逻辑在于Sender.sendProducerData(long now)

```java
private long sendProducerData(long now) {
        Cluster cluster = metadata.fetch();

        // get the list of partitions with data ready to send
        RecordAccumulator.ReadyCheckResult result = this.accumulator.ready(cluster, now);

        // if there are any partitions whose leaders are not known yet, force metadata update
        if (!result.unknownLeaderTopics.isEmpty()) {
            // The set of topics with unknown leader contains topics with leader election pending as well as
            // topics which may have expired. Add the topic again to metadata to ensure it is included
            // and request metadata update, since there are messages to send to the topic.
            for (String topic : result.unknownLeaderTopics)
                this.metadata.add(topic);
            this.metadata.requestUpdate();
        }

        // remove any nodes we aren't ready to send to
        Iterator<Node> iter = result.readyNodes.iterator();
        long notReadyTimeout = Long.MAX_VALUE;
        while (iter.hasNext()) {
            Node node = iter.next();
            if (!this.client.ready(node, now)) {
                iter.remove();
                notReadyTimeout = Math.min(notReadyTimeout, this.client.connectionDelay(node, now));
            }
        }

        // create produce requests
        Map<Integer, List<ProducerBatch>> batches = this.accumulator.drain(cluster, result.readyNodes, this.maxRequestSize, now);
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

        sensors.updateProduceRequestMetrics(batches);

        // If we have any nodes that are ready to send + have sendable data, poll with 0 timeout so this can immediately
        // loop and try sending more data. Otherwise, the timeout is determined by nodes that have partitions with data
        // that isn't yet sendable (e.g. lingering, backing off). Note that this specifically does not include nodes
        // with sendable data that aren't ready to send since they would cause busy looping.
        long pollTimeout = Math.min(result.nextReadyCheckDelayMs, notReadyTimeout);
        if (!result.readyNodes.isEmpty()) {
            log.trace("Nodes with data ready to send: {}", result.readyNodes);
            // if some partitions are already ready to be sent, the select time would be 0;
            // otherwise if some partition already has some data accumulated but not ready yet,
            // the select time will be the time difference between now and its linger expiry time;
            // otherwise the select time will be the time difference between now and the metadata expiry time;
            pollTimeout = 0;
        }
        sendProduceRequests(batches, now);

        return pollTimeout;
    }
```

（10）核心逻辑在this.accumulator.drain（）方法中，drain的意思是排空，这里的意思就是将第六步前的RecordAccumulator中积累的数据排空，构建一个Key是brokerId， Value是List[batch]的Map叫做batches。虽然字面意思为排空，但是我们传入了个参数maxRequestSize，用于控制一个Request的大小

```java
Map<Integer, List<ProducerBatch>> batches = this.accumulator.drain(cluster, result.readyNodes,
                this.maxRequestSize, now);
```

（11）真正发送逻辑在Sender.sendProduceRequests(Map<Integer, List[ProducerBatch]> collated, long now)

根据brokerId组装Request，往同一个broker发送的record组装在一起发送，减少请求次数

```java
private void sendProduceRequests(Map<Integer, List<ProducerBatch>> collated, long now) {
        for (Map.Entry<Integer, List<ProducerBatch>> entry : collated.entrySet())
            sendProduceRequest(now, entry.getKey(), acks, requestTimeout, entry.getValue());
    }
```

#### 1.2.使用注意点

##### 1.2.1.kafkaTemplate.send()

一般情况下我们都会使用如下方法发送消息，有一点需要注意，由于callback的处理也是由kafkaProducer的发送者线程（sender线程）处理的，所以callback中如果加入IO操作等较慢的处理，会阻塞sender线程的发送，导致大批的RecordAccumulator中drain出来的数据超时失效了，抛出异常 xxx ms has passed since batch creation plus linger time（在1.1.9中的accumulator.expiredBatches方法中，控制了失效数据的判定条件及处理）。

故而当需要在回调函数中处理如DB，IO等操作时，尽量使用线程池处理，不要阻塞sender线程！！

```java
ListenableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, message);
future.addCallback(successCallback, failureCallback);
```

##### 1.2.2.linger.ms参数

如果去网上搜，可能会搜到很多关于这个参数的说法：当producer组装batch时，等待lingerMs后producer就会立马发送。这个时候，我会建议，尽量参考官方文档，读了官方文档后，会发现网上的很多解释都是错的。linger的意思是 继续存留;缓慢消失;流连;逗留;徘徊，我们在上述的代码可以看到，唤醒sender线程的条件是batchIsFull Or newBatchCreated，也就是说，lingerMs参数的default值是0，默认情况下producer是只要刚创建一个batch或者该batch已经满了，就会唤醒sender线程，立马进行发送。

但是我们思考一下kafka的使用场景，在大数据量的情况下，如果我的机器处理不够快，将record打包成batch的时候，如果每次batch都没有塞满，sender线程就去将batch发送了，这样当然是不够完美的。所以kafka提供了一个参数叫做linger.ms，每次组装成batch好了之后，逗留lingerMs时间，尽可能的让batch塞满，再去发送，尽量的减少发送请求的次数。

如果真的有需要用到该参数，一般情况不要“逗留”太长，5 - 10ms即可。如果设置2s，在数据量少的时候，producer每次都得等2s才去发送，消息延时太长，所以数据量少时使用default值0ms即可。仍然是视场景取舍的问题。

##### 1.2.3.核心配置参数

参考3.1中加粗的部分

### 2.Spring Kafka Consumer端原理分析

#### 2.1.源码分析

（1）首先是@KafkaListener注解，由于我们主要关注Kafka消费的逻辑，关于Spring如果为Kafka构建容器的部分这里就不赘述了（可自行查看**KafkaListenerAnnotationBeanPostProcessor**）

（2）入口在org.springframework.kafka.listener.KafkaMessageListenerContainer.doStart()

主要逻辑是通过加载proerties，实例化ListenerConsumer（ListenerConsumer是Spring的一层封装，并不是真正用于poll records的consumer，下面统称为ListenerConsumer）

```java
protected void doStart() {
		if (isRunning()) {
			return;
		}
		ContainerProperties containerProperties = getContainerProperties();
		if (!this.consumerFactory.isAutoCommit()) {
			AckMode ackMode = containerProperties.getAckMode();
			if (ackMode.equals(AckMode.COUNT) || ackMode.equals(AckMode.COUNT_TIME)) {
				Assert.state(containerProperties.getAckCount() > 0, "'ackCount' must be > 0");
			}
			if ((ackMode.equals(AckMode.TIME) || ackMode.equals(AckMode.COUNT_TIME))
					&& containerProperties.getAckTime() == 0) {
				containerProperties.setAckTime(5000);
			}
		}

		Object messageListener = containerProperties.getMessageListener();
		Assert.state(messageListener != null, "A MessageListener is required");
		if (containerProperties.getConsumerTaskExecutor() == null) {
			SimpleAsyncTaskExecutor consumerExecutor = new SimpleAsyncTaskExecutor(
					(getBeanName() == null ? "" : getBeanName()) + "-C-");
			containerProperties.setConsumerTaskExecutor(consumerExecutor);
		}
		Assert.state(messageListener instanceof GenericMessageListener, "Listener must be a GenericListener");
		this.listener = (GenericMessageListener<?>) messageListener;
		ListenerType listenerType = ListenerUtils.determineListenerType(this.listener);
		if (this.listener instanceof DelegatingMessageListener) {
			Object delegating = this.listener;
			while (delegating instanceof DelegatingMessageListener) {
				delegating = ((DelegatingMessageListener<?>) delegating).getDelegate();
			}
			listenerType = ListenerUtils.determineListenerType(delegating);
		}
		this.listenerConsumer = new ListenerConsumer(this.listener, listenerType);
		setRunning(true);
		this.listenerConsumerFuture = containerProperties
				.getConsumerTaskExecutor()
				.submitListenable(this.listenerConsumer);
	}
```

（3）ListenerConsumer中主要是实例化了consumer（kafka-client提供的用于subscribe topic的consumer），下面统称为consumer

```java
final Consumer<K, V> consumer =
					KafkaMessageListenerContainer.this.consumerFactory.createConsumer(
							this.consumerGroupId,
							this.containerProperties.getClientId(),
							KafkaMessageListenerContainer.this.clientIdSuffix);
			this.consumer = consumer;

			ConsumerRebalanceListener rebalanceListener = createRebalanceListener(consumer);

			if (KafkaMessageListenerContainer.this.topicPartitions == null) {
				if (this.containerProperties.getTopicPattern() != null) {
					consumer.subscribe(this.containerProperties.getTopicPattern(), rebalanceListener);
				}
				else {
					consumer.subscribe(Arrays.asList(this.containerProperties.getTopics()), rebalanceListener);
				}
			}
```

（4）真正逻辑在于org.springframework.kafka.listener.KafkaMessageListenerContainer的run()方法中

核心逻辑是个死循环，consumer从broker上poll一批消息，然后invokeListener（records）

```java
ConsumerRecords<K, V> records = this.consumer.poll(this.containerProperties.getPollTimeout());
......
if (records != null && records.count() > 0) {
						if (this.containerProperties.getIdleEventInterval() != null) {
							lastReceive = System.currentTimeMillis();
						}
						invokeListener(records);
					}
```

（5）invokeListener(records)

```java
private void invokeListener(final ConsumerRecords<K, V> records) {
		if (this.isBatchListener) {
			invokeBatchListener(records);
		}
		else {
			invokeRecordListener(records);
		}
	}
```

（6）invokeRecordListener(records)，核心逻辑在doInvokeWithRecords（records）

可以看出，对于consumer每次poll下来的一批数据，这里是遍历去对每条record invoke Listener，doInvokeRecordListener中就是去执行我们@KafkaListener的方法下的逻辑

```java
private void doInvokeWithRecords(final ConsumerRecords<K, V> records) throws Error {
			Iterator<ConsumerRecord<K, V>> iterator = records.iterator();
			while (iterator.hasNext()) {
				final ConsumerRecord<K, V> record = iterator.next();
				if (this.logger.isTraceEnabled()) {
					this.logger.trace("Processing " + record);
				}
				doInvokeRecordListener(record, null, iterator);
			}
		}
```

#### 2.2.使用注意点

##### 2.2.1.spring.kafka.consumer.concurrency参数

默认值为1，用于控制单个实例起多少个消费者线程

首先，联系Kafka的原理（同一个topic下有多个partition，比如4，同一个consumer group下的consumer会平均分配，比如2个consumer，即每个consumer各自管2个partition，如果是4个consumer，每个consumer各自管1个partition，即consumer group的rebalance），concurrency参数就是用于控制单套服务中可以起多少个。

consumer线程数量必须小于等于partiton的数量，因为超过partition数量的话多出来的consumer是没意义的

一般情况下，假设某个topic有4个partition，我们的服务中配置的消费者的concurrency是1，但是当我们发现一个消费者的处理速率是不足的，我们就可以通过增加concurrency，比如设置为4，那么单个服务中便会创建4个个KafkaMessageListenerContainer（也就是会起4个ListenerConsumer线程，参考ConcurrentMessageListenerContainer），即单个服务4个consumer。

```java
for (int i = 0; i < this.concurrency; i++) {
	KafkaMessageListenerContainer<K, V> container;
	if (topicPartitions == null) {
	container = new KafkaMessageListenerContainer<>(this, this.consumerFactory, containerProperties);
	}
	else {
        container = new KafkaMessageListenerContainer<>(this, this.consumerFactory, containerProperties, partitionSubset(containerProperties, i));
	}
	String beanName = getBeanName();
	container.setBeanName((beanName != null ? beanName : "consumer") + "-" + i);
	if (getApplicationEventPublisher() != null) {
		container.setApplicationEventPublisher(getApplicationEventPublisher());
	}
	container.setClientIdSuffix("-" + i);
	container.setAfterRollbackProcessor(getAfterRollbackProcessor());
	container.start();
	this.containers.add(container);
}
```

什么情况下会这么使用呢，比如我们单个服务器的资源是100%的话，当我们发现，单服务器中只有1个consumer时，资源可能只消耗了20%，那么我们期望压榨机器性能，可以考虑设置concurrency为4，使单台服务器的性能消耗达到80%，在传统的单服务器模式下可能适用这种做法

当然我不建议这么做，由于我们现在的部署结构主要是部署在K8s环境中，在k8s中，pod是无状态的，而且可以很方便的限制pod资源，可以很方便的对pod进行扩容或者缩减。假设我有4个partition，我期望有4个consumer，每个consumer消费50%的资源，来消费这4个partition，可以有很多种做法

做法一：concurrency设置为2，单个pod资源限制为100%，起2个pod

做法二：concurrency设置为1，单个pod资源限制为50%，起4个pod

由于pod是无状态的，且k8s对于pod的资源管理，扩容删减支持都很好，还支持宕机自动重启新的pod，所以我更建议后者，通过pod的扩容删减实现kafka consumer的横向扩容与删减

##### 2.2.2.batchListener

一般情况下，我们都采用的consumer的方法是：

通过上面的介绍我们知道，每次拉取一批的数据，是for循环遍历消费的

```java
@KafkaListener(topics = CHAIN_ACCESS_TX_COMMON_TOPIC)
public void consumeTransaction(ConsumerRecord<String, String> data) {}
```

所以当我们已经清楚明白，我们每次拉取下来的数据量很大的话，我们可以考虑使用batchListener来进行消费，甚至结合并行流来处理（并行流的底层是ForkJoinPool）

```java
#需要设置batch-listener=true
@KafkaListener(topics = CHAIN_ACCESS_TX_COMMON_TOPIC)
public void consumeTransaction(List<ConsumerRecord<String, String>> data) {
    data.parallelStream().forEach(record -> {});
}
```

##### 2.2.3.性能瓶颈问题

假设我们的consumer中的逻辑处理的比较复杂，消费单个record需要耗费0.1s，那么单个consumer的tps就是达到10个/s，假设1个topic是4个partition，那么我们即使扩容到4个consumer，tps也仅能达到40个/s，如果解决呢？

一个思路是：由于consumer是单线程在处理（for循环遍历处理），也就是说，机器的资源其实占用的并不是特别多，可能单个consumer处理只消耗了5%的机器性能，就是一种资源浪费。所以第一种思路是，比如采用batch-listener进行消费，使用并行流（比如20个线程）进行处理，是可以直接达到水平增长的效果的，单consumer的tps可以达到10*20 = 200个/s，4个consumer的tps可以达到800个/s

另一个思路呢？

其实我们借鉴kafka的整套思想，发现kafka在很多事情上都是“偷懒”的，基本上概括，就是3件事

不断发消息并append（保证顺序）入文件

partition的存在使得topic有了一层物理隔离

consumer各自从各自负责的partion拉取数据，互不影响（partition内亦有序）

所以其实我们可以考虑异步的思想，与上面的producer中的callback使用线程池同理，consumer方法中的逻辑就是将task交给ExecutorServices处理，consumer只要负责拉取数据即可，真正的业务处理交由异步线程池处理。这样做的好处是，我们能更好的控制线程池的资源，甚至是线程池中task处理的逻辑，设计如何保障消息不丢失问题等等。

```java
@KafkaListener(topics = CHAIN_ACCESS_TX_COMMON_TOPIC)
public void consumeTransaction(ConsumerRecord<String, String> data) {
    ThreadPool.submit(new Task(data));
}
```

##### 2.2.4.自动提交与手动提交

上面介绍了，consume方法是遍历的去消费record，自动提交的原理就是，每次doInvokeRecordListener（执行完consume的逻辑后，会更新offset到singletonMap[topicPartition, offset]），然后ackCurrent，在spring-kafka中，autocommit的逻辑也是由spring控制的

```java
Map<TopicPartition, OffsetAndMetadata> offsetsToCommit = Collections.singletonMap(new TopicPartition(record.topic(), record.partition()), new OffsetAndMetadata(record.offset() + 1));
```

ackCurrenct中的逻辑是：

如果是RecordAck，消费一个record就立马提交ack

否则如果是!isManualAck && !isManualImmediateAck&& !autocommit，将record加入一个blockingQueue叫做acks

否则的话，sendOffsetsToTransaction，其逻辑是对上面提及的singletonMap[topicPartition, offset]）的offset进行提交

```java
public void ackCurrent(final ConsumerRecord<K, V> record, @SuppressWarnings("rawtypes") Producer producer) {
	if (this.isRecordAck) {
		Map<TopicPartition, OffsetAndMetadata> offsetsToCommit = Collections.singletonMap(new TopicPartition(record.topic(), record.partition()), new OffsetAndMetadata(record.offset() + 1));
		if (producer == null) {
			this.commitLogger.log(() -> "Committing: " + offsetsToCommit);
			if (this.containerProperties.isSyncCommits()) {
				this.consumer.commitSync(offsetsToCommit);
			}
			else {
				this.consumer.commitAsync(offsetsToCommit, this.commitCallback);
			}
		}
		else {
			this.acks.add(record);
		}
	}
	else if (!this.isAnyManualAck && !this.autoCommit) {
		this.acks.add(record);
	}
	if (producer != null) {
		try {
			sendOffsetsToTransaction(producer);
		}
		catch (Exception e) {
			this.logger.error("Send offsets to transaction failed", e);
		}
	}
}
```

坑：通过上述代码，我们可以发现，每次ListenerConsumer只要执行完invokeListener（我们的业务实现逻辑），就会视该条消息已经被消费了，可以被提交offset了。也就是说，当我们的业务实现中，即使抛出了个异常，该条消费仍然是被消费完毕了，且会被ack的。故而在消费的业务实现中，需要注意异常的处理，适当加入一些回滚操作，保证业务的合理性。

一般情况下，采用spring-kafka的autocommit其实可以保障很多场景了，如果需要更细粒化的ack控制，那么可以采用手动提交offset，选择合适的ackMode，在业务的任意位置，当业务处理结束并且成功时，再去提交offset。

附：ackMode枚举表

|     AckMode      |                            Means                             |
| :--------------: | :----------------------------------------------------------: |
|      RECORD      |    Commit after each record is processed by the listener     |
|      BATCH       | Commit whatever has already been processed before the next poll |
|       TIME       | Commit pending updates after ContainerProperties#setAckTime(long) ackTime has elapsed） |
|      COUNT       | Commit pending updates after ContainerProperties#setAckCount(int) ackCount has been * exceeded） |
|    COUNT_TIME    | Commit pending updates after ContainerProperties#setAckCount(int) ackCount has been exceeded or after ContainerProperties#setAckTime(long)  ackTime has elapsed） |
|      MANUAL      | User takes responsibility for acks using an AcknowledgingMessageListener |
| MANUAL_IMMEDIATE | User takes responsibility for acks using an AcknowledgingMessageListener. The consumer immediately processes the commit |

##### 2.2.5.消息消费幂等性处理

方案一：将offset放入第三方组件（如redis），从redis中拉取offset保证每个offset只会被消费一次

方案二：数据库主键限制，record中含有数据库的主键，通过数据库主键控制无法重复被消费，一般情况下建议使用

##### 2.2.6.核心配置参数

参考3.2中加粗的部分

### 3.Kafka 配置分析

#### 3.1.Producer

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

#### 3.2.Consumer

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
