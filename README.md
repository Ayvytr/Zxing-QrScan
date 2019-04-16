[![jCenter](https://img.shields.io/badge/jCenter-2.1.0-red.svg)](https://bintray.com/ayvytr/maven/qrscan/_latestVersion)[![License](https://img.shields.io/badge/license-Apche%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)



# Zxing-QrScan

QrScan Library, Support QrScan, Create QrScan, Using Zxing Library（二维码扫描库，支持二维码扫描和生成二维码，使用了Zxing库来支持二维码扫描）



## 预览

|       二维码扫描       |    自定义二维码扫描    |       生成二维码       |
| :--------------------: | :--------------------: | :--------------------: |
| ![](screenshots/1.gif) | ![](screenshots/3.gif) | ![](screenshots/5.gif) |



## ScanView属性说明

| 属性             | 值类型    | 默认值                               | 说明                     |
| :--------------- | :-------- | :----------------------------------- | :----------------------- |
| maskColor        | color     | <font color=#000000>#60000000</font> | 扫描区外遮罩的颜色       |
| frameColor       | color     | <font color=#1FB3E2>#7F1FB3E2</font> | 扫描区边框的颜色         |
| cornerColor      | color     | <font color=#1FB3E2>#FF1FB3E2</font> | 扫描区边角的颜色         |
| laserColor       | color     | <font color=#1FB3E2>#FF1FB3E2</font> | 扫描区激光线的颜色       |
| resultPointColor | color     | <font color=#EFBD21>#C0EFBD21</font> | 扫描区结果点的颜色       |
| text             | string    |                                      | 扫描提示文本信息         |
| textColor        | color     | <font color=#C0C0C0>#FFC0C0C0</font> | 提示文本字体颜色         |
| textSize         | dimension | 14sp                                 | 提示文本字体大小         |
| textPadding      | dimension | 24dp                                 | 提示文本距离扫描区的间距 |
| textLocation     | enum      | top                                  | 提示文本信息显示的位置   |



## 导入

**`compile 'com.ayvytr:qrscan:2.1.0'`**

## Change Log

2.2.0
解决了解析**QrUtils.decodeBitmap**二维码图片解析不出的问题

2.1.0
解决了*CameraView*异常问题

2.0.0
1. 优化了**CameraView**的生命周期
2. 优化删除了多余的Handler Callback


## 使用

### 二维码扫描

使用CaptureFragment或者CaptureActivity即可，在onActivityResult接收返回结果，RESULT_OK时，将成功返回二维码，通过`QrUtils.RESULT`获取二维码字符串。

### 解析二维码

QrUtils.decodeBitmap

### 生成二维码Bitmap

QrUtils.createBitmap



