package com.example.zoco.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ProductEntity::class,
        NotificationEntity::class,
        MediaEntity::class,
        OrderEntity::class,
        UserProfileEntity::class
    ],
    version = 9,
    exportSchema = false
)
abstract class ZocoDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun notificationDao(): NotificationDao
    abstract fun mediaDao(): MediaDao
    abstract fun orderDao(): OrderDao
    abstract fun userProfileDao(): UserProfileDao

    companion object {
        @Volatile
        private var INSTANCE: ZocoDatabase? = null

        /** v6 -> v7: cola de sincronización (pendingSync) y renglones del pedido (itemsJson). */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN pendingSync INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE orders ADD COLUMN pendingSync INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE orders ADD COLUMN itemsJson TEXT NOT NULL DEFAULT ''")
            }
        }

        /** v7 -> v8: propiedad del producto publicado por el usuario (isOwnedByUser). */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN isOwnedByUser INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** v8 -> v9: propietario en servidor/offline (sellerId, sellerUsername). */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN sellerId INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE products ADD COLUMN sellerUsername TEXT DEFAULT NULL")
            }
        }

        fun getInstance(context: Context): ZocoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ZocoDatabase::class.java,
                    "zoco_database.db"
                )
                    .addMigrations(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
