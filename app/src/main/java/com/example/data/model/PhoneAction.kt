package com.example.data.model

sealed class PhoneAction {
    data class OpenApp(val packageName: String, val appName: String = packageName) : PhoneAction()
    data class TapByText(val text: String, val exactMatch: Boolean = false) : PhoneAction()
    data class TypeText(val text: String) : PhoneAction()
    data class Scroll(val direction: Direction) : PhoneAction()
    data class Wait(val millis: Long = 800L) : PhoneAction()
    data class PressBack(val times: Int = 1) : PhoneAction()
    object PressHome : PhoneAction()
    data class OpenUrl(val url: String) : PhoneAction()
    data class SearchGoogle(val query: String) : PhoneAction()
    data class MakeCall(val phoneNumber: String, val contactName: String = phoneNumber) : PhoneAction()
    data class SendSms(val phoneNumber: String, val message: String) : PhoneAction()
    data class Custom(val description: String) : PhoneAction()

    enum class Direction {
        UP,
        DOWN
    }
}
