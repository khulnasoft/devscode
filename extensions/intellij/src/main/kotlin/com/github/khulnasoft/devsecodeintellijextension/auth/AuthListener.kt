package com.github.khulnasoft.devscodeintellijextension.auth

import com.intellij.util.messages.Topic

interface AuthListener {
    fun startAuthFlow()

    fun handleUpdatedSessionInfo(sessionInfo: ControlPlaneSessionInfo?)

    companion object {
        val TOPIC = Topic.create("StartAuthFlow", AuthListener::class.java)
    }
}