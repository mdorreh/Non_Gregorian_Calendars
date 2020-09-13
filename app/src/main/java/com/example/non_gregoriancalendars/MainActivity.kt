package com.example.non_gregoriancalendars

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.ContentValues
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_select_calendar.view.*
import kotlinx.android.synthetic.main.radio_button.view.*
import org.joda.time.DateTime
import org.joda.time.DateTimeZone

class MainActivity : AppCompatActivity() {

    private lateinit var eventStartDateTime: DateTime
    private lateinit var eventEndDateTime: DateTime
    private lateinit var calendars: ArrayList<Calendar>
    private lateinit var dialog: AlertDialog
    private var curCalendarId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CALENDAR), 100)
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_CALENDAR), 100)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CALENDAR
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fetchCalendars()
        } else {
            calendars = ArrayList()
        }
        eventStartDateTime = DateTime(System.currentTimeMillis())
        eventEndDateTime = eventStartDateTime.plusHours(1)

        text_view_event_start_date.text = eventStartDateTime.toString()
        text_view_event_end_date.text = eventEndDateTime.toString()
        text_view_event_start_date.setOnClickListener { setupStartDate() }
        text_view_event_end_date.setOnClickListener { setupEndDate() }

        text_view_calendar_type.setOnClickListener {
            selectCalendarDialog(curCalendarId) { calendarId ->
                text_view_calendar_type.text = calendars.first { it.id == calendarId }.displayName
                curCalendarId = calendarId
            }
        }
        create_event.setOnClickListener {
            insertEvent()
        }
    }

    private fun insertEvent() {
        val uri = CalendarContract.Events.CONTENT_URI
        val rrule = edit_text_event_rrule.text
        val rscale = edit_text_rscale.text
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, curCalendarId)
            put(CalendarContract.Events.TITLE, edit_text_event_title.text.toString())
            put(
                CalendarContract.Events.DTSTART,
                eventStartDateTime.withSecondOfMinute(0).withMillisOfSecond(0).millis
            )
            put(
                CalendarContract.Events.DTEND,
                eventEndDateTime.withSecondOfMinute(0).withMillisOfSecond(0).millis
            )
            put(CalendarContract.Events.EVENT_TIMEZONE, DateTimeZone.getDefault().toString())

            if (rrule.isEmpty()) {
                putNull(CalendarContract.Events.RRULE)
            } else {
                if (rscale.isEmpty())
                    put(CalendarContract.Events.RRULE, rrule.toString())
                else
                    put(CalendarContract.Events.RRULE, "$rscale;$rrule")
            }

        }
        contentResolver.insert(uri, values) ?: return
    }

    @SuppressLint("InflateParams")
    private fun selectCalendarDialog(currCalendarId: Int, callback: (id: Int) -> Unit) {
        val view = layoutInflater.inflate(R.layout.dialog_select_calendar, null) as ViewGroup
        val radioGroup: RadioGroup = view.dialog_radio_group
        calendars.forEach {
            val radioButton = layoutInflater.inflate(R.layout.radio_button, null)
            (radioButton.dialog_radio_button as RadioButton).apply {
                text = it.getFullTitle()
                isChecked = it.id == currCalendarId
                id = it.id
            }
            radioButton.setOnClickListener {
                callback(it.id)
                dialog.dismiss()
            }
            radioGroup.addView(
                radioButton,
                RadioGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
        dialog = AlertDialog.Builder(this)
            .create().apply {
                setView(view)
                show()
            }
    }

    private fun setupEndDate() {
        val datePickerDialog = DatePickerDialog(
            this,
            0,
            endDateSetListener,
            eventEndDateTime.year,
            eventEndDateTime.monthOfYear - 1,
            eventEndDateTime.dayOfMonth
        )

        datePickerDialog.show()
    }

    private fun setupStartDate() {
        val datePickerDialog = DatePickerDialog(
            this,
            0,
            startDateSetListener,
            eventStartDateTime.year,
            eventStartDateTime.monthOfYear - 1,
            eventStartDateTime.dayOfMonth
        )

        datePickerDialog.show()
    }

    private val startDateSetListener =
        DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            dateSet(year, monthOfYear, dayOfMonth, true)
        }

    private val endDateSetListener =
        DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            dateSet(year, monthOfYear, dayOfMonth, false)
        }

    private fun dateSet(year: Int, monthOfYear: Int, dayOfMonth: Int, startDate: Boolean) {

        if (startDate) {
            eventStartDateTime = eventStartDateTime.withDate(year, monthOfYear + 1, dayOfMonth)
            eventEndDateTime = eventStartDateTime.plusHours(1)
            updateStartText()
            updateEndText()
        } else {
            eventEndDateTime = eventEndDateTime.withDate(year, monthOfYear, dayOfMonth)
            updateEndText()
        }

    }

    private fun updateEndText() {
        text_view_event_end_date.text = eventEndDateTime.toString()
    }

    private fun updateStartText() {
        text_view_event_start_date.text =
            eventStartDateTime.toString()
    }

    private fun fetchCalendars() {
        calendars = ArrayList()
        val uri = CalendarContract.Calendars.CONTENT_URI
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        )

        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(uri, projection, null, null, null)
            if (cursor?.moveToFirst() == true) {
                do {
                    val id = cursor.getInt(cursor.getColumnIndex(CalendarContract.Calendars._ID))
                    val displayName =
                        cursor.getString(cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME))
                    val accountName =
                        cursor.getString(cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME))
                    val accountType =
                        cursor.getString(cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_TYPE))
                    val accessLevel =
                        cursor.getString(cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL))
                    val calendar =
                        Calendar(id, displayName, accountName, accountType, accessLevel.toInt())
                    if (calendar.canWrite())
                        calendars.add(calendar)
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {

        } finally {
            cursor?.close()
        }
        Log.i("calendarstoshow", calendars.toString())
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        fetchCalendars()
    }
}