[![English](https://img.shields.io/badge/lang-en-green.svg)](https://github.com/rickwangtw/LineNotificationSupport/blob/main/README.md)

# LINE通知小幫手

您使用 LINE 嗎？您使用三星智慧型手錶嗎？這可能是個適合您的 App 唷！


您常抱怨您的三星智慧型手錶總是沒辦法正常顯示來自LINE的訊息嗎？
* 只看得到最後一則訊息嗎？
* 來電時手錶不會通知您嗎？

這是一個能幫助您的 App！

## 功能

* 顯示歷史訊息 （就算您沒有智慧型手錶，您也可以偷偷的利用它來閱讀訊息，而訊息不會變成已讀）。小提醒：訊息回覆或者清除之後就看不到歷史訊息了喔！這功能主要是解決 LINE 只顯示最後一則訊息的問題。
* 訊息分類。不同群組或者個人的訊息會被分類。您不用再被訊息混淆了！
* 有來電的時候您將會收到訊息。您也不必擔心漏掉訊息，它會一直重複傳送訊息直到您接到電話。
* 您可選擇忽略來自特定的群組或個人的訊息。幫助您只專注於重要的訊息！
* LINE 貼圖也會被顯示在您的手錶！
* 您依然 **可以** 回覆您的朋友！

支援所有的LINE語言，然而 App 本身支援非常少的語言，歡迎提供翻譯！

## 使用權限

* 網路：下載 LINE 貼圖
* 通知：用來擷取 LINE 的通知

## 測試

這個 App 在 Samsung Galaxy Watch Tizen 5.5.0.1以及 Android 11 上測試。

測試過的 LINE 版本：

* 11.14.3
* 11.8.3, 11.8.1, 11.8.0
* 11.7.2, 11.7.1, 11.7.0
* 11.6.5
* 11.3.1
* 11.2.1, 11.2.0
* 11.1.1, 11.0.2
* 10.21.5, 10.21.4, 10.21.3
* 10.20.1, 10.20.0
* 10.19.3, 10.19.2
* 10.19.1 （這個 LINE 版本在訊息中不顯示群組名稱。應對這個問題，這個 app 會顯示之前在群組中傳過訊息的朋友列表。如果不顯示群組名稱對您造成困擾，請降版本至 10.18.2 或之前。）
* 10.18.2, 10.18.1

## Google Play
* [免費版](https://play.google.com/store/apps/details?id=com.mysticwind.linenotificationsupport)
* [捐贈版](https://play.google.com/store/apps/details?id=com.mysticwind.linenotificationsupport.donate)

## 常見問題

### <a name="recommended-settings"></a> 1. 請問有推薦的設定嗎？
您的手錶或者手環不在名單中？歡迎分享您的設定！

#### Galaxy Watch / Tizen 5.5.0.1
* [測試中]管理 LINE 通知：啟用
* 一個群組使用一個通知：啟用

### <a name="tasker"></a> 2. 請問支援 Tasker 嗎？
自 1.11 版，您可以利用 Tasker 來改變設定。

#### 改變 LINE 通話時的藍芽設定
* 支援 1.11 以上版本
* 啟動藍牙控制的 Tasker 設定
  * Action: com.mysticwind.linenotificationsupport.action.settings.update
  * Extra: setting-key:bluetooth\_control\_in\_calls
  * Extra: setting-value:true
  * Package: com.mysticwind.linenotificationsupport (若您使用捐贈版 - com.mysticwind.linenotificationsupport.donate)
  * Target: Broadcast Receiver
* 關閉藍芽控制的 Tasker 設定
  * Action: com.mysticwind.linenotificationsupport.action.settings.update
  * Extra: setting-key:bluetooth\_control\_in\_calls
  * Extra: setting-value:false
  * Package: com.mysticwind.linenotificationsupport (若您使用捐贈版 - com.mysticwind.linenotificationsupport.donate)
  * Target: Broadcast Receiver

* 您也可以利用 am 來改變設定
```
adb shell am broadcast -a com.mysticwind.linenotificationsupport.action.settings.update -n com.mysticwind.linenotificationsupport/.SettingsUpdateRequestBroadcastReceiver -e setting-key bluetooth_control_in_calls -e setting-value true
```
