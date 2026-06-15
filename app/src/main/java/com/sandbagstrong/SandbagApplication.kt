package com.sandbagstrong

import android.app.Application
import com.sandbagstrong.data.WorkoutRepository

class SandbagApplication : Application() {
    // Simple service locator for the repo (keeps things minimal, no DI framework)
    val repository: WorkoutRepository by lazy {
        WorkoutRepository(applicationContext)
    }
}
