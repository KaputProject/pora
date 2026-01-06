package si.um.feri.kaput

annotation class BuildConfig {
    companion object {
        const val USER_NAME: String = "LukaKuder"
        const val PASSWORD: String = "luka"
        const val LOG_IN_URL: String = "http://10.0.2.2:5000/users/login"
        const val FAMILY_URL = "http://10.0.2.2:5000/family"
        const val USER_URL = "http://10.0.2.2:5000/users"
    }
}
