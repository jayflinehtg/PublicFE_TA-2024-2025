package com.example.myapplication.data

sealed class EventSink {
    data object Connect : EventSink()
    data object Disconnect : EventSink()
    object GuestLogin : EventSink()
}