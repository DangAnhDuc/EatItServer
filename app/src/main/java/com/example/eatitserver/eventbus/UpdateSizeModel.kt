package com.example.eatitserver.eventbus

import com.example.eatitclient.Model.SizeModel

class UpdateSizeModel {
    var sizeModelList: List<SizeModel>? = null

    constructor()
    constructor(sizeModelList: List<SizeModel>?) {
        this.sizeModelList = sizeModelList
    }
}