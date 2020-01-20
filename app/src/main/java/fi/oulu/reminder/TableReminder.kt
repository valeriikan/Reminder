package fi.oulu.reminder

import androidx.room.*

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) var uid: Int?,
    @ColumnInfo(name = "type") var type: String,
    @ColumnInfo(name = "trigger") var trigger: String,
    @ColumnInfo(name = "message") var message: String
)

@Dao
interface ReminderDao {
    @Transaction @Insert
    fun insert(reminder: Reminder): Long

    @Query("DELETE FROM reminders WHERE uid = :id")
    fun delete(id: Int)

    @Query("SELECT * FROM reminders")
    fun getReminders(): List<Reminder>
}
