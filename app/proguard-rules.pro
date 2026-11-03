# Room builds generated databases (e.g. WorkManager's WorkDatabase_Impl, used by Glance)
# via reflection on the no-arg constructor. R8 strips it otherwise and release crashes
# at startup in androidx.startup.InitializationProvider.
-keep class * extends androidx.room.RoomDatabase {
    <init>();
}
