package com.footprint.footprint.data.remote.walk

import android.util.Log
import com.footprint.footprint.ui.main.calendar.CalendarView
import com.footprint.footprint.utils.GlobalApplication.Companion.TAG
import com.footprint.footprint.utils.GlobalApplication.Companion.retrofit
import retrofit2.*

object WalkService {
    /* calendarFragment */
    fun getMonthWalks(calendarView: CalendarView, year: Int, month: Int) {
        val walkService = retrofit.create(WalkRetrofitInterface::class.java)

        calendarView.onCalendarLoading()

        walkService.getMonthWalks(year, month).enqueue(object : Callback<MonthResponse> {
            override fun onResponse(call: Call<MonthResponse>, response: Response<MonthResponse>) {
                val response = response.body()!!

                when (response.code) {
                    1000 -> calendarView.onMonthSuccess(response.result)
                    else -> calendarView.onCalendarFailure(response.code, response.message)
                }
            }

            override fun onFailure(call: Call<MonthResponse>, t: Throwable) {
                calendarView.onCalendarFailure(400, t.message.toString())
            }

        })
    }

}