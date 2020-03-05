package com.example.eatitserver.common

import com.example.eatitserver.model.CategoryModel
import com.example.eatitserver.model.FoodModel
import com.example.eatitserver.model.ServerUserModel

object Common {
    var foodSelected: FoodModel?=null
    val FULL_WITDTH_COLUMN: Int = 1
    val DEFAULT_COLUMN_COUNT: Int = 0
    var categorySelected: CategoryModel? = null
    val CATEGORY_REF = "Category"
    val SERVER_REF= "Server"
    var currenServerUser: ServerUserModel?=null
}