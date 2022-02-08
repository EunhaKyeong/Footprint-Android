package com.footprint.footprint.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.footprint.footprint.data.remote.walk.DayWalkResult
import com.footprint.footprint.data.remote.walk.UserDateWalk
import com.footprint.footprint.databinding.ItemWalkBinding
import com.footprint.footprint.ui.dialog.ActionDialogFragment

class WalkRVAdapter(val context: Context) : RecyclerView.Adapter<WalkRVAdapter.WalkViewHolder>() {
    private val walks = arrayListOf<DayWalkResult>()

    private lateinit var mOnItemClickListener: OnItemClickListener
    private lateinit var mOnItemRemoveClickListener: OnItemRemoveClickListener
    private lateinit var fragmentManager: FragmentManager


    interface OnItemClickListener {
        fun onItemClick(walk: UserDateWalk)
    }

    interface OnItemRemoveClickListener {
        fun onItemRemoveClick()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setWalks(walks: List<DayWalkResult>) {
        this.walks.clear()
        this.walks.addAll(walks)

        notifyDataSetChanged()
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        mOnItemClickListener = listener
    }

    fun setOnItemRemoveClickListener(listener: OnItemRemoveClickListener) {
        mOnItemRemoveClickListener = listener
    }

    fun setFragmentManager(fragmentManager: FragmentManager) {
        this.fragmentManager = fragmentManager
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun removeWalk(position: Int) {
        if (walks.isEmpty() || position !in 0..walks.size) {
            return
        }

        walks.removeAt(position)
        notifyDataSetChanged()
    }

    private fun showRemoveDialog(position: Int) {
        val actionDialogFragment = ActionDialogFragment()

        actionDialogFragment.setMyDialogCallback(object : ActionDialogFragment.MyDialogCallback {
            override fun action1(isAction: Boolean) {
                if (isAction) {
                    // remove API
                    removeWalk(position)
                    mOnItemRemoveClickListener.onItemRemoveClick()
                }
            }

            override fun action2(isAction: Boolean) {
            }
        })

        val bundle = Bundle()
        bundle.putString("msg", "'${walks[position].walk.walkIdx}번째 산책' 을 삭제하시겠어요?")

        actionDialogFragment.arguments = bundle
        actionDialogFragment.show(fragmentManager, null)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalkViewHolder {
        val binding = ItemWalkBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WalkViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WalkViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int {
        return walks.size
    }

    inner class WalkViewHolder(val binding: ItemWalkBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            val walk = walks[position].walk
            binding.walkNthRecordTv.text = walk.walkIdx.toString()

            binding.root.setOnClickListener {
                mOnItemClickListener.onItemClick(walk)
            }

            binding.walkRemoveTv.setOnClickListener {
                showRemoveDialog(position)
            }

            binding.walkTimeTv.text = String.format("%s~%s", walk.startTime, walk.endTime)

            Glide.with(context).load(walk.pathImageUrl).into(binding.walkPathIv)

            val hashtag = walks[position].hashtag
            for (idx in hashtag.indices) {
                when(idx) {
                    1 -> {
                        binding.walkTag1Tv.visibility = View.VISIBLE
                        binding.walkTag1Tv.text = hashtag[idx]
                    }
                    2 -> {
                        binding.walkTag2Tv.visibility = View.VISIBLE
                        binding.walkTag2Tv.text = hashtag[idx]
                    }
                    3 -> {
                        binding.walkTag3Tv.visibility = View.VISIBLE
                        binding.walkTag3Tv.text = hashtag[idx]
                    }
                    4 -> {
                        binding.walkTag4Tv.visibility = View.VISIBLE
                        binding.walkTag4Tv.text = hashtag[idx]
                    }
                    5 -> {
                        binding.walkTag5Tv.visibility = View.VISIBLE
                        binding.walkTag5Tv.text = hashtag[idx]
                    }
                }
            }
        }
    }
}