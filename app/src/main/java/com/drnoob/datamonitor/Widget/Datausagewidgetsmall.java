// پرانے DataUsageWidget.java کا سارا کوڈ کاپی کریں، صرف نام بدل کر DataUsageWidgetSmall
// اور onUpdate میں یہ 3 لائنیں Add کریں:
if (mobileBytes == 0) remoteViews.setViewVisibility(R.id.mobile_container_small, View.GONE);
else remoteViews.setViewVisibility(R.id.mobile_container_small, View.VISIBLE);

if (wifiBytes == 0) remoteViews.setViewVisibility(R.id.wifi_container_small, View.GONE);
else remoteViews.setViewVisibility(R.id.wifi_container_small, View.VISIBLE);

if (isOverLimit) {
  remoteViews.setInt(R.id.widget_root_small, "setBackgroundResource", R.drawable.circle_red_alert);
}
