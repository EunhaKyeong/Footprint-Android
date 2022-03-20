package com.footprint.footprint.ui.signin

import android.content.Intent
import android.os.Handler
import com.footprint.footprint.data.remote.auth.AuthService
import com.footprint.footprint.data.remote.auth.Login
import com.footprint.footprint.data.remote.badge.BadgeInfo
import com.footprint.footprint.data.remote.badge.BadgeService
import com.footprint.footprint.databinding.ActivitySplashBinding
import com.footprint.footprint.ui.BaseActivity
import com.footprint.footprint.ui.main.MainActivity
import com.footprint.footprint.ui.onboarding.OnBoardingActivity
import com.footprint.footprint.utils.LogUtils
import com.footprint.footprint.utils.getJwt
import com.footprint.footprint.utils.getOnboarding
import com.footprint.footprint.utils.removeJwt
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.gson.Gson


class SplashActivity : BaseActivity<ActivitySplashBinding>(ActivitySplashBinding::inflate),
    SplashView, MonthBadgeView {
    private val APP_UPDATE_REQUEST_CODE = 100

    override fun initAfterBinding() {
        checkAppUpdate()

        //온보딩 화면 O/X => 1.5
        val handler = Handler()
        handler.postDelayed({
            //1. 온보딩 실행 여부 spf에서 받아오기
            if (!getOnboarding()) {
                //2. false -> 온보딩 실행해야 함 -> OnboardingActivity
                startNextActivity(OnBoardingActivity::class.java)
                finish()
            } else {
                autoLogin()
            }
        }, 1500)
    }

    private fun checkAppUpdate() {
        val appUpdateManager = AppUpdateManagerFactory.create(this)

        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                // Request the update.
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    AppUpdateType.IMMEDIATE,
                    this,
                    APP_UPDATE_REQUEST_CODE
                )
            } else {
                LogUtils.d("Splash/Update", "업데이트 없음")

            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == APP_UPDATE_REQUEST_CODE) {
            if (resultCode != RESULT_OK) {
                Snackbar.make(binding.root, "업데이트가 실패했습니다.", Snackbar.LENGTH_LONG)
                finishAffinity()
            }
        }
    }


    private fun autoLogin() {
        if (getJwt() != null) { // O -> 자동로그인 API 호출
            AuthService.autoLogin(this)
        } else {  // X -> 로그인 액티비티
            startNextActivity(SigninActivity::class.java)
            finish()
        }
    }


    /*자동 로그인 API*/
    override fun onAutoLoginSuccess(result: Login?) {
        if (result != null) {
            when (result.status) {
                "ACTIVE" -> {   // 가입된 회원
                    if (result.checkMonthChanged) { // 첫 접속 -> 뱃지 API 호출
                        BadgeService.getMonthBadge(this)
                    } else { // -> 메인 액티비티
                        startMainActivity()
                    }
                }
                "ONGOING" -> { // 가입이 완료되지 않은 회원 -> 로그인 액티비티
                    startSignInActivity()
                }
            }
        }

        LogUtils.d("SPLASH/API-SUCCESS", "status: ${result!!.status}")
    }

    override fun onAutoLoginFailure(code: Int, message: String) {
        LogUtils.d("SPLASH/API-FAILURE", "code: $code message: $message")
        when (code) {
            2001, 2002, 2003, 2004 -> { // JWT 관련 오류 -> 로그인 액티비티,
                removeJwt()
                startSignInActivity()
            }
        }
    }

    /*뱃지 API*/
    override fun onMonthBadgeSuccess(isBadgeExist: Boolean, monthBadge: BadgeInfo?) {
        val intent = Intent(this, MainActivity::class.java)
        if (isBadgeExist)
            intent.putExtra("badge", Gson().toJson(monthBadge))
        startActivity(intent)
        LogUtils.d("SPLASH(BADGE)/API-SUCCESS", monthBadge.toString())
    }

    override fun onMonthBadgeFailure(code: Int, message: String) {
        LogUtils.d("SPLASH(BADGE)/API-FAILURE", code.toString() + message)
    }

    /*액티비티 이동*/
    //Main Activity
    private fun startMainActivity() {
        startNextActivity(MainActivity::class.java)
        finish()
    }

    //SignIn Activity
    private fun startSignInActivity() {
        startNextActivity(SigninActivity::class.java)
        finish()
    }

}