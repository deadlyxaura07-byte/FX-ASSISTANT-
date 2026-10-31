package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.model.ActiveScreenState
import com.example.model.ScreenElement
import com.example.model.ScreenRect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FxAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        currentService = this
        _isServiceRunning.value = true
        Log.d(TAG, "FX Assistant Accessibility Service Connected!")
        inspectCurrentWindow()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkg = event.packageName?.toString() ?: return
        // Ignore internal system noise if necessary, but keep track of app switches
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            val appTitle = event.className?.toString() ?: "App Window"
            inspectCurrentWindow(pkg, appTitle)
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "FX Assistant Accessibility Service Interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (currentService == this) {
            currentService = null
            _isServiceRunning.value = false
        }
    }

    fun inspectCurrentWindow(
        overridePackage: String? = null,
        overrideTitle: String? = null
    ) {
        val rootNode = rootInActiveWindow ?: return
        try {
            val pkg = overridePackage ?: rootNode.packageName?.toString() ?: "Unknown App"
            val elements = mutableListOf<ScreenElement>()
            extractNodes(rootNode, elements, 0)

            val summary = generateSummary(pkg, elements)
            val friendlyAppName = mapPackageToAppName(pkg)

            _screenState.value = ActiveScreenState(
                packageName = pkg,
                appName = friendlyAppName,
                activityTitle = overrideTitle ?: friendlyAppName,
                elements = elements,
                summary = summary,
                timestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error inspecting window", e)
        }
    }

    private fun extractNodes(
        node: AccessibilityNodeInfo,
        outList: MutableList<ScreenElement>,
        depth: Int
    ) {
        if (depth > 12 || outList.size >= 50) return // Cap to avoid memory churn

        val text = node.text?.toString()
        val desc = node.contentDescription?.toString()
        val isClickable = node.isClickable
        val isEditable = node.isEditable

        if (!text.isNullOrBlank() || !desc.isNullOrBlank() || isClickable || isEditable) {
            val rect = Rect()
            node.getBoundsInScreen(rect)
            if (rect.width() > 0 && rect.height() > 0) {
                outList.add(
                    ScreenElement(
                        id = node.viewIdResourceName ?: "node_${outList.size}",
                        text = text ?: desc ?: "",
                        contentDescription = desc,
                        className = node.className?.toString() ?: "View",
                        packageName = node.packageName?.toString() ?: "",
                        bounds = ScreenRect(rect.left, rect.top, rect.right, rect.bottom),
                        isClickable = isClickable,
                        isEditable = isEditable
                    )
                )
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            extractNodes(child, outList, depth + 1)
        }
    }

    private fun generateSummary(pkg: String, elements: List<ScreenElement>): String {
        val clickables = elements.filter { it.isClickable }.take(4).map { it.text.ifBlank { "Button" } }
        val texts = elements.filter { it.text.isNotBlank() }.take(5).map { it.text }
        val clickablesStr = if (clickables.isNotEmpty()) "Actions: [${clickables.joinToString(", ")}]" else ""
        val textSnippet = if (texts.isNotEmpty()) "Text: \"${texts.joinToString(" | ").take(80)}\"" else ""
        return "${mapPackageToAppName(pkg)} active. $clickablesStr $textSnippet".trim()
    }

    fun clickNodeWithText(query: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val matchingNodes = rootNode.findAccessibilityNodeInfosByText(query)
        for (node in matchingNodes) {
            if (node.isClickable) {
                val result = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (result) return true
            }
            var parent = node.parent
            while (parent != null) {
                if (parent.isClickable) {
                    val result = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    if (result) return true
                }
                parent = parent.parent
            }
        }
        return false
    }

    fun inputText(textToEnter: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val focused = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focused != null && focused.isEditable) {
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textToEnter)
            }
            return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        }

        // Fallback: search for first editable node
        val editableNodes = mutableListOf<AccessibilityNodeInfo>()
        findEditableNodes(rootNode, editableNodes)
        for (node in editableNodes) {
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textToEnter)
            }
            return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        }
        return false
    }

    private fun findEditableNodes(node: AccessibilityNodeInfo, outList: MutableList<AccessibilityNodeInfo>) {
        if (node.isEditable) {
            outList.add(node)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            findEditableNodes(child, outList)
        }
    }

    fun dispatchTap(x: Float, y: Float, onComplete: ((Boolean) -> Unit)? = null) {
        val path = Path().apply {
            moveTo(x, y)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 100)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                onComplete?.invoke(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                onComplete?.invoke(false)
            }
        }, null)
    }

    fun dispatchSwipe(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long = 300L,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs.coerceAtLeast(100L))
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                onComplete?.invoke(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                onComplete?.invoke(false)
            }
        }, null)
    }

    companion object {
        private const val TAG = "FxAccessibility"

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        private val _screenState = MutableStateFlow(ActiveScreenState())
        val screenState: StateFlow<ActiveScreenState> = _screenState.asStateFlow()

        @Volatile
        var currentService: FxAccessibilityService? = null

        fun mapPackageToAppName(pkg: String): String {
            return when {
                pkg.contains("instagram") -> "Instagram"
                pkg.contains("tiktok") -> "TikTok"
                pkg.contains("youtube") -> "YouTube"
                pkg.contains("spotify") -> "Spotify"
                pkg.contains("whatsapp") -> "WhatsApp"
                pkg.contains("snapchat") -> "Snapchat"
                pkg.contains("twitter") || pkg.contains("x.android") -> "X / Twitter"
                pkg.contains("chrome") -> "Google Chrome"
                pkg.contains("calendar") -> "Google Calendar"
                pkg.contains("clock") || pkg.contains("deskclock") -> "Clock"
                pkg.contains("camera") -> "Camera"
                pkg.contains("settings") -> "System Settings"
                pkg.contains("launcher") -> "Home Screen"
                pkg.contains("fxassistant") || pkg.contains("example") -> "FX Assistant"
                else -> pkg.substringAfterLast(".").replaceFirstChar { it.uppercase() }
            }
        }
    }
}
