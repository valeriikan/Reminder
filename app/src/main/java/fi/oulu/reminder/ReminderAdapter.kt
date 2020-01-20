package fi.oulu.reminder

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import kotlinx.android.synthetic.main.view_list_item.view.*
import java.text.SimpleDateFormat
import java.util.*

class ReminderAdapter(context: Context, private val list: List<Reminder>) : BaseAdapter() {

    private val inflater: LayoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        val row = inflater.inflate(R.layout.view_list_item, parent, false)

        // Set message text
        row.itemMessage.text = list[position].message

        // Set trigger text depending on the reminder type
        if (list[position].type == "time") {
            val sdf = SimpleDateFormat("HH:mm\ndd.MM.yyy", Locale.getDefault())
            sdf.timeZone = TimeZone.getDefault()

            val time = list[position].trigger.toLong()
            val date = sdf.format(time)
            row.itemTrigger.text = date
        }

        if (list[position].type == "location") {
            // TODO: Double check what has to be shown as a trigger
            // e.g. lat:long
            row.itemTrigger.text = list[position].trigger
        }

        return row
    }

    override fun getItem(position: Int): Any {
        return list[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return list.size
    }

}
