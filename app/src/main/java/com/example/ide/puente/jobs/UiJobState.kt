package com.example.ide.puente.jobs

data class UiJobState(
      val running: Boolean = false,
      val step: String = "",
      val progress: Int = 0,
      val log: String = ""
  )
