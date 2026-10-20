# 新项目编码规则（从 0 创建版 — Java 后端 / Spring Boot）

> 用法：新建项目后复制到项目根目录，重命名为 `AGENTS.md`（或并入 `QODER.md`）。
> 按项目实际删改，保持精简，规则必须可验证。

## 一、总原则

1. 先骨架后血肉：先把目录结构、统一返回体、全局异常、日志、配置文件搭好并跑通空项目，再写第一个业务接口
2. 最小可落地方案：需求没明确要求，禁止引入多模块、微服务、接口化、配置化；单模块能解决就不拆
3. 技术栈一旦锁定禁止中途更换（见第二节）；缺能力时优先用现有依赖解决，引入新依赖必须先征得同意
4. 精准修改：只改任务所需文件；修 bug 先找根因；需求有歧义先提问
5. 禁止为"未来可能的需求"写代码（YAGNI）

## 二、技术栈锁定（创建时确定，中途不得更换）

| 项 | 约定 |
|---|---|
| JDK | Java 17（LTS） |
| 框架 | Spring Boot 3.x 稳定版（禁止 SNAPSHOT/RC） |
| ORM | MyBatis-Plus |
| 数据库 | MySQL 8.x |
| 缓存 | Redis（需要时再加） |
| 构建 | Maven，版本号统一在父 pom `properties` 管理 |
| 工具库 | Lombok、Hutool（按需），禁功能重复依赖 |

## 三、初始化清单（第一个 commit 前必须完成）

1. `.gitignore`：target/、.idea/、*.iml、logs/、application-local.yml
2. `application.yml` 三环境分离：dev（本地）/ test / prod；密码、密钥只在 local 或环境变量，禁止提交
3. 统一返回体 `Result<T>` + 错误码枚举 `ErrorCode` + 全局异常处理器（`@RestControllerAdvice`）先行建好
4. 日志：SLF4J，logback 配置输出到文件，格式含 traceId（为链路排查留口）
5. 包结构一次定好（见第四节），禁止随写随造包

## 四、目录结构（单模块标准）

```
com.<域名>.<项目名>
├── controller      # 只做参数校验 + 调 Service + 封装返回
├── service         # 接口 + impl；业务逻辑唯一所在地
├── mapper          # MyBatis-Plus Mapper
├── entity          # 数据库实体
├── dto             # 请求/响应对象（禁止直接用 entity 接收入参或返回）
├── config          # 配置类（Redis、线程池、拦截器等）
├── common          # Result、ErrorCode、全局异常、常量
├── util            # 工具类
└── XxxxApplication.java
```

## 五、代码规则（高频红线摘选）

1. 命名：类 UpperCamelCase、方法/变量 lowerCamelCase、常量 UPPER_SNAKE_CASE；禁拼音混用、禁魔法值
2. Controller 禁写业务逻辑；Controller 不得直接调 Mapper；跨层调用禁止
3. 禁止空 catch；异常统一抛业务异常由全局处理器兜底；日志用占位符，禁 System.out，禁打印敏感信息明文
4. 事务 `@Transactional(rollbackFor = Exception.class)` 只加在 Service；事务内禁 RPC/HTTP/文件 IO
5. SQL 禁 `select *`、禁 `${}` 拼接（预编译 `#{}`）；分页大小上限 100 且入参校验
6. 所有对外接口必须有鉴权/登录校验；入参一律 `@Valid` 校验
7. 线程池必须 `ThreadPoolExecutor` 显式建参，禁 `Executors` 快捷方法

## 六、数据库

1. 每表必有：`id bigint` 主键、`create_time`、`update_time`、`deleted`（逻辑删除，用则统一 MyBatis-Plus `@TableLogic`）
2. 表和字段全部写注释；索引每表 ≤5 个
3. 建表 SQL 存 `docs/sql/` 或用 Flyway，禁止只在本地 Navicat 里改表结构

## 七、Git

1. 第一次提交就是规范提交：`<type>: <摘要>`，type ∈ feat/fix/refactor/test/docs/chore
2. 一次提交一件事；main 分支只进规范代码；禁止 --force、--no-verify
3. 每完成一个可运行的功能点就提交，禁止攒一大坨

## 八、完成定义（DoD）

编译通过 → 启动无报错 → 接口自测通过（curl/Postman）→ 无遗留警告 → 新增业务有测试。缺任何一项不得声称"完成"；无法验证的项必须明确说"未验证"。

## 九、AI 协作约束

1. 改动范围外的问题：报告，不擅自修
2. 引入新依赖、新建中间件依赖（Redis/Kafka/MQ）、改目录结构：必须先征得同意
3. 遵循本文件既定包结构和已有代码风格，禁止整文件重排
4. 生成配置时禁止硬编码任何密钥/密码，一律占位符 + 环境变量
