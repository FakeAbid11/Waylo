package com.waylo.app

import android.app.Application
import com.waylo.app.data.local.WayloDatabase

class WayloApplication : Application() {

    val wayloDatabase: WayloDatabase by lazy { WayloDatabase.getInstance(this) }
}
