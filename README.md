# FridaHooker
- 图形界面版本的frida，提供一个方便地管理frida的方式，告别命令行。

## 一、主要功能
- 从本地安装frida server到root后的手机
- 启动、关闭frida server

## 二、使用方法
- 打开应用，然后安装frida server后，打开开关启动frida即可。

## 三、环境配置
- 系统要求：Android 5.0或更高版本，需要应用具有root权限。

## 四、frida是什么？
- frida: Dynamic instrumentation toolkit for developers, reverse-engineers, and security researchers.  https://www.frida.re/

## 五、引用和致谢
- frida：https://github.com/frida/frida
- okhttp3：https://github.com/square/okhttp
- gson：https://github.com/google/gson
- XZ Utils：https://git.tukaani.org/xz.git

## 六、Q&A
- Q1：为什么显示安装frida server失败？
- A1：您可能没有给应用授予root权限，前往Magisk、SuperSU等root管理程序中为FridaHooker授予root权限。
- Q2：为什么打开了frida但是开关立刻就关闭了？
- A2：这是frida server崩溃了的表现，在Android 10中，运行frida server可能会有问题。如果您认为这是一个bug，欢迎提出issue。

## 七、截图
<img src="img/demo.png" />
