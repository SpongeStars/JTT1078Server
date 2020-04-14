<p align="center"><a href="https://github.com/SuperChrisliu" target="_blank" rel="noopener noreferrer"><img width="500" src="http://cdn.chrisliu.top/1078logo.png" alt="1078 logo"></a></p>

# Tsing JT/T 1078 Tube

### 简介
JT/T 1078实时视频协议的实现。

### features
* 视频/音频流零拷贝
* websocket推流
* FLV编码H.264流（flv封装音频暂未实现）
* 监听、对讲（服务端不做音频解码编码，只做转发，编码解码由前端完成）
* 支持注册到EurekaServer

### Quick Start
示例配置
```
spring:
  main.web-application-type: none
  application:
    name: service-1078 # 应用名称，使用eurekaserver时会用到

tsing-jtt1078:
  server:
    port: 1077  # 1078协议服务使用的端口
    host: 0.0.0.0
    app: /tsinglive/ 
    livePort: 1079 # websocket服务使用的端口
```
启动服务，使用websocket客户端连接`ws://127.0.0.1:1079/tsinglive/设备号/通道号?type=1`
参数`type=1`表示实时视频，`type=2`表示音频.

`wss`建议配合springcloud gateway实现.

### Ecosystem
* 基于flvjs实现的直播播放器（开发中）
* [前端实现的对讲客户端，支持711a/u、adpcm](https://github.com/SuperChrisliu/voiceintercom)


### Contribution
非常感谢以下网友的帮助和支持，以及其他默默支持的朋友们！
* [@glaciall](https://github.com/glaciall)
* 不岸不名
* 故事~
* yedajiang44.com
* 慢慢

### 交流讨论
QQ群：808432702，加入我们，群里有热心的同道中人、相关资料、测试数据、代码以及各种方案的先行者等着你。

### 打赏
如果本项目为您节约更多时间，去陪恋人、家人和朋友，打赏支持一下吧:)
<a href="http://cdn.chrisliu.top/receivemoney.png" target="_blank" rel="noopener noreferrer"><img src="./receivemoney.png" alt="receivemoney"></a>

## License
[MIT](http://opensource.org/licenses/MIT)

Copyright (c) 2020-present, Suzhou Tsingtech Microvision Electronic Technology Co.,Ltd, Chris liu
