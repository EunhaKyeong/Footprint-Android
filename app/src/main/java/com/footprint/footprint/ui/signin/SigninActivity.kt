package com.footprint.footprint.ui.signin

import android.content.Intent
import android.os.Bundle
import com.footprint.footprint.databinding.ActivitySigninBinding
import com.footprint.footprint.ui.BaseActivity
import com.footprint.footprint.ui.main.MainActivity
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.footprint.footprint.R
import com.footprint.footprint.data.remote.auth.AuthService
import com.footprint.footprint.data.remote.auth.Login
import com.footprint.footprint.data.model.SocialUserModel
import com.footprint.footprint.data.remote.badge.BadgeInfo
import com.footprint.footprint.data.remote.badge.BadgeService
import com.footprint.footprint.ui.agree.AgreeActivity
import com.footprint.footprint.utils.*
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson


class SigninActivity : BaseActivity<ActivitySigninBinding>(ActivitySigninBinding::inflate),
    SignInView, MonthBadgeView{

    lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var getResult: ActivityResultLauncher<Intent>
    private lateinit var socialUserModel: SocialUserModel

    override fun initAfterBinding() {
        setClickListener()
    }

    private fun setClickListener() {
        //카카오 로그인
        binding.signinKakaologinBtnLayout.setOnClickListener {
            setKakaoLogin()
        }

        //구글 로그인
        binding.signinGoogleloginBtnLayout.setOnClickListener {
            getResult.launch(mGoogleSignInClient.signInIntent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //구글 로그인 API Result 처리 부분
        googleClient()
    }

    /*Funtion-Kakao*/
    private fun setKakaoLogin() {
        //카카오 계정으로 로그인
        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if (error != null) {
                signinErrorCheck("KAKAO")
            } else if (token != null) {
                getKakaoUser()
            }
        }

        //카카오톡으로 로그인
        if (UserApiClient.instance.isKakaoTalkLoginAvailable(this@SigninActivity)) {
            UserApiClient.instance.loginWithKakaoTalk(this@SigninActivity) { token, error ->
                if (error != null) {
                    // 사용자가 카카오톡 설치 후 디바이스 권한 요청 화면에서 로그인을 취소한 경우,
                    if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                        return@loginWithKakaoTalk
                    }

                    // 카카오톡에 연결된 카카오계정이 없는 경우, 카카오계정으로 로그인 시도
                    UserApiClient.instance.loginWithKakaoAccount(
                        this@SigninActivity,
                        callback = callback
                    )
                } else if (token != null) {
                    getKakaoUser()
                }
            }
        } else {
            UserApiClient.instance.loginWithKakaoAccount(this@SigninActivity, callback = callback)
        }
    }
    private fun getKakaoUser() {
        UserApiClient.instance.me { user, error ->
            if (error==null && user != null) {
                val userId: String = user.id.toString()
                val nickname: String = user.kakaoAccount?.profile?.nickname!!
                val email: String? = user.kakaoAccount?.email

                //1. User 정보 등록
                socialUserModel = SocialUserModel(userId, nickname, email!!, "kakao")

                //2. 로그인 API
                callSignInAPI()
            }

        }
    }

    /*Function - Google*/
    private fun googleClient(){
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.google_login_server_id))
            .requestEmail()
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        getResult = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                //구글 로그인 성공
                val task: Task<GoogleSignInAccount> =
                    GoogleSignIn.getSignedInAccountFromIntent(result.data)
                getGoogleUser(task)
            }else
                signinErrorCheck("GOOGLE")
        }
    }
    private fun getGoogleUser(completedTask: Task<GoogleSignInAccount>) {
        try {

            //1. user 정보 등록
            val account = completedTask.getResult(ApiException::class.java)
            val username = account?.displayName.toString()
            val useremail = account?.email.toString()
            val userid = account.id.toString()

            socialUserModel = SocialUserModel(userid, username, useremail, "google")

            //2. 로그인 API
            callSignInAPI()

        } catch (e: ApiException) {
        }
    }

    /*로그인 API*/
    private fun callSignInAPI(){
        AuthService.login(this, socialUserModel)
    }

    //-> Response
    override fun onSignInSuccess(result: Login) {

        val jwtId = result.jwtId
        val status = result.status
        val checkMonthChanged = result.checkMonthChanged

        //1. spf에 jwtId 저장, 로그인 상태 저장
        saveJwt(jwtId)
        saveLoginStatus(socialUserModel.providerType)

        //2. STATUS에 따른 처리
        // ACTIVE: 가입된 회원 -> 뱃지 API 호출
        // ONGOING: 가입 안된 회원/정보등록 안된 회원, Register Activity로
        when (status) {
            "ACTIVE" -> {
                if(checkMonthChanged){ // -> 뱃지 API 호출
                    BadgeService.getMonthBadge(this)
                }else{
                    startMainActivity()
                }
            }
            "ONGOING" -> startAgreeActivity()
        }

    }

    override fun onSignInFailure(code: Int, message: String) {
        signinErrorCheck("LOGIN")
    }

    /*이달의 뱃지 조회 API*/
    override fun onMonthBadgeSuccess(isBadgeExist: Boolean, monthBadge: BadgeInfo?) {
        val intent = Intent(this, MainActivity::class.java)
        if(isBadgeExist)
            intent.putExtra("badge", Gson().toJson(monthBadge))
        startActivity(intent)
    }

    override fun onMonthBadgeFailure(code: Int, message: String) {
        signinErrorCheck("BADGE")
    }

    /*액티비티 이동*/
    //Main Activity
    private fun startMainActivity() {
        startNextActivity(MainActivity::class.java)
        finish()
    }

    //Agree Activity
    private fun startAgreeActivity() {
        startNextActivity(AgreeActivity::class.java)
        finish()
    }

    /*에러 체크*/
    private fun signinErrorCheck(type: String){
        val text = if(!isNetworkAvailable(this)){ //네트워크 에러
            getString(R.string.error_network)
        }else{ //나머지
            getString(R.string.error_api_fail)
        }

        Snackbar.make(binding.root, text, Snackbar.LENGTH_INDEFINITE).setAction(getString(R.string.action_retry)) {
            when(type){
                "KAKAO" -> {
                    setKakaoLogin()
                }
                "GOOGLE" -> {
                    getResult.launch(mGoogleSignInClient.signInIntent)
                }
                "LOGIN" -> {
                    AuthService.login(this, socialUserModel)
                }
                "BADGE" -> {
                    BadgeService.getMonthBadge(this)
                }
            }
        }.show()
    }
}