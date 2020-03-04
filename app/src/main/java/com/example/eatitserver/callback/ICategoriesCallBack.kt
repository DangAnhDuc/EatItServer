package com.example.eatitserver.callback

import com.example.eatitserver.model.CategoryModel

interface ICategoriesCallBack {
    fun onCategoriesLoadSuccess(categoryModelList: List<CategoryModel>)
    fun onCategoriesLoadFailed(message: String)
}