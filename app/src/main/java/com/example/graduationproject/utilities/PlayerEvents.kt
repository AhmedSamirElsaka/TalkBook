package com.example.graduationproject.utilities


sealed class PlayerEvents {

    object  PausePlay: PlayerEvents()
    object  Previous : PlayerEvents()
    object  Next : PlayerEvents()
    object  Repeat : PlayerEvents()
    object  SeekForward : PlayerEvents()
    object  SeekBack : PlayerEvents()
}
