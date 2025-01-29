package com.example.genshin_shop

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class UserHomePagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> UserCharactersFragment()
            1 -> UserAllProductsFragment()
            2 -> UserWishlistedFragment()
            else -> Fragment()
        }
    }
}
