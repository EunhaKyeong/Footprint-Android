package com.footprint.footprint.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.RoundedCorner
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.footprint.footprint.data.dto.CourseDTO
import com.footprint.footprint.databinding.ItemCourseBinding
import com.footprint.footprint.ui.main.course.CourseListFragment
import com.footprint.footprint.utils.LogUtils

class CourseListRVAdapter(val context: Context): RecyclerView.Adapter<CourseListRVAdapter.ViewHolder>() {
    private val courseList = arrayListOf<CourseDTO>() /* 수정 */

    inner class ViewHolder(val binding: ItemCourseBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(course: CourseDTO, position: Int) {
            // 타이틀, 인포
            binding.itemCourseTitleTv.text = course.courseName
            val dist = "${course.courseDist}km,"
            val time = if(course.courseTime<60) "약 ${course.courseTime}분" else "약 ${course.courseTime/60}시간 ${course.courseTime%60}분"
            binding.itemCourseInfoTv.text = dist + time

            // 이미지
            Glide.with(context)
                .load(course.courseImg)
                .apply(RequestOptions.bitmapTransform(RoundedCorners(10)))
                .into(binding.itemCourseImageIv)

            // 사람 수, 찜 수
            if(course.courseCount == 0){
                binding.itemCourseParticipantCountIv.visibility = View.GONE
                binding.itemCourseParticipantCountTv.visibility = View.GONE
            }else{
                binding.itemCourseParticipantCountIv.visibility = View.VISIBLE
                binding.itemCourseParticipantCountTv.visibility = View.VISIBLE
            }
            if(course.courseLike == 0){
                binding.itemCourseLikeCountIv.visibility = View.GONE
                binding.itemCourseLikeCountTv.visibility = View.GONE
            }else{
                binding.itemCourseLikeCountIv.visibility = View.VISIBLE
                binding.itemCourseLikeCountTv.visibility = View.VISIBLE
            }
            binding.itemCourseParticipantCountTv.text = "${course.courseCount}명"
            binding.itemCourseLikeCountTv.text = "${course.courseLike}개"

            // tag RV
            val tagRVAdapter = CourseTagRVAdapter(course.courseTags)
            binding.itemCourseTagRv.adapter = tagRVAdapter

            // 찜하기 버튼 관련
            binding.itemCourseLikeIv.isSelected = course.userCourseMark
            binding.itemCourseLikeIv.setOnClickListener {
                myCourseClickListener.markCourse(course.courseIdx)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCourseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(courseList[position], position)
        holder.binding.root.setOnClickListener{
            myCourseClickListener.onClick(courseList[position])
        }
    }

    override fun getItemCount(): Int = courseList.size

    /* 아이템 관리 */
    fun addAll(list: List<CourseDTO>){
        courseList.clear()
        courseList.addAll(list)
        notifyDataSetChanged()
    }

    /* 클릭 이벤트 관리 */
    interface CourseClickListener{
        fun onClick(course: CourseDTO)
        fun markCourse(courseIdx: String)
    }

    private lateinit var myCourseClickListener: CourseClickListener

    fun setMyClickListener(myCourseClickListener: CourseClickListener){
        this.myCourseClickListener = myCourseClickListener
    }
}