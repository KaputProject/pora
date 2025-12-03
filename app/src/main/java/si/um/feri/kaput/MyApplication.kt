package si.um.feri.kaput

import android.app.Application

class MyApplication: Application() {
    var data: MutableList<Int> = mutableListOf();

    override fun onCreate() {
        super.onCreate()
        data = mutableListOf(1, 2, 3, 4, 5)
    }
}