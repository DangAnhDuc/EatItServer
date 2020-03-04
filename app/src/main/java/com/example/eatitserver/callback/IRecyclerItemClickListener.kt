package com.example.eatitserver.callback

import android.view.View

interface IRecyclerItemClickListener {
    fun onItemClick(view: View, pos: Int)
}