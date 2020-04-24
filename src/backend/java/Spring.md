Bean生命周期

（BeanFactory相关）

加载BeanDefinition

实例化Bean对象

设置对象属性

检查Aware相关接口并设置相关依赖

BeanPostProcessor前置处理

@PostContruct

检查是否是InitializationBean及决定是否调用afterPropertiesSet()

检查是否有自定义的init-method()

BeanPostProcessor后置处理

注册必要的Destruction相关回调接口



使用......



是否实现DisposableBean接口

是否配置有自定义的destroy()接口