package com.autolog.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.autolog.app.data.dao.AutoLogDao
import com.autolog.app.data.model.*

@Database(
    entities = [
        Vehicle::class,
        FuelEntry::class,
        RepairEntry::class,
        TripEntry::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun autoLogDao(): AutoLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // ДОБАВЛЕНО: нормальная миграция с версии 1 на 2
        // Если у тебя уже была версия 1 — эта миграция сохранит данные.
        // Если БД сразу создавалась как версия 2 — Room применит её автоматически.
        // Добавляй новые миграции сюда при каждом изменении схемы.
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Пример: если в версии 2 добавили поле bluetoothDeviceName в vehicles
                // Раскомментируй нужное при следующем изменении схемы:
                // database.execSQL("ALTER TABLE vehicles ADD COLUMN bluetoothDeviceName TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "autolog_database"
                )
                    // ИСПРАВЛЕНО: убрали fallbackToDestructiveMigration.
                    // Теперь при изменении схемы нужно добавить новую Migration выше
                    // и прописать её в addMigrations().
                    // Формат: MIGRATION_X_Y где X=старая версия, Y=новая версия.
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
