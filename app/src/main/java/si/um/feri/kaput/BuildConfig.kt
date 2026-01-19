package si.um.feri.kaput

annotation class BuildConfig {
    companion object {
        //user name and password for logging in to the server
        const val USER_NAME: String = "LukaKuder3"
        const val PASSWORD: String = "LukaKuder3"
        //server URL for local testing with emulator
        const val FAMILY_URL = "http://10.0.2.2:5000/family"
        const val USER_URL = "http://10.0.2.2:5000/users"
    }
}
