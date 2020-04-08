package com.example.eatitserver.callback

import com.example.eatitserver.model.OrderModel

interface IOrderCallbackListener {
    fun onOrderLoadSucces(orderList: List<OrderModel>)
    fun onOrderLoadFailed(message: String)
}