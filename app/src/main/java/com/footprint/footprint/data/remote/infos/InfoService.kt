package com.footprint.footprint.data.remote.infos

import android.util.Log
import com.footprint.footprint.data.model.UserModel
import com.footprint.footprint.data.remote.user.InfoDetailResponse
import com.footprint.footprint.ui.main.mypage.MyPageView
import com.footprint.footprint.ui.register.RegisterView
import com.footprint.footprint.utils.GlobalApplication.Companion.TAG
import com.footprint.footprint.utils.GlobalApplication.Companion.retrofit
import retrofit2.*

object InfoService {

    /*초기 정보 등록 API*/
    fun registerInfos(registerView: RegisterView,  userModel: UserModel) {
        val infoService = retrofit.create(InfosRetrofitInterface::class.java)

        registerView.onRegisterLoading()
        infoService.registerInfos(userModel).enqueue(object : Callback<InfosResponse>{
            override fun onResponse(call: Call<InfosResponse>, response: Response<InfosResponse>) {
                val body = response.body()
                if(body != null){
                    when(body!!.code){
                        1000 -> {
                            registerView.onRegisterSuccess(body.result)
                        }
                        else -> registerView.onRegisterFailure(body.code, body.message)
                    }
                    Log.d("REGISTER/API-SUCCESS", body.toString())
                }else{
                    Log.d("REGISTER/NULL", body.toString())
                }


            }

            override fun onFailure(call: Call<InfosResponse>, t: Throwable) {
                registerView.onRegisterFailure(213, t.message.toString())
                Log.d("REGISTER/API-FAILURE", t.message.toString())
            }
        })
    }

    /*마이페이지 정보 조회 API*/
    fun getInfoDetail(myPageView: MyPageView) {
        val infoService = retrofit.create(InfosRetrofitInterface::class.java)

        myPageView.onMyPageLoading()

        infoService.getInfoDetail().enqueue(object : Callback<InfoDetailResponse>{
            override fun onResponse(
                call: Call<InfoDetailResponse>,
                response: Response<InfoDetailResponse>
            ) {
                val response = response.body()!!

                when(response.code) {
                    1000 -> myPageView.onMyPageSuccess(response.result)
                    else -> myPageView.onMyPageFailure(response.code, response.message)
                }
            }

            override fun onFailure(call: Call<InfoDetailResponse>, t: Throwable) {
                myPageView.onMyPageFailure(400, t.message.toString())
                Log.d("$TAG/MYPAGE/API-FAILURE", t.message.toString())
            }

        })
    }
}