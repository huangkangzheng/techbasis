## Lettuce源码详解

[TOC]

### 1. RedisTemplate源码解读

以下内容针对的为Cluster场景

#### 1.1. Lettuce连接Redis Cluster

对于Redis集群，Lettuce连接有下面的特点：

```
Connecting to a Redis Cluster requires one or more initial seed nodes. The full cluster topology view (partitions) is obtained on the first connection so you’re not required to specify all cluster nodes. Specifying multiple seed nodes helps to improve resiliency as lettuce is able to connect the cluster even if a seed node is not available. Lettuce holds multiple connections, which are opened on demand. You are free to operate on these connections.

Connections can be bound to specific hosts or nodeIds. Connections bound to a nodeId will always stick to the nodeId, even if the nodeId is handled by a different host. Requests to unknown nodeId’s or host/ports that are not part of the cluster are rejected. Do not close the connections. Otherwise, unpredictable behavior will occur. Keep also in mind that the node connections are used by the cluster connection itself to perform cluster operations: If you block one connection all other users of the cluster connection might be affected.
```

（1）连接到Redis集群需要一个或多个的初始（种子）节点（spring.redis.cluster.nodes:）

（2）初始连接时会去获取集群拓扑

（3）Lettuce连接到集群的cluster connection持有多个由守护线程开启的连接到具体node的 node connection，我们可以操作这些node connection，node connection可以绑定到特定的host或者是nodeId

（4）不要关闭node connection，否则会发生不可预料的事情

（5）这些node connections被cluster connection所持有来做集群的操作，如果关闭一个node connection，那么使用cluster connection的用户将会被影响

```
With Standalone Redis, a single connection object correlates with a single transport connection. Redis Cluster works differently: A connection object with Redis Cluster consists of multiple transport connections. These are:

Default connection object (Used for key-less commands and for Pub/Sub message publication)

Connection per node (read/write connection to communicate with individual Cluster nodes)

When using ReadFrom: Read-only connection per read replica node (read-only connection to read data from read replicas)

Connections are allocated on demand and not up-front to start with a minimal set of connections. Formula to calculate the maximum number of transport connections for a single connection object:

1 + (N * 2)
Where N is the number of cluster nodes.

Apart of connection objects, RedisClusterClient uses additional connections for topology refresh. These are created on topology refresh and closed after obtaining the topology:

Set of connections for cluster topology refresh (a connection to each cluster node)
```

一个ClusterConnection持有的多个NodeConnection连接，分别为：

（1）默认的连接对象（用于不需要key的命令或者pub/sub模型）

（2）对于每个节点的连接（与集群的每个节点的读/写连接）

（3）ReadFrom所配置的连接：对每个读备份节点的只读连接

这些连接在后台被分配最少连接数，且最大的连接数为 1 + （N * 2）

除了上面这些连接对象外，RedisClusterClient还使用了额外的连接来做拓扑刷新，在刷新的时候开启获得拓扑之后关闭，拓扑刷新的连接也是每个节点一个连接

#### 1.2. RedisTemplate类图

@startuml
title  RedisTemplate

'skinparam packageStyle rect/' 加入这行代码，样式纯矩形'/
skinparam backgroundColor #EEEBDC
skinparam roundcorner 20
skinparam sequenceArrowThickness 2
'skinparam handwritten true

class RedisTemplate {
	
}

class RedisAccessor {
	RedisConnectionFactory connectionFactory //连接工厂
}

interface RedisConnectionFactory {
	
}

class JedisConnectionFactory {
	
}

class LettuceConnectionFactory {
	LettuceClientConfiguration clientConfiguration; //Lettuce客户端配置，如SSL，网络配置，池配置等等所有配置
	AbstractRedisClient client; //RedisClient配置
	LettuceConnectionProvider connectionProvider; //
	LettuceConnectionProvider reactiveConnectionProvider; //
	LettucePool pool; //池化专用链接
	RedisClusterConfiguration clusterConfiguration; //集群配置
	ClusterComandExecutor clusterCommandExecutor； //集群命令执行者
}

interface LettuceClientConfiguration {
	
}

interface LettucePoolingClientConfiguration {
	LettucePoolingClientConfigurationBuilder builder(); //builder
}

class LettucePoolingClientConfigurationBuilder {
	LettucePoolingClientConfigurationBuilder poolConfig(GenericObjectPoolConfig poolConfig); //连接池池配置
	LettucePoolingClientConfigurationBuilder clientOption(ClientOptions clientOptions); //客户端配置 
	LettucePoolingClientConfigurationBuilder build(); //build方法
}

interface ClientOptions {
	
}

class ClusterTopologyRefreshOptions {
	Builder build; //builder, enablePeriodicRefresh(), enableAdaptiveRefreshTriggers();
}

interface ClientResources {
	
}

abstract class AbstractRedisClient {
	ClientResources clientResources; //IO, Pool等性能优化配置
}

class RedisClusterClient {
	AtomicBolean clusterTopolotyRefreshActivated; //是否开启拓扑刷新
	ClusterTopologyRefresh clusterTopologyRefresh； //刷新拓扑逻辑的封装
	ClusterTopologyRefreshScheduler clusterTopologyRefreshScheduler; //定时刷新调度器
	Iterable<RedisURI> initialUris; //初始的nodeUri集合
	Partitions partitions; //集群节点集合的封装

	void initalizePartitions(); //初始化Partitions
	void reloadPartitions(); //重新加载Partitions
	Partitions loadPartitions(); // 加载Partitions，调用clusterTopologyRefresh.loadViews()
}

class ClusterTopologyRefresh {
	NodeConnectionFactory nodeConnectionFactory;
	ClientResources clientResources; //IO, Pool等性能优化配置

	Map<RedisURIm Partitions> loadViews(Iterable<RedisURI> seed, boolean discovery); //discovery就是是否允许发现除seed外的node
}

class ClusterTopologyRefreshScheduler {
	RedisClusterClient redisClusterClient;
	ClientResource clientResource;
	ClisterTopologyRefreshTask clusterTopologyRefreshTask;

	void run(); //implement Runable,线程run方法，只做了submit task的操作
}

class Partitions { 
	//继承了Collection<RedisClusterNode>
	Collection<RedisClusterNode> nodeReadView;
}

class ClisterTopologyRefreshTask {
	RedisClusterClient redisClusterClient;

	void run(); //implement Runable,线程run方法，只做了redisClusterClient.reloadPartitions();
}

interface LettuceConnectionProvider {
	
}

class ClusterConnectionProvider {
	RedisClusterClient client;
	RedisCodec<?, ?> codec;
	Optional<ReadFrom> readFrom; //从哪里读
	<T extends StatefulConnection<?, ?>> T getConnection(Class<T> connectionType); 
	//connectionType就是连接方式，pubsub或者普通连接，调用RedisClusterClient.connect(codec)
}

class RedisClusterConfiguration {
	Set<RedisNode> clusterNodes;
	Integer maxRedirects;
	RedisPassword password;
}

class ClusterCommandExecutor {
	AsyncTaskExecutor executor; //命令执行者
	ClusterTopologyProvider topologyProvider; //会获得集群拓扑，就是RedisClusterClient.partitions
	ClusterNodeResourceProvider resourceProvider; //获得某个特定的node host:port的连接
	ExceptionTranslationStrategy exceptionTranslationStrategy; //异常策略
}

RedisAccessor <|-- RedisTemplate
RedisAccessor --> RedisConnectionFactory

RedisConnectionFactory <|.. JedisConnectionFactory
RedisConnectionFactory <|.. LettuceConnectionFactory

AbstractRedisClient --|> ClientResources

LettuceConnectionFactory --> AbstractRedisClient
LettuceConnectionFactory --> LettuceClientConfiguration
LettuceConnectionProvider <-- LettuceConnectionFactory
ClusterCommandExecutor <-- LettuceConnectionFactory
LettuceConnectionFactory --> RedisClusterConfiguration

LettuceClientConfiguration <|-- LettucePoolingClientConfiguration
LettucePoolingClientConfiguration --> LettucePoolingClientConfigurationBuilder
LettucePoolingClientConfigurationBuilder ..> ClientOptions
ClientOptions <|.. ClusterTopologyRefreshOptions

RedisClusterClient --|> AbstractRedisClient
RedisClusterClient --> ClusterTopologyRefresh
RedisClusterClient <--> ClusterTopologyRefreshScheduler
RedisClusterClient --> Partitions

ClusterTopologyRefresh --> ClientResources

ClusterTopologyRefreshScheduler --> ClientResources
ClusterTopologyRefreshScheduler --> ClisterTopologyRefreshTask

ClisterTopologyRefreshTask --> RedisClusterClient

ClusterConnectionProvider ..|> LettuceConnectionProvider
ClusterConnectionProvider --> RedisClusterClient
@enduml

#### 1.3. redisTemplate.opsForValue().get("key")

就是执行DefaultValueOperations.get(Object key)方法

```
## DefaultValueOperations.java
	public V get(Object key) {
	
## 调用了RedisTemplate.execute()
		return execute(new ValueDeserializingRedisCallback(key) {

			@Override
			protected byte[] inRedis(byte[] rawKey, RedisConnection connection) {
				return connection.get(rawKey);
			}
		}, true);
	}

## RedisTemplate.java
	public <T> T execute(RedisCallback<T> action, boolean exposeConnection) {
		return execute(action, exposeConnection, false);
	}
	
	public <T> T execute(RedisCallback<T> action, boolean exposeConnection, boolean pipeline) {

		Assert.isTrue(initialized, "template not initialized; call afterPropertiesSet() before using it");
		Assert.notNull(action, "Callback object must not be null");

## 获取到了RedisAccessor中的connectionFactory
		RedisConnectionFactory factory = getRequiredConnectionFactory();
		RedisConnection conn = null;
		try {

			if (enableTransactionSupport) {
				// only bind resources in case of potential transaction synchronization
## enableTransactionSupport为true的时候会绑定连接与线程放入RedisConnectionHolder，默认为false
				conn = RedisConnectionUtils.bindConnection(factory, enableTransactionSupport);
			} else {
## 获取connection,
## 如果有RedisConnectionHolder，则直接从connectionHolder.getConnection()
## 否则是new LettuceClusterConnection(connectionProvider，clusterClient，clusterCommandExecutor)
				conn = RedisConnectionUtils.getConnection(factory);
			}

			boolean existingConnection = TransactionSynchronizationManager.hasResource(factory);

			RedisConnection connToUse = preProcessConnection(conn, existingConnection);

			boolean pipelineStatus = connToUse.isPipelined();
## pipeline = false
			if (pipeline && !pipelineStatus) {
				connToUse.openPipeline();
			}

## exposeConnection = true
			RedisConnection connToExpose = (exposeConnection ? connToUse : createRedisConnectionProxy(connToUse));
			
## 就是DefaultValueOperations传进来的callback函数
## ValueDeserializingRedisCallback.doInRedis(RedisConnection connection) {
## 			byte[] result = inRedis(rawKey(key), connection);
## 			return deserializeValue(result);
## 		}
## ValueDeserializingRedisCallback.inRedis(byte[] rawKey, RedisConnection connection) {
## 				return connection.get(rawKey);
## 			}
## connection.get(rawKey) 方法就是调用LettuceStringCommands执行
			T result = action.doInRedis(connToExpose);

			// close pipeline
			if (pipeline && !pipelineStatus) {
				connToUse.closePipeline();
			}

			// TODO: any other connection processing?
## 异步获取result
			return postProcessResult(result, connToUse, existingConnection);
		} finally {
## 释放连接，如果有RedisConnectionHolder且开启事务则直接return
## 否则不开启事务时就是conn.close();
			RedisConnectionUtils.releaseConnection(conn, factory);
		}
	}
	
## 释放连接详解
## 走到最后会执行connectionProvider.release(asyncDedicatedConn);就是poolingProvider
	public void release(StatefulConnection<?, ?> connection) {

## 从PoolRef（concurrentHashMap）中移除
		GenericObjectPool<StatefulConnection<?, ?>> pool = poolRef.remove(connection);

		if (pool == null) {
			throw new PoolException("Returned connection " + connection
					+ " was either previously returned or does not belong to this connection provider");
		}
		
## 用完了把connection放回到pool里面
		pool.returnObject(connection);
	}

```

#### 1.4. LettuceStringCommands解读

实际是LettuceStringsCommand，LettuceStringCommand持有LettuceConnection connection

在集群环境就是LettuceClusterConnection

LettuceClusterConnection持有ClusterCommandExecutor和ClusterTopologyProvider

执行getConnection().get(key)

```
## LettuceStringCommand
	public byte[] get(byte[] key) {

		Assert.notNull(key, "Key must not be null!");

		try {
			if (isPipelined()) {
				pipeline(connection.newLettuceResult(getAsyncConnection().get(key)));
				return null;
			}
			if (isQueueing()) {
				transaction(connection.newLettuceResult(getAsyncConnection().get(key)));
				return null;
			}
			return getConnection().get(key);
		} catch (Exception ex) {
			throw convertLettuceAccessException(ex);
		}
	}
## getConnection()方法
protected RedisClusterCommands<byte[], byte[]> getConnection() {

		if (isQueueing()) {
			return getDedicatedConnection();
		}
		if (asyncSharedConn != null) {

			if (asyncSharedConn instanceof StatefulRedisConnection) {
				return ((StatefulRedisConnection<byte[], byte[]>) asyncSharedConn).sync();
			}
			if (asyncSharedConn instanceof StatefulRedisClusterConnection) {
				return ((StatefulRedisClusterConnection<byte[], byte[]>) asyncSharedConn).sync();
			}
		}
		return getDedicatedConnection();
	}
	
## 没有shareConnection时，默认就是getDedicatedConnection()
RedisClusterCommands<byte[], byte[]> getDedicatedConnection() {

		if (asyncDedicatedConn == null) {
		
			asyncDedicatedConn = doGetAsyncDedicatedConnection();

			if (asyncDedicatedConn instanceof StatefulRedisConnection) {
				((StatefulRedisConnection<byte[], byte[]>) asyncDedicatedConn).sync().select(dbIndex);
			}
		}

		if (asyncDedicatedConn instanceof StatefulRedisConnection) {
			return ((StatefulRedisConnection<byte[], byte[]>) asyncDedicatedConn).sync();
		}
		if (asyncDedicatedConn instanceof StatefulRedisClusterConnection) {
			return ((StatefulRedisClusterConnection<byte[], byte[]>) asyncDedicatedConn).sync();
		}

		throw new IllegalStateException(
				String.format("%s is not a supported connection type.", asyncDedicatedConn.getClass().getName()));
	}
	
## 获得连接的方法是doGetAsyncDedicatedConnection()
## 就是通过connectionProvider去获取连接
## 在有Pool配置时是LettucePoolingConnectionProvider
## 没有Pool的话就是ClusterConnectionProvider去创建连接
## 在这里我们是Pooling，源码分析在下文<1.5>
protected StatefulConnection<byte[], byte[]> doGetAsyncDedicatedConnection() {
		return connectionProvider.getConnection(StatefulConnection.class);
	}

## getDedicatedConnection() 在这里return的是((StatefulRedisClusterConnection<byte[], byte[]>) 
## asyncDedicatedConn).sync(); 即RedisAdvancedClusterCommands
## ClusterFutureSyncInvocationHandler去执行Command
## StatefulRedisClusterConnectionImpl.sync()的源码分析在下文<1.7>
public interface RedisAdvancedClusterCommands<K, V> extends RedisClusterCommands<K, V> {

    RedisClusterCommands<K, V> getConnection(String nodeId);

    RedisClusterCommands<K, V> getConnection(String host, int port);
    
    ......
    还有各种redis操作的Commands的方法
    ......
    }
```

#### 1.5. LettucePoolingConnectionProvider获取连接源码解读

```
## pools是ConcurrentHashMap<Class<?>, GenericObjectPool<StatefulConnection<?, ?>>>
## key是connectionType，value是pool
## computeIfAbsent是如果不存在key则put value并返回value，存在则直接返回get(key)
## put的就是ConnectionPoolSupport.createGenericObjectPool()，在下面解析
## 在这里就是说一个connectionType对应一个pool
## pool.borrowObject()获取一个连接

public <T extends StatefulConnection<?, ?>> T getConnection(Class<T> connectionType) {

		GenericObjectPool<StatefulConnection<?, ?>> pool = pools.computeIfAbsent(connectionType, poolType -> {
			return ConnectionPoolSupport.createGenericObjectPool(() -> connectionProvider.getConnection(connectionType),
					poolConfig, false);
		});

		try {

			StatefulConnection<?, ?> connection = pool.borrowObject();

## 将获得得connection放入poolRef中（是个ConcurrentHashMap），释放连接的时候会remove的
			poolRef.put(connection, pool);

			return connectionType.cast(connection);
		} catch (Exception e) {
			throw new PoolException("Could not get a resource from the pool", e);
		}
	}
	
## connectionSupplier就是ClusterConnectionProvider.getConnection()
## new GenericObjectPool<T>(PooledObjectFactory<T> factory, GenericObjectPoolConfig config)
## factory就是RedisPooledObjectFactory，参数是connectionSupplier
## 池化的封装，获取连接是通过connectionSupplier获得的
public static <T extends StatefulConnection<?, ?>> GenericObjectPool<T> createGenericObjectPool(
            Supplier<T> connectionSupplier, GenericObjectPoolConfig config, boolean wrapConnections) {

        LettuceAssert.notNull(connectionSupplier, "Connection supplier must not be null");
        LettuceAssert.notNull(config, "GenericObjectPoolConfig must not be null");

        AtomicReference<Origin<T>> poolRef = new AtomicReference<>();

        GenericObjectPool<T> pool = new GenericObjectPool<T>(new RedisPooledObjectFactory<T>(connectionSupplier), config) {

            @Override
            public T borrowObject() throws Exception {
                return wrapConnections ? ConnectionWrapping.wrapConnection(super.borrowObject(), poolRef.get()) : super
                        .borrowObject();
            }

            @Override
            public void returnObject(T obj) {

                if (wrapConnections && obj instanceof HasTargetConnection) {
                    super.returnObject((T) ((HasTargetConnection) obj).getTargetConnection());
                    return;
                }
                super.returnObject(obj);
            }
        };

        poolRef.set(new ObjectPoolWrapper<>(pool));

        return pool;
    }
```

#### 1.6. ClusterConnectionProvider获取(创建)连接StatefulRedisClusterConnectionImpl源码解读

```
## ClusterConnectionProvider.java
## 2种连接方式，connect(codec) || connectPubSub(codec)
## 调用RedisClusterClient.connect();

public <T extends StatefulConnection<?, ?>> T getConnection(Class<T> connectionType) {

		if (connectionType.equals(StatefulRedisPubSubConnection.class)) {
			return connectionType.cast(client.connectPubSub(codec));
		}

		if (StatefulRedisClusterConnection.class.isAssignableFrom(connectionType)
				|| connectionType.equals(StatefulConnection.class)) {

			StatefulRedisClusterConnection<?, ?> connection = client.connect(codec);
			readFrom.ifPresent(connection::setReadFrom);

			return connectionType.cast(connection);
		}

		throw new UnsupportedOperationException("Connection type " + connectionType + " not supported!");
	}
```

```
## RedisClusterClient.java

	public <K, V> StatefulRedisClusterConnection<K, V> connect(RedisCodec<K, V> codec) {

        if (partitions == null) {
## 如果为空则初始化Partitions
            initializePartitions();	
        }
        
## getConnection() 返回的就是 connectClusterAsync(codec).get();
        return getConnection(connectClusterAsync(codec));
    }
    
	private <K, V> CompletableFuture<StatefulRedisClusterConnection<K, V>> connectClusterAsync(RedisCodec<K, V> codec) {

        if (partitions == null) {
            return Futures.failed(new IllegalStateException(
                    "Partitions not initialized. Initialize via RedisClusterClient.getPartitions()."));
        }

## 激活刷新拓扑
        activateTopologyRefreshIfNeeded();	

        logger.debug("connectCluster(" + initialUris + ")");

## 根据partitions加载socketAddress
        Mono<SocketAddress> socketAddressSupplier = getSocketAddressSupplier(TopologyComparators::sortByClientCount);

        DefaultEndpoint endpoint = new DefaultEndpoint(clientOptions, clientResources);
        RedisChannelWriter writer = endpoint;

        if (CommandExpiryWriter.isSupported(clientOptions)) {
            writer = new CommandExpiryWriter(writer, clientOptions, clientResources);
        }

## clusterTopologyRefreshScheduler 还实现了ClusterEventListener接口
## channelWriter，用于channel写入操作封装等等
        ClusterDistributionChannelWriter clusterWriter = new ClusterDistributionChannelWriter(clientOptions, writer,
                clusterTopologyRefreshScheduler);
        PooledClusterConnectionProvider<K, V> pooledClusterConnectionProvider = new PooledClusterConnectionProvider<>(this,
                clusterWriter, codec, clusterTopologyRefreshScheduler);

## 这里set了pooledClusterConnectionProvider，<1.8>中会用到
        clusterWriter.setClusterConnectionProvider(pooledClusterConnectionProvider);

## 创建Connection实例
        StatefulRedisClusterConnectionImpl<K, V> connection = new StatefulRedisClusterConnectionImpl<>(clusterWriter, codec,
                timeout);

## 从master读，setPartitions
        connection.setReadFrom(ReadFrom.MASTER);
        connection.setPartitions(partitions);

        Supplier<CommandHandler> commandHandlerSupplier = () -> new CommandHandler(clientOptions, clientResources, endpoint);

        Mono<StatefulRedisClusterConnectionImpl<K, V>> connectionMono = Mono.defer(() -> connect(socketAddressSupplier,
 codec,
                endpoint, connection, commandHandlerSupplier));

## 尝试getConnectionAttempts（）次重连
        for (int i = 1; i < getConnectionAttempts(); i++) {
            connectionMono = connectionMono.onErrorResume(t -> connect(socketAddressSupplier, codec, endpoint, connection,
                    commandHandlerSupplier));
        }

        return connectionMono
                .flatMap(c -> c.reactive().command().collectList() //
                        .map(CommandDetailParser::parse) //
                        .doOnNext(detail -> c.setState(new RedisState(detail))) //
                                .doOnError(e -> c.setState(new RedisState(Collections.emptyList()))).then(Mono.just(c))
                                .onErrorResume(RedisCommandExecutionException.class, e -> Mono.just(c)))
                .doOnNext(
                        c -> connection.registerCloseables(closeableResources, clusterWriter, pooledClusterConnectionProvider))
                .map(it -> (StatefulRedisClusterConnection<K, V>) it).toFuture();
    }
```

#### 1.7. StatefulRedisClusterConnectionImpl源码分析

```
## StatefulRedisClusterConnectionImpl持有
protected final RedisAdvancedClusterCommands<K, V> sync;
protected final RedisAdvancedClusterAsyncCommandsImpl<K, V> async;

## 构造函数
## sync() 就是java Proxy代理执行RedisAdvancedClusterCommands，handler是syncInvocationHandler()
## async() 有用，在下文介绍

	public StatefulRedisClusterConnectionImpl(RedisChannelWriter writer, RedisCodec<K, V> codec, Duration timeout) {

        super(writer, timeout);
        this.codec = codec;

        this.async = new RedisAdvancedClusterAsyncCommandsImpl<>(this, codec);
        this.sync = (RedisAdvancedClusterCommands) Proxy.newProxyInstance(AbstractRedisClient.class.getClassLoader(),
                new Class<?>[] { RedisAdvancedClusterCommands.class }, syncInvocationHandler());
        this.reactive = new RedisAdvancedClusterReactiveCommandsImpl<>(this, codec);
    }
   
## syncInvocationHandler()，返回的就是一个ClusterFutureSyncInvocationHandler，且持有async
    protected InvocationHandler syncInvocationHandler() {
        return new ClusterFutureSyncInvocationHandler<>(this, RedisClusterAsyncCommands.class, NodeSelection.class,
                NodeSelectionCommands.class, async());
    }
    
## Proxy执行到RedisAdvancedClusterCommands时，就会调用
## ClusterFutureSyncInvocationHandler.handleInvocation()方法
	protected Object handleInvocation(Object proxy, Method method, Object[] args) throws Throwable {

        try {

            if (method.isDefault()) {
                return methodHandleCache.computeIfAbsent(method, ClusterFutureSyncInvocationHandler::lookupDefaultMethod)
                        .bindTo(proxy).invokeWithArguments(args);
            }

            if (method.getName().equals("getConnection") && args.length > 0) {
                return getConnection(method, args);
            }

            if (method.getName().equals("readonly") && args.length == 1) {
                return nodes((Predicate<RedisClusterNode>) args[0], ClusterConnectionProvider.Intent.READ, false);
            }

            if (method.getName().equals("nodes") && args.length == 1) {
                return nodes((Predicate<RedisClusterNode>) args[0], ClusterConnectionProvider.Intent.WRITE, false);
            }

            if (method.getName().equals("nodes") && args.length == 2) {
                return nodes((Predicate<RedisClusterNode>) args[0], ClusterConnectionProvider.Intent.WRITE, (Boolean) args[1]);
            }

## 重点关注点在这
## apiMethodCache是个ConcurrentHashMap<>(Method, Method)
## key是sync的method，也就是Proxy代理运行的方法，value是
## asyncApi.getClass().getMethod(key.getName(), key.getParameterTypes()
## 也就是说，每个sync的方法运行时，实际上都是调用async的对应的方法
## 所以这里的执行是异步的，接着看async，async()就是RedisAdvancedClusterAsyncCommandsImpl
## RedisAdvancedClusterAsyncCommandsImpl就是真正的调用逻辑，继承了
## RedisAdvancedClusterAsyncCommandsImpl，持有之前获得到的StatefulRedisClusterConnectionImpl对象
            Method targetMethod = apiMethodCache.computeIfAbsent(method, key -> {

                try {
                    return asyncApi.getClass().getMethod(key.getName(), key.getParameterTypes());
                } catch (NoSuchMethodException e) {
                    throw new IllegalStateException(e);
                }
            });

            Object result = targetMethod.invoke(asyncApi, args);

            if (result instanceof RedisFuture) {
                RedisFuture<?> command = (RedisFuture<?>) result;
                if (!method.getName().equals("exec") && !method.getName().equals("multi")) {
                    if (connection instanceof StatefulRedisConnection && ((StatefulRedisConnection) connection).isMulti()) {
                        return null;
                    }
                }
                
## 由于是异步的，这里就是返回result，timeout就是StatefulClusterConnectionImpl.getTimeout()
## 默认值是60秒，就是我们在<1.6>中的redisClusterClient创建对象时传参的timeout
## 在lettuceClientConfiguration中可以配置
                return LettuceFutures.awaitOrCancel(command, getTimeoutNs(command), TimeUnit.NANOSECONDS);
            }

            return result;

        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }
    
## 比如我们运行的method是get()，实际调用的就是RedisAdvancedClusterAsyncCommandsImpl.get()
## RedisAdvancedClusterAsyncCommandsImpl继承AbstractRedisAsyncCommands
## 就是AbstractRedisAsyncCommands.get()
	public RedisFuture<V> get(K key) {
        return dispatch(commandBuilder.get(key));
    }
## commandBuilder.get(key)返回的是个Command[type=GET,output=ValueOutut[output=null,error='null]]
## args="CommandArgs[buffer=$43\r\n key \r\n]
## 看dispatch()方法
	public <T> AsyncCommand<K, V, T> dispatch(RedisCommand<K, V, T> cmd) {
        AsyncCommand<K, V, T> asyncCommand = new AsyncCommand<>(cmd);
        
## 这个connection就是我们之前一直说的StatefulRedisClusterConnectionImpl对象，就是pool提供的
        RedisCommand<K, V, T> dispatched = connection.dispatch(asyncCommand);
        if (dispatched instanceof AsyncCommand) {
            return (AsyncCommand<K, V, T>) dispatched;
        }
        return asyncCommand;
    }
    
## dispatched = connection.dispatch(asyncCommand)，就是调用
## StatefulRedisClusterConnectionImpl.dispatch()方法
## preProcessCommand是对cmd做一些auth，readOnly，readWrite的判断
    public <T> RedisCommand<K, V, T> dispatch(RedisCommand<K, V, T> command) {
        return super.dispatch(preProcessCommand(command));
    }
    
## super.dispatch()方法
    protected <T> RedisCommand<K, V, T> dispatch(RedisCommand<K, V, T> cmd) {

        if (debugEnabled) {
            logger.debug("dispatching command {}", cmd);
        }
        
        return channelWriter.write(cmd);
    }
    
## ClusterDistributionChannelWriter.write(cmd) 
	public <K, V, T> RedisCommand<K, V, T> write(RedisCommand<K, V, T> command) {

        if (command instanceof ClusterCommand && !command.isDone()) {

            ClusterCommand<K, V, T> clusterCommand = (ClusterCommand<K, V, T>) command;
            if (clusterCommand.isMoved() || clusterCommand.isAsk()) {

                HostAndPort target;
                boolean asking;
 ## 判断MOVE ASK重定向
                if (clusterCommand.isMoved()) {
                    target = getMoveTarget(clusterCommand.getError());
                    clusterEventListener.onMovedRedirection();
                    asking = false;
                } else {
                    target = getAskTarget(clusterCommand.getError());
                    asking = true;
                    clusterEventListener.onAskRedirection();
                }

                command.getOutput().setError((String) null);

                CompletableFuture<StatefulRedisConnection<K, V>> connectFuture = asyncClusterConnectionProvider
                        .getConnectionAsync(Intent.WRITE, target.getHostText(), target.getPort());

                if (isSuccessfullyCompleted(connectFuture)) {
                    writeCommand(command, asking, connectFuture.join(), null);
                } else {
                    connectFuture.whenComplete((connection, throwable) -> writeCommand(command, asking, connection, throwable));
                }

                return command;
            }
        }

## 发送请求
## 封装commandToSend，持有maxRedirections=5，retry（是个ClusterDistributionChannelWriter对象，基本上## 包含了所有的信息），command对象，complete标志位（代表是否完成）
        ClusterCommand<K, V, T> commandToSend = getCommandToSend(command);
        CommandArgs<K, V> args = command.getArgs();

        // exclude CLIENT commands from cluster routing
        if (args != null && !CommandType.CLIENT.equals(commandToSend.getType())) {

            ByteBuffer encodedKey = args.getFirstEncodedKey();
            if (encodedKey != null) {

## 计算hash
                int hash = getSlot(encodedKey);
## 获取READ或者WRITE
                Intent intent = getIntent(command.getType());
##根据hash和READ or WRITE 异步获取连接，从pooledClusterConnectionProvider中获得
## pooledClusterConnectionProvider获得连接在下文<1.8>解析
                CompletableFuture<StatefulRedisConnection<K, V>> connectFuture = ((AsyncClusterConnectionProvider) clusterConnectionProvider)
                        .getConnectionAsync(intent, hash);

## 如果获取到了连接，执行writeCommand操作
## channelWriter就是封装了channel操作，连接节点并且写内容
## 
                if (isSuccessfullyCompleted(connectFuture)) {
                    writeCommand(commandToSend, false, connectFuture.join(), null);
                } else {
                    connectFuture.whenComplete((connection, throwable) -> writeCommand(commandToSend, false, connection,
                            throwable));
                }

                return commandToSend;
            }
        }

        writeCommand(commandToSend, defaultWriter);

        return commandToSend;
    }
```

#### 1.8. PooledClusterConnectionProvider源码解析

```
	public CompletableFuture<StatefulRedisConnection<K, V>> getConnectionAsync(Intent intent, int slot) {

        if (debugEnabled) {
            logger.debug("getConnection(" + intent + ", " + slot + ")");
        }

        if (intent == Intent.READ && readFrom != null && readFrom != ReadFrom.MASTER) {
            return getReadConnection(slot);
        }

        return getWriteConnection(slot).toCompletableFuture();
    }
    
## 在这里是ReadConnection(slot)
	private CompletableFuture<StatefulRedisConnection<K, V>> getReadConnection(int slot) {

        CompletableFuture<StatefulRedisConnection<K, V>> readerCandidates[];// avoid races when reconfiguring partitions.

        boolean cached = true;

        synchronized (stateLock) {
            readerCandidates = readers[slot];
        }

        if (readerCandidates == null) {

## 从partitions中获得当前slot对应的master node
            RedisClusterNode master = partitions.getPartitionBySlot(slot);
            if (master == null) {
                throw new PartitionSelectorException(String.format("Cannot determine a partition to read for slot %d.", slot),
                        partitions.clone());
            }

## candidates就是该master与它的slave
            List<RedisNodeDescription> candidates = getReadCandidates(master);
            List<RedisNodeDescription> selection = readFrom.select(new ReadFrom.Nodes() {
                @Override
                public List<RedisNodeDescription> getNodes() {
                    return candidates;
                }

                @Override
                public Iterator<RedisNodeDescription> iterator() {
                    return candidates.iterator();
                }
            });

## selection获得的就是主的node
            if (selection.isEmpty()) {
                throw new PartitionSelectorException(String.format(
                        "Cannot determine a partition to read for slot %d with setting %s.", slot, readFrom),
                        partitions.clone());
            }

## 获取该node的连接，是个CompletableFuture<StatefulRedisConnectionImpl> []数组
## getReadFromConnections(selection)就是遍历selection调用
## getConnectionAsync(ConnectionKey(selection)).toCompletableFuture();
## getConnetionAsync()源码在下面解析
            readerCandidates = getReadFromConnections(selection);
            cached = false;
        }

## selectedReaderCandidates[] 就是返回的CompletableFuture数组
        CompletableFuture<StatefulRedisConnection<K, V>> selectedReaderCandidates[] = readerCandidates;

        if (cached) {

            return CompletableFuture.allOf(readerCandidates).thenCompose(v -> {

                for (CompletableFuture<StatefulRedisConnection<K, V>> candidate : selectedReaderCandidates) {

                    if (candidate.join().isOpen()) {
                        return candidate;
                    }
                }

                return selectedReaderCandidates[0];
            });
        }

        CompletableFuture<StatefulRedisConnection<K, V>[]> filteredReaderCandidates = new CompletableFuture<>();

## 遍历selectedReaderCandidates获取连接，放入filteredReaderCandidates中
        CompletableFuture.allOf(readerCandidates).thenApply(v -> selectedReaderCandidates)
                .whenComplete((candidates, throwable) -> {

                    if (throwable == null) {
                        filteredReaderCandidates.complete(getConnections(candidates));
                        return;
                    }

                    StatefulRedisConnection<K, V>[] connections = getConnections(selectedReaderCandidates);

                    if (connections.length == 0) {
                        filteredReaderCandidates.completeExceptionally(throwable);
                        return;
                    }

                    filteredReaderCandidates.complete(connections);
                });

        return filteredReaderCandidates
                .thenApply(statefulRedisConnections -> {

                    CompletableFuture<StatefulRedisConnection<K, V>> toCache[] = new CompletableFuture[statefulRedisConnections.length];

                    for (int i = 0; i < toCache.length; i++) {
                        toCache[i] = CompletableFuture.completedFuture(statefulRedisConnections[i]);
                    }
                    synchronized (stateLock) {
                        readers[slot] = toCache;
                    }

                    for (StatefulRedisConnection<K, V> candidate : statefulRedisConnections) {
                        if (candidate.isOpen()) {
                            return candidate;
                        }
                    }

## 返回第一个连接
                    return statefulRedisConnections[0];
                });
    }
    
## 如何获得node的连接呢，就是该类下的getConnectionAsync(这里面有node的信息)
## connectionProvider是个SynchronizingClusterConnectionProvider
protected ConnectionFuture<StatefulRedisConnection<K, V>> getConnectionAsync(ConnectionKey key) {

## 这一步返回的就是个DefaultConnectionFuture(持有remoteAddress = 某个节点的host:port)
        ConnectionFuture<StatefulRedisConnection<K, V>> connectionFuture = connectionProvider.getConnection(key);
        CompletableFuture<StatefulRedisConnection<K, V>> result = new CompletableFuture<>();

        connectionFuture.handle((connection, throwable) -> {

            if (throwable != null) {
   result.completeExceptionally(RedisConnectionException.create(connectionFuture.getRemoteAddress(), throwable));
            } else {
                result.complete(connection);
            }

            return null;
        });

        return ConnectionFuture.from(connectionFuture.getRemoteAddress(), result);
    }
    
## 接着往下看，SynchronizingClusterConnectionProvider.get(key) 是做什么的
## SynchronizingClusterConnectionProvider持有一个ConcurrentHashMap<ConnectionKey, Sync<K, V>>
## 还持有一个ClusterNodeConnectionFactory
## 这个map的key就是连接到连接到某个node，value是sync
## 这一步其实就是想要返回一个nodeConnection的连接，如果在map中就从map取，否则就去创建一个并且放入map中
	public ConnectionFuture<StatefulRedisConnection<K, V>> getConnectionAsync(ConnectionKey key) {
        return getConnectionSync(key).getConnectionAsync();
	}

## 所以看getConnectionSync(key) ，核心代码就是
## 又是putIfAbsent，就是说如果存在key就直接拿，不存在就创建connection并put进connections
## 因为持有了个clusterNodeConnectionFactory，所以创建nodeConnection就是有它创建，
## clusterNodeConnectionFactory创建nodeConnection的源码分析在<1.9>
Sync<k, V> sync = connections.computeIfAbsent(key, connectionKey -> {
	InProgress<K, V> createdSync = new InProgress<>(key, connectionFactory.apply(key), connections){
	if(closed) {
        createdSync.remove = InProgress.STFINISHED;
        createdSync.future.thenAcceptAsync(StatefulConnection::close);
	}
	return createdSync;
        });
    return sync;
}

## 接着就看sync.getConnectionAsync，是个接口，有2种实现，InProcess和Finished
## InProcess就是处理中的逻辑，Finished就是return future;
```

#### 1.9. ClusterNodeConnectionFactory源码解析

```
## ClusterNodeConnectionFactory默认是DefaultClusterNodeConnectionFactory，
## 是PooledClusterConnectionProvider的静态内部类
## 持有redisClusterClient，redisCodeC，clusterWriter
## 我们关注看connectionFactory.apply方法

## 我们发现，只要connectionKey中带有nodeId属性，就会用nodeId去连接，而且是
## 调用redisClusterClient.connectToNodeAsync
public ConnectionFuture<StatefulRedisConnection<K, V>> apply(ConnectionKey key) {

            if (key.nodeId != null) {
                // NodeId connections do not provide command recovery due to cluster reconfiguration
                return redisClusterClient.connectToNodeAsync(redisCodec, key.nodeId, null, getSocketAddressSupplier(key));
            }

            // Host and port connections do provide command recovery due to cluster reconfiguration
            return redisClusterClient.connectToNodeAsync(redisCodec, key.host + ":" + key.port, clusterWriter,
                    getSocketAddressSupplier(key));
        }
        
## redisClusterClient.connectToNodeAsync()
	<K, V> ConnectionFuture<StatefulRedisConnection<K, V>> connectToNodeAsync(RedisCodec<K, V> codec, String nodeId,
            RedisChannelWriter clusterWriter, Mono<SocketAddress> socketAddressSupplier) {

        assertNotNull(codec);
        assertNotEmpty(initialUris);
        LettuceAssert.notNull(socketAddressSupplier, "SocketAddressSupplier must not be null");

        ClusterNodeEndpoint endpoint = new ClusterNodeEndpoint(clientOptions, getResources(), clusterWriter);

        RedisChannelWriter writer = endpoint;

        if (CommandExpiryWriter.isSupported(clientOptions)) {
            writer = new CommandExpiryWriter(writer, clientOptions, clientResources);
        }

## 这里就是我们创建的nodeConnection
## timeout与command执行的超时时间是同一个timeout，都是abstractRedisClient.timeout,默认是60秒
## 在lettuceClientConfiguration中配置的commandTimeout()
        StatefulRedisConnectionImpl<K, V> connection = new StatefulRedisConnectionImpl<>(writer, codec, timeout);

        ConnectionFuture<StatefulRedisConnection<K, V>> connectionFuture = connectStatefulAsync(connection, codec, endpoint,
                getFirstUri(), socketAddressSupplier, () -> new CommandHandler(clientOptions, clientResources, endpoint));

## 如果有异常，会关闭该连接的
        return connectionFuture.whenComplete((conn, throwable) -> {
            if (throwable != null) {
                connection.close();
            }
        });
    }
```

#### 1.10. 总结

分析到这里，redisTemplate.get("key")的整个流程都结束了

概括起来就是

1、LettuceConnectionFactory持有了Cluster与Client的各种信息，从类图可以看出来



2、redisTemplate.get("key")就是执行执行action.doInRedis

首先是会去获得一个连接，由PoolingClusterConnectionProvider提供的StatefulRedisClusterConnection实例

PoolingClusterConnectionProvider持有个map<连接类型，池>，池中的连接创建是通过ClusterConnectionProvider<1.6>去创建StatefulRedisClusterConnection对象，池配置就是poolConfig

每次通过PoolingClusterConnectionProvider获取一个实例就是2步操作

（1）判断连接类型获取池

（2）池.borrowObject();

最后return StatefulRedisClusterConnection.sync()



3、StatefulRedisClusterConnection.sync()返回的是一个RedisAdvancedClusterCommands，这是个接口，由Proxy去执行

StatefulRedisClusterConnectionImpl持有sync和async，调用sync实际上也是通过调用async实现的，源码在<1.7>

所以真正的操作都在RedisAdvancedClusterAsyncCommandsImpl中执行的，里面都是异步操作



4、RedisAdvancedClusterAsyncCommandsImpl中的执行逻辑（异步执行），大概就是

（1）知道了要执行的命令是什么，参数是什么

（2）通过参数（如key，读写配置等等），计算slot，并且找到所能连接的节点信息是什么

（3）获得该节点的连接，通过channelwriter异步写入请求

（4）在redisTemplate.execute中, T result = action.doInRedis(connToExpose);

return postProcessResult(result, connToUse, existingConnection);异步获得了result并返回



5、第4步中的第（3）点，是由PooledClusterConnectionProvider提供连接（异步的方式提供）

它也持有一个map<ConnectionKey, Sync<>>

就是说ConnectionKey是标识哪个节点，Sync就是具体的连接（由clusterNodeConnectionFactory提供）

如果没有key则创建连接并put进map，如果有key则return 该连接——get(key)



6、NodeConnectionFactory调用了RedisClusterClient.connectToNodeAsync()方法，创建连接，可以用nodeId创建，也可以用host:port创建



7、timeout参数在lettuceClientConfiguration中配置，command获取结果与创建连接的超时时间是同一个timeout，lettuceClientConfiguration.commandTimeout()，会被set到AbstractRedisClient.timeout成员

### 2. 刷新集群拓扑

#### 2.1. 集群刷新配置

（1）调用 `RedisClusterClient.reloadPartitions` 

（2）周期性刷新

（3）触发MOVED，ASK，PERSISTENT_RECONNECTS 重定向时自适应刷新

```
ClusterTopologyRefreshOptions topologyRefreshOptions = ClusterTopologyRefreshOptions.builder()
                ##开启周期性刷新，10秒执行一次
                .enablePeriodicRefresh(refreshPeriod(10, TimeUnit.MINUTES))
                ##开启自适应刷新
                .enableAllAdaptiveRefreshTriggers()
                .build();

client.setOptions(ClusterClientOptions.builder()
                       .topologyRefreshOptions(topologyRefreshOptions)
                       .build());
```

默认情况是是都不开启的

参数如下：

| Name                                                         | Method                             | Default      |
| ------------------------------------------------------------ | ---------------------------------- | ------------ |
| 开启周期性刷新                                               | `enablePeriodicRefresh`            | `false`      |
| 周期性刷新间隔时间                                           | `refreshPeriod`                    | `60 SECONDS` |
| 自适应刷新触发器                                             | `enableAdaptiveRefreshTrigger`     | `(none)`     |
| 自适应刷新任务一次只会触发一个，触发多个只执行第一个，该任务跑的超时时间 | `adaptiveRefreshTriggersTimeout`   | `30 SECONDS` |
| 失联后Reconnect的重试次数                                    | `refreshTriggersReconnectAttempts` | `5`          |
| 允许集群加入新的node且会被扫描到topology，false则只会扫描初始配置的节点 | `dynamicRefreshSources`            | `true`       |
| 重定向最大次数                                               | `maxRedirects`                     | `5`          |
| 允许连接到节点前先验证节点在集群内                           | `validateClusterNodeMembership`    | `true`       |

#### 2.2. 刷新拓扑关键源码分析

```
## 重新加载partitions的方法

public void reloadPartitions() {

        if (partitions == null) {
            initializePartitions();
            partitions.updateCache();
        } else {

            Partitions loadedPartitions = loadPartitions();
            if (TopologyComparators.isChanged(getPartitions(), loadedPartitions)) {

                logger.debug("Using a new cluster topology");

                List<RedisClusterNode> before = new ArrayList<>(getPartitions());
                List<RedisClusterNode> after = new ArrayList<>(loadedPartitions);

                getResources().eventBus().publish(new ClusterTopologyChangedEvent(before, after));
            }

## 重新加载Partitions
            this.partitions.reload(loadedPartitions.getPartitions());
        }
        
## 更新连接的Partitions
        updatePartitionsInConnections();
    }
```

开启自动刷新配置的方法

就是执行genericWorkerPool.scheduleAtFixedRate(...）方法，开启一个定时任务

定时任务的内容就是执行reloadPartitions()

```
private void activateTopologyRefreshIfNeeded() {

        if (getOptions() instanceof ClusterClientOptions) {
            ClusterClientOptions options = (ClusterClientOptions) getOptions();
            ClusterTopologyRefreshOptions topologyRefreshOptions = options.getTopologyRefreshOptions();

            if (!topologyRefreshOptions.isPeriodicRefreshEnabled() || clusterTopologyRefreshActivated.get()) {
                return;
            }

            if (clusterTopologyRefreshActivated.compareAndSet(false, true)) {
                ScheduledFuture<?> scheduledFuture = genericWorkerPool.scheduleAtFixedRate(clusterTopologyRefreshScheduler,
                        options.getRefreshPeriod().toNanos(), options.getRefreshPeriod().toNanos(), TimeUnit.NANOSECONDS);
                clusterTopologyRefreshFuture.set(scheduledFuture);
            }
        }
    }
```

#### 2.3. 更新连接中的partions源码分析（ClusterConnection）

```
## 就是执行这个方法
protected void updatePartitionsInConnections() {

        forEachClusterConnection(input -> {
            input.setPartitions(partitions);
        });

        forEachClusterPubSubConnection(input -> {
            input.setPartitions(partitions);
        });
    }
## 我们关注在forEachClusterConnection()
   protected void forEachClusterConnection(Consumer<StatefulRedisClusterConnectionImpl<?, ?>> function) {
        forEachCloseable(input -> input instanceof StatefulRedisClusterConnectionImpl, function);
    }
## 再看一下forEachCloseable()
## 上面几个方法都在clusterRedisClient中
## closeableResources是abstractRedisClient中的一个成员，是个ConcurrentSet<Closeable>
## 这个函数的意思就是，遍历closeableResources，如果closeable instanceof 
## StatefulRedisClusterConnectionImpl，就setPartitions(partitions);
    protected <T extends Closeable> void forEachCloseable(Predicate<? super Closeable> selector, Consumer<T> function) {
        for (Closeable c : closeableResources) {
            if (selector.test(c)) {
                function.accept((T) c);
            }
        }
    }
    
## 这么看起来，它就是对所有closeable的连接，进行了setPartitions的操作，就更新了他们的partitions
## 那么这个closeable里面有哪些内容呢
## 回到RedisClusterClient的代码，我们每次创建连接，都是调用它的connectClusterPubSubAsync或者
## connectClusterAsync方法，代码可以参考<1.6>

        return connectionMono
                .flatMap(c -> c.reactive().command().collectList() //
                        .map(CommandDetailParser::parse) //
                        .doOnNext(detail -> c.setState(new RedisState(detail))) //
                                .doOnError(e -> c.setState(new RedisState(Collections.emptyList()))).then(Mono.just(c))
                                .onErrorResume(RedisCommandExecutionException.class, e -> Mono.just(c)))
## 就是在这里，做了registerCloseables的操作
                .doOnNext(
                        c -> connection.registerCloseables(closeableResources, clusterWriter, pooledClusterConnectionProvider))
                .map(it -> (StatefulRedisClusterConnection<K, V>) it).toFuture();
                
## 也就是说，创建的clusterConnection（也是个future），都是会加入closeableResources里面的
## 这里我的理解是，尝试连接，成功连接即连接成功
## 连接失败就doOnError，然后执行doErrorResume（重新尝试连接，默认5次）
## 然后注册到可关闭的连接closeableResources，可关闭的连接会被更新partitions
## 而正在执行动作的连接，会把当前动作做完
```

#### 2.4. 更新连接中的partions源码分析（NodeConnection）

```
## 通过上文<1.9>我们之前，创建nodeConnection是通过调用RedisCluster.connectToNodeAsync()方法的
    <K, V> ConnectionFuture<StatefulRedisConnection<K, V>> connectToNodeAsync(RedisCodec<K, V> codec, String nodeId,
            RedisChannelWriter clusterWriter, Mono<SocketAddress> socketAddressSupplier) {

        assertNotNull(codec);
        assertNotEmpty(initialUris);
        LettuceAssert.notNull(socketAddressSupplier, "SocketAddressSupplier must not be null");

        ClusterNodeEndpoint endpoint = new ClusterNodeEndpoint(clientOptions, getResources(), clusterWriter);

        RedisChannelWriter writer = endpoint;

        if (CommandExpiryWriter.isSupported(clientOptions)) {
            writer = new CommandExpiryWriter(writer, clientOptions, clientResources);
        }

        StatefulRedisConnectionImpl<K, V> connection = new StatefulRedisConnectionImpl<>(writer, codec, timeout);

## 重点在这段代码，就是去创建一个statefulConnection，也是异步的
        ConnectionFuture<StatefulRedisConnection<K, V>> connectionFuture = connectStatefulAsync(connection, codec, endpoint,
                getFirstUri(), socketAddressSupplier, () -> new CommandHandler(clientOptions, clientResources, endpoint));

        return connectionFuture.whenComplete((conn, throwable) -> {
            if (throwable != null) {
                connection.close();
            }
        });
    }
## 看一下connectionStatefulAsync方法是干嘛的
	private <K, V, T extends RedisChannelHandler<K, V>, S> ConnectionFuture<S> connectStatefulAsync(T connection,
            RedisCodec<K, V> codec,
            DefaultEndpoint endpoint, RedisURI connectionSettings, Mono<SocketAddress> socketAddressSupplier,
            Supplier<CommandHandler> commandHandlerSupplier) {

        ConnectionBuilder connectionBuilder = createConnectionBuilder(connection, endpoint, connectionSettings,
                socketAddressSupplier, commandHandlerSupplier);

        if (clientOptions.isPingBeforeActivateConnection()) {
            if (hasPassword(connectionSettings)) {
                connectionBuilder.enableAuthPingBeforeConnect();
            } else {
                connectionBuilder.enablePingBeforeConnect();
            }
        }
## 上面的逻辑就不详述，可以看的出来，我们关注在这个方法，创建channelHandler，异步创建
        ConnectionFuture<RedisChannelHandler<K, V>> future = initializeChannelAsync(connectionBuilder);
        ConnectionFuture<?> sync = future;

        if (!clientOptions.isPingBeforeActivateConnection() && hasPassword(connectionSettings)) {

            sync = sync.thenCompose(channelHandler -> {

                CommandArgs<K, V> args = new CommandArgs<>(codec).add(connectionSettings.getPassword());
                AsyncCommand<K, V, String> command = new AsyncCommand<>(new Command<>(CommandType.AUTH, new StatusOutput<>(
                        codec), args));

                if (connection instanceof StatefulRedisClusterConnectionImpl) {
                    ((StatefulRedisClusterConnectionImpl) connection).dispatch(command);
                }

                if (connection instanceof StatefulRedisConnectionImpl) {
                    ((StatefulRedisConnectionImpl) connection).dispatch(command);
                }

                return command;
            });

        }

        if (LettuceStrings.isNotEmpty(connectionSettings.getClientName())) {
            sync = sync.thenApply(channelHandler -> {

                if (connection instanceof StatefulRedisClusterConnectionImpl) {
                    ((StatefulRedisClusterConnectionImpl) connection).setClientName(connectionSettings.getClientName());
                }

                if (connection instanceof StatefulRedisConnectionImpl) {
                    ((StatefulRedisConnectionImpl) connection).setClientName(connectionSettings.getClientName());
                }
                return channelHandler;
            });
        }

        return sync.thenApply(channelHandler -> (S) connection);
    }
    
## 看一下InitChannelAsync方法是干嘛的，在AbstractRedisClient类中
## 在这边就需要去创建channel
	protected <K, V, T extends RedisChannelHandler<K, V>> ConnectionFuture<T> initializeChannelAsync(
            ConnectionBuilder connectionBuilder) {

## socketAddress提供者
        Mono<SocketAddress> socketAddressSupplier = connectionBuilder.socketAddress();

        if (clientResources.eventExecutorGroup().isShuttingDown()) {
            throw new IllegalStateException("Cannot connect, Event executor group is terminated.");
        }

        CompletableFuture<SocketAddress> socketAddressFuture = new CompletableFuture<>();
        CompletableFuture<Channel> channelReadyFuture = new CompletableFuture<>();

        socketAddressSupplier.doOnError(socketAddressFuture::completeExceptionally).doOnNext(socketAddressFuture::complete)
                .subscribe(redisAddress -> {

                    if (channelReadyFuture.isCancelled()) {
                        return;
                    }
## 创建的逻辑就在这个方法中
                    initializeChannelAsync0(connectionBuilder, channelReadyFuture, redisAddress);
                }, channelReadyFuture::completeExceptionally);

        return new DefaultConnectionFuture<>(socketAddressFuture, channelReadyFuture.thenApply(channel -> (T) connectionBuilder
                .connection()));
    }
    
## initializeChannelAsync0（）
	private void initializeChannelAsync0(ConnectionBuilder connectionBuilder, CompletableFuture<Channel> channelReadyFuture,
            SocketAddress redisAddress) {

        logger.debug("Connecting to Redis at {}", redisAddress);

        Bootstrap redisBootstrap = connectionBuilder.bootstrap();

        RedisChannelInitializer initializer = connectionBuilder.build();
        redisBootstrap.handler(initializer);

        clientResources.nettyCustomizer().afterBootstrapInitialized(redisBootstrap);
## 这个是channel创建的future
        CompletableFuture<Boolean> initFuture = initializer.channelInitialized();
## 连接到redisAddress（就是连接到那个节点）的future
        ChannelFuture connectFuture = redisBootstrap.connect(redisAddress);

        channelReadyFuture.whenComplete((c, t) -> {

            if (t instanceof CancellationException) {
                connectFuture.cancel(true);
                initFuture.cancel(true);
            }
        });

        connectFuture.addListener(future -> {

            if (!future.isSuccess()) {

                logger.debug("Connecting to Redis at {}: {}", redisAddress, future.cause());
                connectionBuilder.endpoint().initialState();
                channelReadyFuture.completeExceptionally(future.cause());
                return;
            }

            initFuture.whenComplete((success, throwable) -> {

                if (throwable == null) {

                    logger.debug("Connecting to Redis at {}: Success", redisAddress);
                    RedisChannelHandler<?, ?> connection = connectionBuilder.connection();
## 在这里连接到节点已经成功了，而且通道也建立成功了
## 注册到closeableResources中
                    connection.registerCloseables(closeableResources, connection);
                    channelReadyFuture.complete(connectFuture.channel());
                    return;
                }

                logger.debug("Connecting to Redis at {}, initialization: {}", redisAddress, throwable);
                connectionBuilder.endpoint().initialState();
                Throwable failure;

                if (throwable instanceof RedisConnectionException) {
                    failure = throwable;
                } else if (throwable instanceof TimeoutException) {
                    failure = new RedisConnectionException("Could not initialize channel within "
                            + connectionBuilder.getTimeout(), throwable);
                } else {
                    failure = throwable;
                }
                channelReadyFuture.completeExceptionally(failure);
            });
        });
    }

```

#### 2.5. 总结

集群创建连接获得拓扑都是依托于Partitions对象进行操作，且是线程安全的操作

具体的连接逻辑都是依托与Partitions对象的，执行动作中的连接会先把动作执行完

而执行完动作的连接（包括集群和node的connection）如果执行刷新任务的时候，会被触发setPartitions的动作

刷新任务就是可以由定时任务开启，也可以由触发器触发

故而实现了刷新的功能