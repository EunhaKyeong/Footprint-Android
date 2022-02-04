package com.footprint.footprint.ui.walk

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.footprint.footprint.R
import com.footprint.footprint.data.model.FootprintModel
import com.footprint.footprint.data.model.FootprintsModel
import com.footprint.footprint.databinding.FragmentWalkConfirmBinding
import com.footprint.footprint.ui.BaseFragment
import com.footprint.footprint.ui.adapter.FootprintRVAdapter
import com.footprint.footprint.ui.dialog.ActionDialogFragment
import com.footprint.footprint.ui.main.calendar.WalkDetailActivity
import com.footprint.footprint.utils.getDeviceHeight
import com.google.gson.Gson
import com.sothree.slidinguppanel.SlidingUpPanelLayout

class WalkConfirmFragment :
    BaseFragment<FragmentWalkConfirmBinding>(FragmentWalkConfirmBinding::inflate) {
    private lateinit var actionDialogFragment: ActionDialogFragment
    private lateinit var footprints: FootprintsModel
    private lateinit var footprintRVAdapter: FootprintRVAdapter

    private val args by navArgs<WalkConfirmFragmentArgs>()

    //뒤로가기 콜백
    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            binding.walkConfirmSlidingUpPanelLayout.apply {
                if (panelState == SlidingUpPanelLayout.PanelState.EXPANDED || panelState == SlidingUpPanelLayout.PanelState.ANCHORED)   //SlidingUpPanelLayout 이 위로 올라가 있으면 아래로 내리기
                    panelState = SlidingUpPanelLayout.PanelState.COLLAPSED
                else if (requireActivity() is WalkDetailActivity) { //WalkDetailActivity 화면에서 온 경우 -> 액티비티 종료
                    requireActivity().finish()
                } else {    //WalkAfterActivity 화면에서 온 경우 -> ‘OO번째 산책’ 작성을 취소할까요? 다이얼로그 화면 띄우기
                    setWalkDialogBundle(getString(R.string.msg_stop_walk))
                    actionDialogFragment.show(
                        requireActivity().supportFragmentManager,
                        null
                    )
                }
            }
        }

    }

    private var position: Int = -1  //클릭된 기록 인덱스

    override fun initAfterBinding() {
        //이전 화면(WalkAfterActivity or WalkDetailActivity)에서 전달받은 기록 데이터(footprints)를 FootprintsModel 로 변환
        val footprintsStr = args.footprints
        if (footprintsStr.isNotBlank())  //footprintsStr 이 비어 있다는 건 발자국 데이터가 없다는 뜻
            footprints = Gson().fromJson(footprintsStr, FootprintsModel::class.java)

        //어댑터 초기화는 한번만
        if (!::footprintRVAdapter.isInitialized)
            initAdapter()

        setWalkDialog()
        setFootprintView()
        setMyClickListener()
        observe()

        requireActivity().onBackPressedDispatcher.addCallback(backPressedCallback)  //뒤로가기 콜백 리스너 등록
    }

    //발자국 기록을 보여주는 뷰 설정
    private fun setFootprintView() {
        if (requireActivity() is WalkDetailActivity && !::footprints.isInitialized) { //WalkDetailActivity 에서 기록이 없을 경우 -> 산책 기록이 없어요! 텍스트뷰 보여주기
            binding.walkConfirmNoFootprintTv.visibility = View.VISIBLE
            binding.walkConfirmSlidedLayout.visibility = View.INVISIBLE
        } else {    //그 이외에 모든 경우 -> slidedPanelLayout 보여주기
            binding.walkConfirmNoFootprintTv.visibility = View.INVISIBLE
            binding.walkConfirmSlidedLayout.visibility = View.VISIBLE

            binding.walkConfirmSlidingUpPanelLayout.panelHeight = (getDeviceHeight() * 0.5).toInt()

            if (!::footprints.isInitialized) {  //산책 도중 기록을 남기지 않았을 때
                binding.walkConfirmPostRv.visibility = View.INVISIBLE
                binding.walkConfirmPlusLineView.visibility = View.VISIBLE
                binding.walkConfirmPlusTv.visibility = View.VISIBLE
            } else {    //산책 도중 기록을 남겼을 때
                binding.walkConfirmPostRv.visibility = View.VISIBLE
                binding.walkConfirmPlusLineView.visibility = View.INVISIBLE
                binding.walkConfirmPlusTv.visibility = View.INVISIBLE
            }
        }
    }

    private fun setMyClickListener() {
        binding.walkConfirmPlusTv.setOnClickListener {
            val action = WalkConfirmFragmentDirections.actionWalkConfirmFragment2ToFootprintDialogFragment3("")
            findNavController().navigate(action)
        }
    }

    private fun observe() {
        //실시간 글 작성하기 화면으로부터 추가된 footprint 데이터
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<String>("footprint")
            ?.observe(viewLifecycleOwner) {
                Log.d("WalkConfirmFragment", "footprint observe -> $it")

                if (it != null) {
                    //string -> FootprintModel
                    val footprint = Gson().fromJson<FootprintModel>(it, FootprintModel::class.java)
                    footprint.isMarked = false  //산책 종료 후 추가된 발자국에는 발자국 아이콘 안 붙이기

                    if (!::footprints.isInitialized) {  //산책 중에 남긴 발자국이 없다가 종료 후 처음 발자국을 남긴 경우
                        footprints = FootprintsModel(arrayListOf(footprint))
                        footprintRVAdapter.setData(footprints.footprints)

                        binding.walkConfirmPlusTv.visibility = View.INVISIBLE
                        binding.walkConfirmPlusLineView.visibility = View.INVISIBLE
                        binding.walkConfirmPostRv.visibility = View.VISIBLE
                    } else {    //어댑터에 데이터 추가하고 UI 업데이트
                        footprintRVAdapter.addData(footprint, position + 1)
                    }
                }
            }

        //실시간 글 작성하기 화면으로부터 수정된 footprint 데이터
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<String>("updatedFootprint")
            ?.observe(viewLifecycleOwner) {
                Log.d("WalkConfirmFragment", "updatedFootprint observe -> $it")

                if (it != null) {
                    //string -> FootprintModel
                    val footprint = Gson().fromJson<FootprintModel>(it, FootprintModel::class.java)

                    //어댑터에 데이터 수정하고 UI 업데이트
                    footprintRVAdapter.updateData(footprint, position)
                }
            }
    }

    //기록 관련 리사이클러뷰 초기화
    private fun initAdapter() {
        footprintRVAdapter = FootprintRVAdapter(requireActivity().javaClass.simpleName)
        footprintRVAdapter.setMyItemClickListener(object : FootprintRVAdapter.MyItemClickListener {
            //발자국 삭제 텍스트뷰 클릭 리스너 -> 해당 발자국을 삭제할까요? 다이얼로그 화면 띄우기
            override fun showDeleteDialog(position: Int) {
                this@WalkConfirmFragment.position = position  //클릭된 post 인덱스를 전역변수로 저장해 놓는다.

                setWalkDialogBundle(getString(R.string.msg_delete_footprint))
                actionDialogFragment.show(requireActivity().supportFragmentManager, null)
            }

            //발자국 추가 텍스트뷰 클릭 리스너
            override fun addFootprint(position: Int) {
                this@WalkConfirmFragment.position = position

                if (footprints.footprints.size >= 9) {
                    //"발자국은 최대 9개까지 남길 수 있어요." 다이얼로그 화면 띄우기
                    val action =
                        WalkConfirmFragmentDirections.actionGlobalMsgDialogFragment(
                            getString(R.string.error_post_cnt_exceed)
                        )
                    findNavController().navigate(action)
                } else {
                    //발자국 작성하기 다이얼로그 화면 띄우기
                    val action =
                        WalkConfirmFragmentDirections.actionWalkConfirmFragment2ToFootprintDialogFragment3(
                            ""
                        )
                    findNavController().navigate(action)
                }
            }

            //발자국 편집 텍스트뷰 클릭 리스너
            override fun updateFootprint(position: Int, footprint: FootprintModel) {
                this@WalkConfirmFragment.position = position

                //발자국 작성하기 다이얼로그 화면 띄우기(수정)
                val action =
                    WalkConfirmFragmentDirections.actionWalkConfirmFragment2ToFootprintDialogFragment3(
                        Gson().toJson(footprint)
                    )
                findNavController().navigate(action)
            }
        })

        //어댑터에 데이터 저장
        if (::footprints.isInitialized) {
            footprintRVAdapter.setData(footprints.footprints)
        }
        binding.walkConfirmPostRv.adapter = footprintRVAdapter
    }

    //WalkDialogFragment 초기화
    private fun setWalkDialog() {
        actionDialogFragment = ActionDialogFragment()

        actionDialogFragment.setMyDialogCallback(object : ActionDialogFragment.MyDialogCallback {

            //‘OO번째 산책’ 작성을 취소할까요?
            override fun action1(isAction: Boolean) {
                if (isAction)
                    requireActivity().finish()
            }

            //해당 발자국을 삭제할까요?
            override fun action2(isAction: Boolean) {
                if (isAction)
                    footprintRVAdapter.removeData(this@WalkConfirmFragment.position)
            }
        })
    }

    //WalkDialogFragment 에 넘겨준 메세지(ex.‘OO번째 산책’을 저장할까요?)를 저장하는 함수
    private fun setWalkDialogBundle(msg: String) {
        val bundle: Bundle = Bundle()
        bundle.putString("msg", msg)

        actionDialogFragment.arguments = bundle
    }
}