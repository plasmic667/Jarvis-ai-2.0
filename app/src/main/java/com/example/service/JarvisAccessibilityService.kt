package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.data.model.PhoneAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object JarvisAccessibilityServiceHolder {
    @Volatile
    var service: JarvisAccessibilityService? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    fun updateConnected(connected: Boolean) {
        _isConnected.value = connected
    }
}

class JarvisAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        JarvisAccessibilityServiceHolder.service = this
        JarvisAccessibilityServiceHolder.updateConnected(true)
        Log.i(TAG, "Jarvis Accessibility Service connected successfully.")
    }

    override fun onInterrupt() {
        Log.w(TAG, "Jarvis Accessibility Service interrupted.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Can be monitored for window state changes or UI updates
    }

    override fun onDestroy() {
        super.onDestroy()
        JarvisAccessibilityServiceHolder.service = null
        JarvisAccessibilityServiceHolder.updateConnected(false)
        Log.i(TAG, "Jarvis Accessibility Service destroyed.")
    }

    fun performPhoneAction(action: PhoneAction): Boolean {
        return try {
            when (action) {
                is PhoneAction.OpenApp -> openApp(action.packageName)
                is PhoneAction.TapByText -> tapByText(action.text, action.exactMatch)
                is PhoneAction.TypeText -> typeText(action.text)
                is PhoneAction.Scroll -> scroll(action.direction)
                is PhoneAction.Wait -> {
                    Thread.sleep(action.millis.coerceIn(100L, 5000L))
                    true
                }
                is PhoneAction.PressBack -> pressBack(action.times)
                is PhoneAction.PressHome -> pressHome()
                is PhoneAction.OpenUrl -> openUrl(action.url)
                is PhoneAction.SearchGoogle -> searchGoogle(action.query)
                is PhoneAction.MakeCall -> makeCall(action.phoneNumber)
                is PhoneAction.SendSms -> sendSms(action.phoneNumber, action.message)
                is PhoneAction.Custom -> true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing phone action: $action", e)
            false
        }
    }

    private fun openApp(packageName: String): Boolean {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            startActivity(launchIntent)
            Thread.sleep(1200L)
            return true
        }

        return try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setPackage(packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            Thread.sleep(1000L)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Could not open app: $packageName", e)
            false
        }
    }

    private fun tapByText(text: String, exactMatch: Boolean): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = mutableListOf<AccessibilityNodeInfo>()
        collectNodes(root, nodes)

        val target = nodes.firstOrNull { node ->
            val nodeText = node.text?.toString() ?: node.contentDescription?.toString() ?: ""
            if (exactMatch) {
                nodeText.equals(text, ignoreCase = true)
            } else {
                nodeText.contains(text, ignoreCase = true)
            }
        } ?: return false

        var clickableNode: AccessibilityNodeInfo? = target
        while (clickableNode != null && !clickableNode.isClickable) {
            clickableNode = clickableNode.parent
        }

        val nodeToClick = clickableNode ?: target
        val clicked = nodeToClick.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        if (!clicked) {
            target.parent?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
        Thread.sleep(800L)
        return true
    }

    private fun typeText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = mutableListOf<AccessibilityNodeInfo>()
        collectNodes(root, nodes)

        val target = nodes.firstOrNull { it.isFocused && it.isEditable }
            ?: nodes.firstOrNull { it.isEditable }
            ?: return false

        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        val typed = target.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        Thread.sleep(500L)
        return typed
    }

    private fun scroll(direction: PhoneAction.Direction): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = mutableListOf<AccessibilityNodeInfo>()
        collectNodes(root, nodes)

        val scrollable = nodes.firstOrNull { it.isScrollable } ?: return false
        val action = if (direction == PhoneAction.Direction.UP) {
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        } else {
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        }
        val scrolled = scrollable.performAction(action)
        Thread.sleep(600L)
        return scrolled
    }

    private fun pressBack(times: Int): Boolean {
        repeat(times.coerceIn(1, 5)) {
            performGlobalAction(GLOBAL_ACTION_BACK)
            Thread.sleep(300L)
        }
        return true
    }

    private fun pressHome(): Boolean {
        val result = performGlobalAction(GLOBAL_ACTION_HOME)
        Thread.sleep(600L)
        return result
    }

    private fun openUrl(url: String): Boolean {
        return try {
            val validUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "https://$url"
            } else url
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(validUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            Thread.sleep(1000L)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open url: $url", e)
            false
        }
    }

    private fun searchGoogle(query: String): Boolean {
        val searchUrl = "https://www.google.com/search?q=" + Uri.encode(query)
        return openUrl(searchUrl)
    }

    private fun makeCall(phoneNumber: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun sendSms(phoneNumber: String, message: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:$phoneNumber")).apply {
                putExtra("sms_body", message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun collectNodes(node: AccessibilityNodeInfo, list: MutableList<AccessibilityNodeInfo>) {
        list.add(node)
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                collectNodes(child, list)
            }
        }
    }

    companion object {
        private const val TAG = "JarvisAccessService"

        fun isAccessibilityServiceEnabled(context: Context): Boolean {
            val expectedServiceName = "${context.packageName}/${JarvisAccessibilityService::class.java.canonicalName}"
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServices)

            while (colonSplitter.hasNext()) {
                val componentName = colonSplitter.next()
                if (componentName.equals(expectedServiceName, ignoreCase = true)) {
                    return true
                }
            }
            return false
        }

        fun openAccessibilitySettings(context: Context) {
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open accessibility settings", e)
            }
        }

        fun openOverlaySettings(context: Context) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open overlay settings", e)
            }
        }
    }
}
