package com.example.eatitserver.eventbus

import com.example.eatitserver.model.CategoryModel

class CategoryClick(
    var isSuccess: Boolean,
    var category: CategoryModel
) {
}