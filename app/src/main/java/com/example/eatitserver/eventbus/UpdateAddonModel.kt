package com.example.eatitserver.eventbus

import com.example.eatitclient.Model.AddonModel

class UpdateAddonModel {
    var addonModelList: List<AddonModel>? = null

    constructor()
    constructor(addonModelList: List<AddonModel>?) {
        this.addonModelList = addonModelList
    }
}