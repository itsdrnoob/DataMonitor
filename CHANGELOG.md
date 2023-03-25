# Changelogs

All notable changes to Data Monitor will be documented in this file.


## v2.3.0

Data Monitor v2.3.0 release <br>
This release includes many changes and improvements.

### What's new?
- Added data plan auto-update. When enabled, custom data plan will automatically update itself with a similar one.
- Users can now change the time of their custom data plan.
- Home screen summary, shows the currently active plan, it's validity and the amount of data used and remaining.
- App details view now shows the total amount of data used by the app.
- Added a new session "Current Plan" to App usage filter, which shows the data usage during the span of the currently set data plan.
- Added a toggle to always show total data usage in notification irrespective of the slected options.
- Added support for Czech, Dutch, Marathi and Vietnamese languages.

### Fixes and Improvements
- Fixed multiple connection issues and crashes during Network diagnostics.
- Fixed issues with Data usage alert not working as intended.
- Fixed the crash caused due to the denial of SCHEDULE_EXACT_ALARM permission.
- Fixed ReceiverCallNotAllowedException while registering receiver.
- Fixed visibility issues with update check dialog.
- Fixed multiple instances of NumberFormatException caused due to decimal separator.
- Fixed combined notification text visibility in dark mode.
- Fixed widget refresh button tint in light mode.
- Fixed RuntimeException caused by the unavailability of drawable resources in API 23.
- Fixed error while instantiating LiveNetworkReceiver and CompoundNotificationReceiver.
- Fixed the crash caused due to a null list reference upon device reboot.
- Widget can now display mobile data usage without setting a data plan.
- Fixed incorrectly shown data plan reset notfication.
- Fixed an issue with app data usage where the previously selected data type was incorrectly shown after a refresh.
- Fixed the constant recreation of notification view when using cobined notification.
- Removed unused services.
- Updated network speed notification to remain on top of the tray when possible, based on priority level.
- Updated custom data plan validity check.
- Updated UI and dynamic color-scheme.
- Improved data usage calculation for better accuracy.
- Improved app responsiveness.
- Updated app dependencies.
- Other minor improvements and bug fixes.


## v2.2.0

Data Monitor v2.2.0 release <br>
This release includes many changes and improvements.

- Introducing Material You! A closer experience to your device with a new & revamped User Interface.
- Users can now add applications to the Excluded apps list, which will exclude their traffic from the data plan.
- The home screen now shows the amount of data left in the data plan.
- An all new custom data plan option. Now users can choose any start/end date.
- Added system language detection.
- Result of network diagnostics will now be saved to diagnostics history. Can be disabled from settings.
- Added the option to disable haptic feedback.
- Improved widget flexibility
- Added the option to combine Data usage and Network speed notifications into a single one.
- Added a toggle to enable or disable notifications on lockscreen.
- Updated the crash reporter for better crash logs.
- A new popup for weekly overview quick look.
- Now Mobile data/Wifi usage will not be included in total data usage in the notification if disabled in Setup.
- Added 3 additional options to notification refresh interval (1 sec, 15 sec, 30 sec) when combined notification is enabled.
- Fixed the crash caused by "," as the decimal seperator.
- Added support for Hindi, Bhojpuri, Korean, Indonesian and Uzbek languages.
- Updated app and gradle dependencies.
- Reduced the app download size.


## v2.1.0

Data Monitor v2.1.0 release <br>
This release includes many changes and improvements.

- Added custom data plan
- Added diagnostics server selection
- Added crash reporter
- Added speed meter to network diagnostics
- Updated user interface
- Added Material You themed app icon
- Added option to auto hide network indicator
- Data usage notifiaton now shows the percentage of data used as icon when a data plan is active
- App data usage now shows the total data used for the selected time period
- Added in-app battery optimisation checks
- Added option to hide wifi usage in widget
- Fixed crash due to NetworkCallback
- Fixed issue with navigation when going to app data usage from home
- Fixed duplicate list in app data usage
- Fixed weekly overview data not updating on refresh
- Fixed permission denied message before granting READ_PHONE_STATE permission
- Fixed crash while updating screen time
- Fixed monitor not starting on device boot
- Improved live network speed accuracy
- Updated app dependencies
- Added Russian, Turkish, German, Norwegian Bokmål, Portuguese, Spanish and Ukrainian translations


## v2.0.0

Data Monitor v2.0.0 release <br>
This major release includes many changes and improvements.

- Added live network speed monitor
- Redesigned Data Usage widget
- Added the ability to check for updates
- Revamped the user interface
- Added the ability to select monthly data reset date
- New date and time picker
- Added haptic feedback
- Added long press to view each day's data usage in weekly overview
- Fixed delay while opening app data usage stats view
- Updated network diagnostics connection method
- Updated mothly data usage calculation. If on a monthly data plan, monthly data usage will be calculated according to the reset date.
- Fixed not able to set Simplified Chinese as app language
- Added manual request for READ_PHONE_STATE permission on Android 9 and below
- Switched from Google OSS license plugin to internal OSS license View
- Added Telegram support group and Play store listing in about section
- Fixed error while refreshing App data usage
- Added new theme picker. Now app will follow system theme by default
- Added Simplified Chinese, Traditional Chinese, French, Arabic, Malayalam and Italian translations
- Minor code improvements and fixes


## v1.6.9

Data Monitor v1.6.9 release <br>

- Added Romanian Language
- Added Data usage in notification title
- Fixed system apps data usage stats not opening when app is opened from widget
- Fixed minor UI flaws


## v1.6.1

Data Monitor v1.6.1 release <br>

- Fixed crash while starting Data Usage receiver


## v1.6.0

Data Monitor v1.6.0 release <br>

- Added app usage time
- Minimal core changes


## v1.5.1

Data Monitor v1.5.1 hotfix release

- Fixed crash when using network diagnostics without network connection
- Optimized code

# v1.5

Data Monitor v1.5 release

- Added feature Network diagnostics
- Check Data speed and latency
- Fixed distorted icon shapes
- Fixed OEM battery settings in unsupported devices
- Updated some ui elements
- Bumped up targetSdkVersion to 31
- Updated app dependencies


## v1.0

Data Monitor v1.0

- Initial release
