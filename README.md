# Pixel Studio · 终端像素画与动画工具

> 一个用来自学 Java 的小项目：在终端里画像素图、做逐帧动画、导出成图片。
>
> 全程 100% Java 标准库，**没有任何第三方运行时依赖**，约 2700 行代码覆盖了 Java 最重要的那些知识点。

---

## 目录

- [一、这个项目是什么](#一这个项目是什么)
- [二、跑起来（3 分钟）](#二跑起来3-分钟)
- [三、命令速查](#三命令速查)
- [四、完整玩法演示](#四完整玩法演示)
- [五、项目结构](#五项目结构)
- [六、Java 知识点地图](#六java-知识点地图)
- [七、文件格式](#七文件格式)
- [八、学习路线（5 个阶段）](#八学习路线5-个阶段)
- [九、动手练习](#九动手练习)
- [十、常见问题](#十常见问题)
- [十一、发布到 GitHub](#十一发布到-github)

---

## 一、这个项目是什么

**一句话**：一个跑在命令行里的迷你绘图 + 动画工具，用文字命令在终端画像素图，能存盘、能导出成真图片。

### 为什么选这个项目来学 Java

学 Java 最容易掉进的坑是：看了两章语法，写了个 `HelloWorld`，然后不知道下一步该写什么。
所以我选了一个**有真实功能、能立刻看到结果、但结构足够复杂**的项目：

- **立刻能看到结果** —— 敲一行 `show`，屏幕上就出现图案。多巴胺有了，才学得下去。
- **规模刚刚好** —— 20 个源文件，每个几十到两百行。你可以逐行读完，不用怕"这代码我看不懂"。
- **知识密度高** —— 它不是玩具，Java 面试和实际工作里的核心概念，这里几乎全用到了（见[第六节](#六java-知识点地图)）。
- **零第三方依赖** —— 只用 JDK 自带的东西，不用先去学 Spring Boot、数据库、HTTP。学 Java 本身就得先把 JDK 学明白。

### 它能做什么

| 能力 | 说明 |
|------|------|
| 画基本图形 | 点、线、矩形、圆、文字 |
| 上色 | 18 色调色板，终端里显示为彩色 |
| 做动画 | 多帧 + 可调帧率 + 循环播放 |
| 存盘 | 存成人类可读的纯文本格式 `.pxs`，可以手改 |
| 导出 | `.txt` 字符画 / `.ppm` / `.bmp` 图片（双击就能看） |
| 批量执行 | 命令脚本，可以当"代码"复用 |

---

## 二、跑起来（3 分钟）

### 环境要求

- **JDK 17 或更高**（用到 `switch` 表达式、文本块这些新语法）
- **Maven 3.6+**（只是用来编译和跑测试，不学 Maven 也没关系，照抄命令即可）

检查一下：

```bash
java -version    # 应该显示 17 或更高
mvn -v
```

> 装 JDK 建议直接下 [Eclipse Temurin](https://adoptium.net/)（免费、免安装、解压即用）。
> 装完记得配好 `JAVA_HOME` 环境变量。

### 方式一：用 Maven（推荐，功能最全）

```bash
# 1. 进到项目目录
cd java-project

# 2. 直接启动
mvn compile exec:java

# 3. 跑测试（看 24 个测试全部通过）
mvn test

# 4. 打包成可执行 jar
mvn package
java -jar target/pixel-studio.jar
```

### 方式二：只用 JDK（不想碰 Maven）

```bash
# Windows（PowerShell / cmd）
dir /s /b src\main\java\*.java > sources.txt
javac -encoding UTF-8 -d out @sources.txt
java -cp out com.pixelstudio.PixelStudioApp

# macOS / Linux
find src/main/java -name "*.java" > sources.txt
javac -encoding UTF-8 -d out @sources.txt
java -cp out com.pixelstudio.PixelStudioApp
```

> ⚠️ 记得加 `-encoding UTF-8`，否则中文注释和字符串会乱码。

### 方式三：跑示例脚本

```bash
java -cp target/classes com.pixelstudio.PixelStudioApp examples/bouncing-ball.pxs
```

会一次性执行一个动画脚本，生成 `out/bouncing-ball.bmp`。

### 第一次进去之后

```
  ╔══════════════════════════════════════════╗
  ║   Pixel Studio · 终端像素画 & 动画工具      ║
  ╚══════════════════════════════════════════╝

  输入 help 看命令列表，quit 退出。
  试试： new 画布 40 16  然后  show --ruler

> 
```

**照着敲这三行，立刻能看到东西：**

```
new 画布 40 16
fill 10 3 20 3 red
show --ruler
```

---

## 三、命令速查

输入 `help` 看完整列表，输入 `help <命令名>` 看某条命令的详细用法。

### 画图

| 命令 | 作用 | 例子 |
|------|------|------|
| `fill <x> <y> <宽> <高> [色]` | 实心矩形 | `fill 2 2 10 5 red` |
| `rect <x> <y> <宽> <高> [色]` | 空心矩形 | `rect 2 2 10 5 blue` |
| `line <x0> <y0> <x1> <y1> [色]` | 直线 | `line 0 0 20 10 green` |
| `circle <cx> <cy> <r> [色]` | 空心圆 | `circle 12 8 5 yellow` |
| `disc <cx> <cy> <r> [色]` | 实心圆 | `disc 12 8 5 yellow` |
| `set <x> <y> [色]` | 单个点 | `set 3 3 red` |
| `text <x> <y> <内容> [色]` | 写字（3×5 点阵字体） | `text 2 1 "HI" cyan` |
| `clear [色]` | 清空画布 | `clear` |
| `show [--ruler] [--nocolor]` | 预览当前帧 | `show --ruler` |

### 颜色

| 命令 | 作用 |
|------|------|
| `color` | 显示调色板 |
| `color <名字>` | 设置当前画笔颜色，中英文都行：`red` / `红` / `cyan` / `青` |

颜色也可以直接敲**屏幕上的那个字符**：`line 0 0 10 10 x` 和 `... gray` 是一个意思（`x` 就是灰色画出来的样子）。

不写颜色的命令，默认用当前画笔色。

### 帧与动画

| 命令 | 作用 |
|------|------|
| `frames` | 列出所有帧 |
| `frame <序号>` | 切换帧（`next` / `prev` 也可以） |
| `add` | 在当前帧后插入空白帧 |
| `copy` | **复制当前帧**（做动画最常用） |
| `del <序号>` | 删除帧 |
| `fps <数字>` | 播放帧率，1~60 |
| `loop <on\|off>` | 是否循环播放 |
| `play [轮数]` | 播放动画，`Ctrl+C` 停止 |

### 文件

| 命令 | 作用 |
|------|------|
| `save <文件名>` | 存成 `.pxs`（不写扩展名会自动补） |
| `load <文件名>` | 读取项目（会清空当前内容） |
| `export <文件名.txt>` | 导出字符画 |
| `export <文件名.ppm>` | 导出纯文本像素图 |
| `export <文件名.bmp>` | 导出图片，**双击用「画图」打开** |

### 会话

| 命令 | 作用 |
|------|------|
| `new <名字> <宽> <高>` | 新建项目 |
| `info` | 看项目状态：帧数、帧率、颜色分布 |
| `rename <名字>` | 改项目名 |
| `help [命令]` | 帮助 |
| `quit` / `exit` / `q` | 退出 |

---

## 四、完整玩法演示

### 4.1 画一只小幽灵

```
new 幽灵 24 14

# 身体：上面一个半圆，下面补一个矩形
color white
disc 12 6 5 white
fill 7 6 11 6 white

# 眼睛
color black
set 9 5
set 10 5
set 14 5
set 15 5

# 嘴巴
set 11 8
set 12 8

show --ruler
```

### 4.2 做一段弹跳球动画

核心套路就三步：**画好第一帧 → `copy` → 在副本上挪一点点**。

```
new 弹跳球 24 12
color gray
fill 0 11 24 1          # 地面
color red
disc 3 8 2              # 球在左边

copy                    # 复制一帧
clear
fill 0 11 24 1
disc 5 6 2              # 球挪到中间偏上

copy
clear
fill 0 11 24 1
disc 7 4 2              # 再往右往上

fps 10
play                    # 循环播放，Ctrl+C 停
```

完整版在 [`examples/bouncing-ball.pxs`](examples/bouncing-ball.pxs)。小幽灵的完整版在 [`examples/ghost.pxs`](examples/ghost.pxs)。

### 4.3 导出成图片

```
export out/ghost.bmp
```

然后去文件夹里双击 `ghost.bmp`，用「画图」打开，就能看到你的像素画了。
**这是整个项目最爽的一步** —— 从一段 Java 代码，到一张能存进相册的图片。

### 4.4 批量执行（脚本模式）

不想每次手敲？把命令存成文件批量跑：

```bash
java -cp target/classes com.pixelstudio.PixelStudioApp examples/smoke-test.pxs
```

脚本里 `#` 开头是注释，空行自动跳过。某一行的参数写错了也不会中断整个脚本。

---

## 五、项目结构

```
java-project/
├── pom.xml                              Maven 配置
├── README.md                            你正在读的这个
├── examples/                            示例命令脚本
│   ├── night-scene.pxs                  夜景（把绘图命令用在一起）
│   ├── smoke-test.pxs                   全功能冒烟测试（每条命令都用到）
│   ├── ghost.pxs                        小幽灵
│   └── bouncing-ball.pxs                弹跳球动画（演示帧的复制与移动）
└── src/
    ├── main/java/com/pixelstudio/
    │   ├── PixelStudioApp.java          ★ 程序入口（只负责组装）
    │   ├── model/                       ── 数据模型：不认识"命令行"，只管数据
    │   │   ├── Color.java                  调色板（enum）
    │   │   ├── BitmapFont.java            3×5 点阵字体
    │   │   ├── Canvas.java                 画布 + 绘图算法
    │   │   ├── Frame.java                  一帧
    │   │   ├── Animation.java              动画（帧列表 + 播放设置）
    │   │   └── Project.java                一个项目
    │   ├── command/                     ── 命令系统
    │   │   ├── Command.java                命令接口
    │   │   ├── Args.java                   参数切分与类型转换
    │   │   ├── CommandContext.java         执行上下文（项目 + 画笔色 + 输出）
    │   │   ├── CommandRegistry.java        命令注册表（Map<String, Command>）
    │   │   ├── SessionCommands.java        new / help / info / color / rename / quit
    │   │   ├── DrawCommands.java           fill / rect / line / circle / text / ...
    │   │   ├── FrameCommands.java          copy / add / del / fps / play / ...
    │   │   └── FileCommands.java           save / load / export
    │   ├── io/
    │   │   └── ProjectStore.java            存盘、读盘、导出 txt/ppm/bmp
    │   ├── ui/                           ── 界面
    │   │   ├── ConsoleRenderer.java         把画布渲染到终端
    │   │   └── CommandLine.java             命令循环
    │   └── exception/
    │       ├── StudioException.java         业务异常基类
    │       └── ProjectFormatException.java  文件格式异常
    └── test/java/com/pixelstudio/
        ├── CanvasTest.java                 画布的单元测试
        └── CommandLineTest.java            端到端测试
```

**建议的阅读顺序**（从简单到难）：

```
Color → Canvas → Frame → Animation → Project
                                    ↓
                     Command + Args + CommandContext
                                    ↓
                              CommandRegistry
                                    ↓
                          各 Commands 文件 → CommandLine
                                    ↓
                          ProjectStore（存读盘）
                                    ↓
                        PixelStudioApp（串起来）
```

---

## 六、Java 知识点地图

这个项目和 Java 知识点是对应着设计的。**下面每一行都指向具体文件，读完那一行就掌握了那个知识点。**

### 基础语法

| 知识点 | 在哪 | 看什么 |
|--------|------|--------|
| 二维数组 | `Canvas` | `Color[][] pixels` —— 注意 Java 是 `[高][宽]` |
| 静态方法 / 静态块 | `BitmapFont` | `static { ... }` 在类加载时自动执行一次 |
| 枚举 `enum` | `Color` | 18 个固定值 + 每种颜色自带三种表示 |
| 不可变对象 | `Canvas`, `Args` | 字段 `final` + 构造后冻结 |
| 字符串方法 | `ProjectStore` | `split(limit)`, `strip()`, `String.join()` |
| `var` 与增强 for | 全局 | 比如 `for (char c : text.toCharArray())` |

### 面向对象

| 知识点 | 在哪 | 看什么 |
|--------|------|--------|
| 封装 | `Animation` | `frames()` 返回 `List.copyOf()` 副本，外部改不了内部 |
| 组合优于继承 | `Frame`, `Project` | 类里装的是对象，不是 `extends` |
| 静态工厂方法 | `Project.newProject()`, `Animation.of()` | 把"构造合法对象"的规则收在一处 |
| 接口 + 多态 | `Command` | 20 多条命令实现同一个接口，循环代码一行不改 |
| 接口默认方法 | `Command.aliases()` | `default` 方法，实现类不想管就不用写 |
| 不可变常量类 | `Color`, `Canvas` | 私有构造器 + 全静态方法 |

### 集合与泛型

| 知识点 | 在哪 | 看什么 |
|--------|------|--------|
| `Map` 查找表 | `BitmapFont`, `CommandRegistry` | `Map<Character, String[]>`、`Map<String, Command>` |
| `LinkedHashMap` 保序 | `CommandRegistry` | `help` 的输出顺序可控 |
| `List` 与下标 | `Animation.frames()` | 越界检查与友好报错 |
| 泛型 + 菱形语法 | 全局 | `Map<String, Command>` |

### 函数式与 Stream（Java 8+）

| 知识点 | 在哪 | 看什么 |
|--------|------|--------|
| Stream + `groupingBy` | `Canvas.colorHistogram()` | 统计每种颜色多少个像素 |
| Stream + `sorted` | 同上 | 颜色按像素数从多到少排 |
| Stream + `mapToObj` | `FrameCommands.FramesCommand` | 生成帧列表文本 |
| `IntStream.range` | 同上 | 遍历帧下标 |
| Lambda 表达式 | `FrameCommands.PlayCommand` | 线程体、`AtomicBoolean` |
| `Collector.joining` | 多处 | 把流拼回一个字符串 |

### 异常处理

| 知识点 | 在哪 | 看什么 |
|--------|------|--------|
| 自定义异常 | `StudioException` | 继承 `RuntimeException`（非受检） |
| 异常继承 | `ProjectFormatException` | 精确处理 vs 粗粒度兜底 |
| 异常包装 | `Args.parseInt()`, `ProjectStore` | 把 `IOException` 包成自己的异常 |
| 分层捕获 | `CommandLine.executeOne()` | 业务错误给人话，意外错误才给堆栈 |
| try-with-resources | `ProjectStore`, `CommandLine` | 自动关流，不用写 `finally` |

### 其它

| 知识点 | 在哪 | 看什么 |
|--------|------|--------|
| `Optional` | `CommandRegistry.find()`, `Canvas` 之外多处 | "可能没有"而不是返回 `null` |
| `switch` 表达式 | `Animation`, `SessionCommands`, `ProjectStore` | Java 14+ 的写法，不再漏 `break` |
| 文本块 `"""` | `PixelStudioApp` | Java 15+ |
| 文件 I/O（文本） | `ProjectStore.save/load` | `BufferedReader` / `PrintWriter` |
| **文件 I/O（二进制）** | `ProjectStore.exportAsBmp` | **小端字节序 + 行对齐 + BGR 顺序** |
| 多线程基础 | `FrameCommands.PlayCommand` | `Thread`, `AtomicBoolean`, 守护线程, `interrupt` |
| 单元测试 | `src/test/` | JUnit 5 + `@DisplayName` + 行为测试思路 |

---

## 七、文件格式

`.pxs` 是纯文本，用记事本就能打开、能手改。程序升级也不会读不懂老文件。

```
pixelstudio 1
name=我的像素画
fps=8
loop=true
frames=2
frame 0 frame1
44 18
CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC
                    lllll
    YYY YYllY Y Yll Y
  ...（每一行是一个像素字符，空白用空格）
frame 1 frame2
44 18
...
```

**颜色是怎么编码的？** 每个颜色分配了一个独一无二的字符：

| 字符 | 颜色 | 字符 | 颜色 |
|------|------|------|------|
| ` ` | 空白 | `W` | 白 |
| `@` | 黑 | `x` | 灰 |
| `R` | 红 | `o` | 浅灰 |
| `G` | 绿 | `n` | 棕 |
| `Y` | 黄 | `p` | 粉 |
| `B` | 蓝 | `l` | 亮绿 |
| `M` | 品红 | `v` | 橄榄 |
| `C` | 青 | `y` | 藏青 |
| | | `t` | 蓝绿 |
| | | `u` | 紫 |

因为字符和颜色是一一对应的，所以存进去再读出来是**完全无损**的 —— 这是 `CanvasTest` 里有个测试专门验证的。

> **为什么不用 `ObjectOutputStream`？** 它记的是**字段名**。以后你把字段 `width` 改名成 `w`，所有老文件就全废了。文本格式没有这个问题。

---

## 八、学习路线（5 个阶段）

每个阶段都能独立跑通，**前一阶段不做完也能跳到下一阶段**。

### 阶段 1 · 先玩起来（半天）

**目标**：不写代码，先把这个工具当玩具玩熟。

- 照着[第四节](#四完整玩法演示)把幽灵和弹跳球都画一遍
- 跑一遍 `examples/` 里的三个脚本
- 跑 `mvn test`，看 24 个测试全绿

**产出**：你对"什么是有用的软件"有了直觉。

### 阶段 2 · 读懂数据模型（1 天）

**目标**：搞懂 Java 的类和对象。

按 `Color` → `BitmapFont` → `Canvas` → `Frame` → `Animation` → `Project` 的顺序读。

**重点搞懂三件事：**

1. `Canvas` 里为什么是 `pixels[y][x]` 而不是 `pixels[x][y]`
2. `Canvas.copy()` 为什么必须逐行复制，直接赋值行不行
3. `get()` 越界返回空白、`set()` 越界抛异常，为什么这样设计

**练习**：把画布改成"倒着存"，看看哪些地方会出错。

### 阶段 3 · 吃透命令系统（1~2 天）

**目标**：掌握接口、多态、泛型集合 —— 这是 Java 的核心。

读 `Command` → `Args` → `CommandContext` → `CommandRegistry` → 各 Commands。

**重点搞懂两件事：**

1. **为什么 `CommandLine` 里没有一处 `if (命令是 rect)`？** —— 这就是多态的价值
2. **`CommandContext` 为什么要单独搞一个类？** —— 想想如果不用它，`Command.execute` 的参数会是什么样子

**练习**：**新写一条命令** `pixel <x> <y>`（用一个字符把整张画布刷成那个字符）。只需要写一个类、在 `DrawCommands.all()` 里加一行。

### 阶段 4 · 文件与线程（1~2 天）

**目标**：搞懂 I/O 和多线程。

读 `ProjectStore` 和 `FrameCommands.PlayCommand`。

**重点搞懂两件事：**

1. **二进制文件的坑**：`exportAsBmp` 里的三个"为什么" —— 为什么要手动转小端？为什么要补齐行尾？为什么要倒着写行？
2. **`play` 命令为什么要起一个额外线程监视 `Ctrl+C`？** —— 不这么做会怎样？

**练习**：给 `Color` 加一个 `RGB → Color` 的反查方法，然后导出 BMP 时支持自定义配色。

### 阶段 5 · 重构与扩展（看你兴趣）

**目标**：把"能跑"变成"跑得好"。

挑一个方向做深：

- **加一个真正的 GUI**（`Swing` 或 `JavaFX`）—— 理解 MVC
- **加撤销/重做**（`Deque<Canvas>` 存历史快照）—— 理解"快照"思想
- **加图层**（每帧存多个 `Canvas` 叠在一起）—— 理解合成
- **加真正的 GIF 导出**（需要手写 LZW 压缩）—— 理解数据压缩
- **用线程池做多线程导出**（`ExecutorService`）—— 理解并发

---

## 九、动手练习

由易到难，答案在源码里都有线索。

**热身**

1. 加一条命令 `pixel <x> <y> <字符>`，用指定字符刷一个点
2. 把默认画布从 40×16 改成 64×32
3. 给 `Color` 加一个 `GOLD`（金色）

**进阶**

4. 实现 `triangle <x1> <y1> <x2> <y2> <x3> <y3>` 命令
5. 实现 `mirror` 命令：把画布左右翻转
6. 给 `save` 加一个 `saveas` 变体，存到指定路径
7. 实现 Ctrl+Z 撤销（栈里存每次修改前的画布副本）

**挑战**

8. 让 `show` 支持 `--zoom 2`，一个像素画成 2×2
9. 支持 `copy <源帧> <目标帧>`，在两帧之间复制内容
10. 写一个 `random` 命令，随机画一片"星空"
11. 给 BMP 导出加缩放（导出成 4 倍大的图片）

---

## 十、常见问题

<details>
<summary><b>Q: 中文输出乱码怎么办？</b></summary>

三个地方要检查：

1. **源码存成了 GBK** —— 用 IDEA 的话，右下角状态栏点一下改成 `UTF-8`
2. **编译时没指定编码** —— Maven 已在 `pom.xml` 里配好 `project.build.sourceEncoding=UTF-8`；手用 `javac` 要加 `-encoding UTF-8`
3. **Windows 控制台代码页** —— 先执行一次 `chcp 65001`
</details>

<details>
<summary><b>Q: 终端里看不到颜色，只有一堆奇怪的字符？</b></summary>

你的终端不支持 ANSI 颜色。用 `show --nocolor` 就会输出纯字符画。

VS Code 内置终端、Windows Terminal、PowerShell 7+ 都支持。
</details>

<details>
<summary><b>Q: <code>mvn</code> 命令找不到 / 报 JAVA_HOME 错误</b></summary>

- 检查 `java -version` 和 `mvn -v` 是不是都能跑
- 确认 `JAVA_HOME` 指向 JDK 而不是 JRE
- 实在搞不定就用[方式二](#方式二只用-jdk不想碰-maven)，完全不依赖 Maven
</details>

<details>
<summary><b>Q: 画布坐标从 0 开始还是从 1 开始？</b></summary>

**从 0 开始**，和所有图形界面一致。画布 40×16，合法范围是 `x: 0~39`、`y: 0~15`。
用 `show --ruler` 可以看到刻度。
</details>

<details>
<summary><b>Q: 坐标画到画布外面会怎样？</b></summary>

- `set` 会报错并告诉你画布有多大（这是故意的，让你知道自己画歪了）
- `fill` / `rect` / `line` / `circle` 等会自动裁剪，画出界的那部分看不见，但不会崩
</details>

<details>
<summary><b>Q: 怎么撤销？</b></summary>

**目前没有撤销功能，这正是练习 7 要你实现的东西。**
在做之前，建议先想想：存历史快照会不会很占内存？（提示：40×16 的画布，每个像素是一个枚举的引用）

</details>

<details>
<summary><b>Q: 为什么 <code>show</code> 的边框每一行结尾都有一串转义字符？</b></summary>

那是在重置背景色，让画布右边不会出现"颜色渗出去"。纯文本模式（`--nocolor`）下不会有。
</details>

<details>
<summary><b>Q: 我想换配色 / 加渐变色怎么办？</b></summary>

编辑 `Color.java` 的枚举，加几个值和对应字符即可。
注意：**已有文件里保存的字符不能改**，否则老文件会读成别的颜色。
</details>

---

## 十一、发布到 GitHub

把这个项目传到 GitHub 上，这样你就有了一个可以随时回看的学习档案。

### 1. 先在 GitHub 上建一个空仓库

打开 <https://github.com/new>，填：

- **仓库名**：`Pixel-Studio`
- 选 **Public**（公开）或 Private（私有）都行
- ⚠️ **不要勾选** "Add a README file"、"Add .gitignore"、"Choose a license"

> 勾了会和本地的 README / `.gitignore` 冲突，导致 push 被拒绝。

建完 GitHub 会给你一段命令，其中 **仓库地址**长这样：

```
https://github.com/你的用户名/Pixel-Studio.git
```

### 2. 在项目目录里执行

```bash
# ① 变成一个 git 仓库
git init

# ② 告诉 git 你是谁（只需要设一次，之后所有项目通用）
git config --global user.name "你的GitHub用户名"
git config --global user.email "你的GitHub邮箱"

# ③ 把所有文件加进来
git add .

# ④ 提交
git commit -m "Pixel Studio 初始版本：终端像素画与动画工具"

# ⑤ 把默认分支改名为 main
git branch -M main

# ⑥ 连上 GitHub（换成你自己的地址）
git remote add origin https://github.com/你的用户名/Pixel-Studio.git

# ⑦ 推上去，并把 main 和远程关联起来
git push -u origin main
```

### 常见问题

**`git commit` 报 "Please tell me who you are"**
没配身份，跑第 ② 步那两行。

**`git push` 报 "could not read Username" 或一直卡住**
GitHub 从 2021 年起不接受密码了，要用**个人访问令牌（PAT）**：

1. 打开 <https://github.com/settings/tokens>
2. `Generate new token (classic)` → 勾选 `repo` → 生成
3. 复制那串 40 位字符，粘贴到密码框（**这个 token 只显示一次，先复制**）

或者更简单：**在浏览器打开 VS Code，左下角 Accounts 图标 → 登录 GitHub**，之后所有 push 都会自动带上凭证。

**`git push` 报 "rejected: non-fast-forward"**
远程仓库里有你本地没有的提交。最常见的解法：

```bash
git pull origin main --rebase
git push -u origin main
```

**`fatal: remote origin already exists`**
`git remote add origin ...` 跑过两次了。先删掉再加：

```bash
git remote remove origin
git remote add origin https://github.com/你的用户名/Pixel-Studio.git
```

### 改完代码再传一次

以后每次更新只需要两条：

```bash
git add .
git commit -m "说明你改了什么"
git push
```

**养成每次改动后 `git log --oneline` 看一眼的习惯** —— 这就是你的学习日志。三个月后回头翻，能清楚看到自己是怎么一步步成长的。

---

## 学习建议

1. **先跑起来，再读代码。** 一定要先自己画点东西出来。
2. **改代码，别只读代码。** 照着[第九节](#九动手练习)做，做不出来再回头看。
3. **卡住了就缩小范围。** 整个项目看不懂？只看 `Canvas.drawLine` 一个方法，看懂就够了。
4. **写笔记。** 每读懂一个类，用自己的话写三行总结。半年后回来看，会很有意思。
5. **不要追求一次全懂。** 第一遍读不懂是正常的，第二遍会清楚很多，第三遍才真正舒服。

祝玩得开心 🎨
