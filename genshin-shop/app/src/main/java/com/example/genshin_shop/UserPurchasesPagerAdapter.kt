package com.example.genshin_shop

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class UserPurchasesPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> UserToShipFragment()
            1 -> UserToReceiveFragment()
            2 -> UserCompletedFragment()
            else -> throw IllegalStateException("Unexpected position $position")
        }
    }
}
