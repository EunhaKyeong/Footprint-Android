package com.footprint.footprint.ui.main.calendar

import android.content.Intent
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.footprint.footprint.data.model.WalkModel
import com.footprint.footprint.data.remote.walk.TagWalkDatesResponse
import com.footprint.footprint.data.remote.walk.UserDateWalk
import com.footprint.footprint.data.remote.walk.WalkDateResult
import com.footprint.footprint.databinding.FragmentSearchResultBinding
import com.footprint.footprint.ui.BaseFragment
import com.footprint.footprint.ui.adapter.WalkDateRVAdapter
import com.footprint.footprint.ui.adapter.WalkRVAdapter
import com.footprint.footprint.ui.main.MainActivity
import com.google.gson.Gson

class SearchResultFragment() : BaseFragment<FragmentSearchResultBinding>(FragmentSearchResultBinding::inflate) {

    override fun initAfterBinding() {
        val argument = navArgs<SearchResultFragmentArgs>()

        setBinding(argument.value.tag)

//        initAdapter()
    }

    private fun setBinding(tag: String) {
        binding.searchResultSearchBarTv.text = tag

        binding.searchResultSearchBarTv.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.searchResultBackIv.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.searchResultBackgroundV.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.searchResultSearchIv.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun initAdapter(walkDates: List<WalkDateResult>) {
        val adapter = WalkDateRVAdapter(requireContext())

        adapter.setWalkDates(walkDates)
        adapter.setFragmentManager(requireActivity().supportFragmentManager)

        adapter.setWalkDateRemoveListener(object : WalkDateRVAdapter.OnWalkDateRemoveListener {
            override fun onWalkDateRemove() {
                val itemCount = adapter.itemCount
                if (itemCount == 0) {
                    binding.searchResultHintTv.visibility = View.VISIBLE
                    binding.searchResultWalkDatesRv.visibility = View.GONE
                } else {
                    binding.searchResultHintTv.visibility = View.GONE
                    binding.searchResultWalkDatesRv.visibility = View.VISIBLE
                }
            }
        })

        binding.searchResultWalkDatesRv.adapter = adapter
    }
}