package com.example.eatitserver.ui.foodlist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.eatitserver.common.Common
import com.example.eatitserver.model.FoodModel

class FoodlistViewModel : ViewModel() {

    private var mutableFoodModelList: MutableLiveData<List<FoodModel>>? = null

    fun getMutableFoodModelList(): MutableLiveData<List<FoodModel>> {
        if (mutableFoodModelList == null)
            mutableFoodModelList = MutableLiveData()
        mutableFoodModelList!!.value = Common.categorySelected!!.foods
        return mutableFoodModelList!!
    }
}