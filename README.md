[![zhtw](https://img.shields.io/badge/lang-zhtw-green.svg)](https://github.com/rickwangtw/LineNotificationSupport/blob/main/README.zh-tw.md)

# LINE Notification Support

Do you use LINE app? Do you have a Samsung smart watch? This could be an app for you!

Do you complain about your Samsung smart watch not showing the LINE messages the way you want?

* Only the last messages are being shown?
* You're not being notified when there is a LINE call?

This is the app for you!

## Features

* Shows the message history (even if you don't have a watch, you can read the messages without the them being marked as read!). Please note that you won't have access to the message history after responding or dismissing the messages. This feature is to address LINE *only* showing the latest message.
* Grouping of messages. Chats from groups and those from individuals will be separated out. You don't have to filter those messages in your brain!
* You'll get messages when you get an incoming call. Worried that you may not get it? It will send messages continuously until you accept or reject.
* You can configure each chat the way you want! You can choose to ignore notifications that come from some to focus on the critical messages!
* LINE stickers will also be shown on your watch!
* You **CAN** reply to your friends on your watch!
* Disable Bluetooth during ongoing calls - workaround to address a low volume bug when connected to Samsung watches.

Supports all LINE languages (the app still has limited language support though - translation contributions are always welcomed).

## Permissions

* Internet: this is to download LINE stickers
* Notification access: this is to read notifications from LINE so that it can resend the notifications in the right format

## Testing

This app has been tested with Samsung Galaxy Watch Tizen 5.5.0.1 with Android 11.

LINE version tested:

* 11.22.2
* 11.17.1
* 11.16.0
* 11.15.3, 11.15.2, 11.15.0
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
* 10.19.1 (This LINE version does not provide the title of the chat group. Instead of showing the title of the chat group, this app will list out the users who previously responded as a mitigation. If not showing the title is a problem, please downgrade your LINE version.)
* 10.18.2, 10.18.1

## Google Play
* [Free version](https://play.google.com/store/apps/details?id=com.mysticwind.linenotificationsupport)
* [Donate version](https://play.google.com/store/apps/details?id=com.mysticwind.linenotificationsupport.donate)

## Credits
* Thanks kanji1113 for providing JP translation

## FAQ

### <a name="recommended-settings"></a> 1. What are the recommended settings for watches?
You use a watch that is not on the list? Share your settings!

#### Galaxy Watch / Tizen 5.5.0.1
* [BETA] Manage LINE notificaions: true
* Use a single notification for each conversation: true

### <a name="tasker"></a> 2. Does the app support Tasker integration?
You can integrate with Tasker and update settings since version 1.11.

#### Change setting to enable/disable Bluetooth during LINE calls
* Available since 1.11
* Tasker settings to enable Bluetooth control
  * Action: com.mysticwind.linenotificationsupport.action.settings.update
  * Extra: setting-key:bluetooth\_control\_in\_calls
  * Extra: setting-value:true
  * Package: com.mysticwind.linenotificationsupport (if you are using the donate version - com.mysticwind.linenotificationsupport.donate)
  * Target: Broadcast Receiver
* Tasker settings to disable Bluetooth control
  * Action: com.mysticwind.linenotificationsupport.action.settings.update
  * Extra: setting-key:bluetooth\_control\_in\_calls
  * Extra: setting-value:false
  * Package: com.mysticwind.linenotificationsupport (if you are using the donate version - com.mysticwind.linenotificationsupport.donate)
  * Target: Broadcast Receiver

* Sample Command to enable this setting through am command
```
adb shell am broadcast -a com.mysticwind.linenotificationsupport.action.settings.update -n com.mysticwind.linenotificationsupport/.SettingsUpdateRequestBroadcastReceiver -e setting-key bluetooth_control_in_calls -e setting-value true
```
