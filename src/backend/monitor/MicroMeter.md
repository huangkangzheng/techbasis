## 1. MeterRegistry

- 1、SimpleMeterRegistry：每个Meter的最新数据可以收集到SimpleMeterRegistry实例中，但是这些数据不会发布到其他系统，也就是数据是位于应用的内存中的。
- 2、CompositeMeterRegistry：多个MeterRegistry聚合，内部维护了一个MeterRegistry的列表。
- 3、全局的MeterRegistry：工厂类`io.micrometer.core.instrument.Metrics`中持有一个静态final的`CompositeMeterRegistry`实例globalRegistry

SpringBoot中采用了(3)

## 2. Tags

数据标记，用于表示数据维度，类型等等

如：

```java
MeterRegistry registry = ...
registry.counter("database.calls", "db", "user")
registry.counter("http.requests", "uri", "/api/users")
```

## 3. Meter

### 3.1. Counter

单值计数器，单调递增

`Counter`接口允许使用者使用一个固定值(必须为正数)进行计数。

使用示例：

```java
## 参数1为meter的name， 后面的参数为多个<K, V>的Tags

Metrics.counter("api.count", "uri", "/project/test").increment();
```

效果（通过访问/actuator/prometheus）：

```

```

### 3.2. Timer

Timer(计时器)适用于记录耗时比较短的事件的执行时间，通过时间分布展示事件的序列和发生频率

所有的Timer的实现至少记录了发生的事件的数量和这些事件的总耗时，从而生成一个时间序列

使用示例：

```java

```

效果（通过访问/actuator/prometheus）：

```
# HELP api_timer_cost_seconds_max  
# TYPE api_timer_cost_seconds_max gauge
api_timer_cost_seconds_max{application="chainAccessAdmin",uri="/project/test",} 0.0
# HELP api_timer_cost_seconds  
# TYPE api_timer_cost_seconds summary
api_timer_cost_seconds_count{application="chainAccessAdmin",uri="/project/test",} 2.0
api_timer_cost_seconds_sum{application="chainAccessAdmin",uri="/project/test",} 2001.0
```

Prometheus自带了HTTP请求监控（是个Summary）如下：

```
# HELP http_server_requests_seconds  
# TYPE http_server_requests_seconds summary
http_server_requests_seconds_count{application="chainAccessAdmin",exception="None",method="GET",status="200",uri="/project/test",} 2.0
http_server_requests_seconds_sum{application="chainAccessAdmin",exception="None",method="GET",status="200",uri="/project/test",} 2.1065459
http_server_requests_seconds_count{application="chainAccessAdmin",exception="None",method="GET",status="200",uri="/actuator/prometheus",} 3.0
http_server_requests_seconds_sum{application="chainAccessAdmin",exception="None",method="GET",status="200",uri="/actuator/prometheus",} 0.4188697
http_server_requests_seconds_count{application="chainAccessAdmin",exception="None",method="GET",status="200",uri="/**/favicon.ico",} 1.0
http_server_requests_seconds_sum{application="chainAccessAdmin",exception="None",method="GET",status="200",uri="/**/favicon.ico",} 0.0256645
# HELP http_server_requests_seconds_max  
# TYPE http_server_requests_seconds_max gauge
http_server_requests_seconds_max{application="chainAccessAdmin",exception="None",method="GET",status="200",uri="/project/test",} 0.0
http_server_requests_seconds_max{application="chainAccessAdmin",exception="None",method="GET",status="200",uri="/actuator/prometheus",} 0.0
http_server_requests_seconds_max{application="chainAccessAdmin",exception="None",method="GET",status="200",uri="/**/favicon.ico",} 0.0
```

### 3.3. LongTaskTimer

耗时长的任务选择LongTaskTimer

Timer 只有在任务完成之后才会记录时间

LongTaskTimer 可以在任务进行中记录已经耗费的时间

### 3.4. Gauge

Gauge(仪表)是获取当前度量记录值的句柄，也就是它表示一个可以任意变动的单数值度量Meter

如某个数值的波动与变化

使用示例：

```java
    private List<Integer> list = new ArrayList<>();
    {
        list.add(1);
        list.add(2);
        list.add(3);
        list.add(4);
        list.add(5);
        Metrics.gauge("list.count", list.size());
    }

    @GetMapping("test3")
    public void test3() {
        list.add(1);
    }
```

效果（通过访问/actuator/prometheus）：

```
# HELP list_count  
# TYPE list_count gauge
list_count{application="chainAccessAdmin",} 5.0
```

如果访问了/meter/test3之后则显示为：

```
# HELP meter_count  
# TYPE meter_count gauge
meter_count{application="chainAccessAdmin",propertyName="testList",} 6.0
```

### 3.5. DistributionSummary

分布概要（Distribution summary）用来记录事件的分布情况。计时器本质上也是一种分布概要。表示分布概要的类 `DistributionSummary` 可以从注册表中创建，也可以使用 `DistributionSummary.builder()` 提供的构建器来创建。分布概要根据每个事件所对应的值，把事件分配到对应的桶（bucket）中。Micrometer 默认的桶的值从 1 到最大的 long 值。可以通过 `minimumExpectedValue` 和 `maximumExpectedValue` 来控制值的范围。如果事件所对应的值较小，可以通过 scale 来设置一个值来对数值进行放大。与分布概要密切相关的是直方图和百分比（percentile）。大多数时候，我们并不关注具体的数值，而是数值的分布区间。比如在查看 HTTP 服务响应时间的性能指标时，通常关注是的几个重要的百分比，如 50%，75%和 90%等。所关注的是对于这些百分比数量的请求都在多少时间内完成。Micrometer 提供了两种不同的方式来处理百分比。

- 对于 Prometheus 这样本身提供了对百分比支持的监控系统，Micrometer 直接发送收集的直方图数据，由监控系统完成计算。
- 对于其他不支持百分比的系统，Micrometer 会进行计算，并把百分比结果发送到监控系统。

 使用示例：

```java
    private DistributionSummary summary = DistributionSummary.builder("api.summary")
            .tag("uri", "/meter/test4")
            .minimumExpectedValue(1L)
            .maximumExpectedValue(10L)
            .publishPercentiles(0.5, 0.75, 0.9)
            .register(Metrics.globalRegistry);
    @GetMapping("test4")
    public void test4() {
        summary.record(1);
        summary.record(1.3);
        summary.record(2.4);
        summary.record(3.5);
        summary.record(4.1);
        log.info(String.valueOf(summary.takeSnapshot()));
    }
```

效果（通过访问/actuator/prometheus）：

```
# HELP api_summary  
# TYPE api_summary summary
api_summary{application="chainAccessAdmin",uri="/meter/test4",quantile="0.5",} 2.4375
api_summary{application="chainAccessAdmin",uri="/meter/test4",quantile="0.75",} 3.5625
api_summary{application="chainAccessAdmin",uri="/meter/test4",quantile="0.9",} 4.1875
api_summary_count{application="chainAccessAdmin",uri="/meter/test4",} 5.0
api_summary_sum{application="chainAccessAdmin",uri="/meter/test4",} 12.299999999999999
# HELP api_summary_max  
# TYPE api_summary_max gauge
api_summary_max{application="chainAccessAdmin",uri="/meter/test4",} 4.1
```

