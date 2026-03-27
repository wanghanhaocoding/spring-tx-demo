# spring-tx-demo

一个用 **Spring Boot + MyBatis + MySQL** 编写的事务学习项目，专门用来理解 Spring 的事务传播机制、回滚规则，以及代理边界问题。

这个项目的目标不是做复杂业务，而是通过一个尽量小、尽量直观的转账场景，把 Spring 事务讲清楚。

---

## 项目特点

- 使用 **Spring Boot 3**
- ORM 使用 **MyBatis**
- 主运行环境使用 **MySQL**
- 测试环境使用 **H2 内存数据库**
- 覆盖 **7 种事务传播机制**
- 包含可运行示例和测试用例
- 包含 self-invocation 导致事务失效的演示

---

## 这个项目讲什么

### 1. 基础事务行为

- 正常执行时提交
- 抛出 `RuntimeException` 时回滚
- checked exception 默认不回滚
- `rollbackFor` 如何让 checked exception 也回滚

### 2. 7 种事务传播机制

- `REQUIRED`
- `REQUIRES_NEW`
- `SUPPORTS`
- `NOT_SUPPORTED`
- `MANDATORY`
- `NEVER`
- `NESTED`

### 3. 常见事务坑

- 为什么 self-invocation 会导致 `@Transactional` 失效
- 为什么事务通常放在 Service 层
- `REQUIRES_NEW`、`NOT_SUPPORTED`、`NESTED` 的区别

---

## 项目结构

```text
src/main/java/com/example/transactiondemo/
├── account/
│   ├── Account.java
│   ├── AccountMapper.java
│   └── AccountService.java
├── audit/
│   ├── AuditLog.java
│   ├── AuditLogMapper.java
│   └── AuditService.java
├── demo/
│   └── DemoRunner.java
├── tx/
│   ├── PropagationDemoService.java
│   ├── ProxyBoundaryDemoService.java
│   ├── RollbackDemoService.java
│   ├── SelfInvocationDemoService.java
│   └── exception/
└── TransactionTeachingApplication.java
```

---

## 如何运行主程序

### 1. 准备 MySQL

先创建数据库：

```sql
create database transaction_demo;
```

### 2. 修改配置

编辑：

- `src/main/resources/application.yml`

把下面这几个值改成你自己的：

- `spring.datasource.url`
- `spring.datasource.username`
- `spring.datasource.password`

默认配置只是占位示例，不是实际账号：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/transaction_demo?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: demo_user
    password: demo_password
```

### 3. 启动项目

```bash
mvn spring-boot:run
```

启动后，`DemoRunner` 会自动顺序打印事务示例。

---

## 如何运行测试

测试不依赖你本机 MySQL，直接使用 H2 内存数据库。

运行全部测试：

```bash
mvn test
```

如果你只想看某一类测试，可以重点关注：

- `AccountServiceTest`：基础事务提交 / 回滚
- `RollbackDemoTest`：异常与回滚规则
- `PropagationDemoTest`：7 种传播机制
- `SelfInvocationDemoTest`：代理边界与自调用失效

---

## 7 种传播机制对应位置

### REQUIRED
- `account/AccountService.java`
- 默认传播行为

### REQUIRES_NEW
- `audit/AuditService.java`
- `tx/PropagationDemoService.java`

### SUPPORTS
- `audit/AuditService.java`
- `tx/PropagationDemoService.java`

### NOT_SUPPORTED
- `audit/AuditService.java`
- `tx/PropagationDemoService.java`

### MANDATORY
- `audit/AuditService.java`
- `tx/PropagationDemoService.java`

### NEVER
- `audit/AuditService.java`
- `tx/PropagationDemoService.java`

### NESTED
- `audit/AuditService.java`
- `tx/PropagationDemoService.java`

---

## 公司里最常用的是哪些

通常最常见的是：

1. `REQUIRED`
   默认值，绝大多数业务 Service 方法都在用

2. `REQUIRES_NEW`
   常用于审计日志、失败记录、补偿记录

3. `SUPPORTS`
   偶尔用于查询类或辅助类方法

相对少见但值得理解：

- `MANDATORY`
- `NOT_SUPPORTED`
- `NEVER`
- `NESTED`

---

## 你可以重点观察的区别

### REQUIRED
有事务就加入，没有事务就新开

### REQUIRES_NEW
挂起外层事务，自己开一个新事务

### NOT_SUPPORTED
挂起外层事务，自己在无事务状态下执行

### NESTED
不新开事务，而是在当前事务里建立 savepoint

---

## 适合怎么学

建议按这个顺序看：

1. `AccountServiceTest`
2. `RollbackDemoTest`
3. `PropagationDemoTest`
4. `SelfInvocationDemoTest`
5. `DemoRunner`

如果你想真正理解事务，建议一边跑测试，一边打断点看：

- 账户余额什么时候变化
- 抛异常后为什么回滚
- 不同传播行为为什么结果不同

---

## 注意事项

- 主程序运行依赖 MySQL
- 测试运行依赖 H2
- `NESTED` 的表现依赖底层事务管理器和数据库对 savepoint 的支持
- `@Transactional` 一般基于 Spring AOP 代理实现，因此 self-invocation 场景不会按直觉生效

---

## 许可证

如果你需要，可以自行补充 License 文件。
