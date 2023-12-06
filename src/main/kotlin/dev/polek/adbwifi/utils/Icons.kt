package dev.polek.adbwifi.utils

import com.intellij.openapi.util.IconLoader

object Icons {
    val USB = IconLoader.getIcon("/icons/usbIcon.svg", Icons::class.java)
    val NO_USB = IconLoader.getIcon("/icons/noUsbIcon.svg", Icons::class.java)
    val WIFI = IconLoader.getIcon("/icons/wifiIcon.svg", Icons::class.java)
    val NO_WIFI = IconLoader.getIcon("/icons/noWifi.svg", Icons::class.java)
    val WIFI_NETWORK = IconLoader.getIcon("/icons/wifiNetwork.svg", Icons::class.java)
    val MOBILE_NETWORK = IconLoader.getIcon("/icons/mobileNetwork.svg", Icons::class.java)
    val HOTSPOT_NETWORK = IconLoader.getIcon("/icons/hotspotNetwork.svg", Icons::class.java)
    val DEVICE_LINEUP = IconLoader.getIcon("/icons/devices-lineup.png", Icons::class.java)
    val DEVICE_WARNING = IconLoader.getIcon("/icons/deviceWarning.png", Icons::class.java)
    val MENU = IconLoader.getIcon("/icons/menuIcon.svg", Icons::class.java)
    val SHARE_SCREEN = IconLoader.getIcon("/icons/shareScreen.svg", Icons::class.java)
    val DELETE = IconLoader.getIcon("/icons/deleteIcon.svg", Icons::class.java)
    val OK = IconLoader.getIcon("AllIcons.General.InspectionsOK", Icons::class.java)
    val ERROR = IconLoader.getIcon("AllIcons.General.Error", Icons::class.java)
    val LAYOUT_BOUNDS_SHOW = IconLoader.getIcon("/icons/layout_bounds_show.svg", Icons::class.java)
    val LAYOUT_BOUNDS_HIDE = IconLoader.getIcon("/icons/layout_bounds_hide.svg", Icons::class.java)
    val GPU_OVERDRAW_SHOW = IconLoader.getIcon("/icons/gpu_overdraw_show.svg", Icons::class.java)
    val GPU_OVERDRAW_HIDE = IconLoader.getIcon("/icons/gpu_overdraw_hide.svg", Icons::class.java)
    val HWUI_RENDERING_SHOW = IconLoader.getIcon("/icons/hwui_rendering_show.svg", Icons::class.java)
    val HWUI_RENDERING_HIDE = IconLoader.getIcon("/icons/hwui_rendering_hide.svg", Icons::class.java)
}
