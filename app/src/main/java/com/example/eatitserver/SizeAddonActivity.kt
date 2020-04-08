package com.example.eatitserver

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eatitclient.Model.AddonModel
import com.example.eatitclient.Model.SizeModel
import com.example.eatitserver.adapter.MyAddonAdapter
import com.example.eatitserver.adapter.MySizeAdapter
import com.example.eatitserver.common.Common
import com.example.eatitserver.eventbus.*
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_size_addon.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class SizeAddonActivity : AppCompatActivity() {

    var sizeAdapter: MySizeAdapter? = null
    var addonAdapter: MyAddonAdapter? = null

    private var foodEditPosition = -1
    private var needSave = false
    private var isAddon = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_size_addon)
        init()
    }

    private fun init() {
        setSupportActionBar(tool_bar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)

        recycler_addon_size.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(this)
        recycler_addon_size!!.layoutManager = layoutManager
        recycler_addon_size.addItemDecoration(
            DividerItemDecoration(
                this,
                layoutManager.orientation
            )
        )

        btn_create.setOnClickListener {
            if (!isAddon) {
                if (sizeAdapter != null) {
                    val sizeModel = SizeModel()
                    sizeModel.name = edt_name.text.toString()
                    sizeModel.price = edt_price.text.toString().toLong()
                    sizeAdapter!!.addNewSize(sizeModel)
                }
            } else {
                if (addonAdapter != null) {
                    val addonModel = AddonModel()
                    addonModel.name = edt_name.text.toString()
                    addonModel.price = edt_price.text.toString().toLong()
                    addonAdapter!!.addNewAddon(addonModel)
                }
            }
        }

        btn_edit.setOnClickListener {
            if (!isAddon) {
                if (sizeAdapter != null) {
                    val sizeModel = SizeModel()
                    sizeModel.name = edt_name.text.toString()
                    sizeModel.price = edt_price.text.toString().toLong()
                    sizeAdapter!!.editSize(sizeModel)
                }
            } else {
                if (addonAdapter != null) {
                    val addonModel = AddonModel()
                    addonModel.name = edt_name.text.toString()
                    addonModel.price = edt_price.text.toString().toLong()
                    addonAdapter!!.editAddon(addonModel)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().removeStickyEvent(UpdateSizeModel::class.java)
        super.onStop()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_size_addon, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_save -> saveData()
            android.R.id.home -> {
                if (needSave) {
                    val buider = AlertDialog.Builder(this)
                        .setTitle("Cancel?")
                        .setMessage("Do you really wan to close withou saving")
                        .setNegativeButton("CANCEL") { dialogInterface, _ -> dialogInterface.dismiss() }
                        .setPositiveButton("OK") { dialogInterface, i ->
                            needSave = false
                            closeActivity()
                        }
                    val dialog = buider.create()
                    dialog.show()
                } else {
                    closeActivity()
                }
            }
        }
        return true
    }

    private fun saveData() {
        if (foodEditPosition != -1) {
            Common.categorySelected!!.foods?.set(foodEditPosition, Common.foodSelected!!)
            val updateData: MutableMap<String, Any> = HashMap()
            updateData["foods"] = Common.categorySelected!!.foods!!

            FirebaseDatabase.getInstance()
                .getReference(Common.CATEGORY_REF)
                .child(Common.categorySelected!!.menu_id!!)
                .updateChildren(updateData)
                .addOnFailureListener { e: Exception ->
                    Toast.makeText(
                        this@SizeAddonActivity,
                        "" + e.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this@SizeAddonActivity, "Reload Success", Toast.LENGTH_SHORT)
                            .show()
                        needSave = false
                        edt_name.setText("")
                        edt_price.setText("0")
                    }
                }
        }
    }

    private fun closeActivity() {
        edt_name.setText("")
        edt_price.setText("")
        finish()

    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onAdddSizeReceive(event: AddoSizeEditEvent) {
        if (!event.isAddon) {
            if (Common.foodSelected!!.size != null) {
                sizeAdapter = MySizeAdapter(this, Common.foodSelected!!.size.toMutableList())
                foodEditPosition = event.pos
                recycler_addon_size!!.adapter = sizeAdapter
                isAddon = event.isAddon
            }
        } else {
            if (Common.foodSelected!!.addon != null) {
                addonAdapter = MyAddonAdapter(this, Common.foodSelected!!.addon.toMutableList())
                foodEditPosition = event.pos
                recycler_addon_size!!.adapter = addonAdapter
                isAddon = event.isAddon
            }
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onSizeModelUpdate(event: UpdateSizeModel) {
        if (event.sizeModelList != null) {
            needSave = true;
            Common.foodSelected!!.size = event.sizeModelList!!
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onAddonModelUpdate(event: UpdateAddonModel) {
        if (event.addonModelList != null) {
            needSave = true;
            Common.foodSelected!!.addon = event.addonModelList!!
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onSelectSizeEvent(event: SelectSizeModel) {
        if (event.sizeModel != null) {
            edt_name.setText(event.sizeModel.name!!)
            edt_price.setText(event.sizeModel!!.price!!.toString())
            btn_edit.isEnabled = true
        } else {
            btn_edit.isEnabled = false
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onSelectAddonEvent(event: SelectAddonModel) {
        if (event.addonmodel != null) {
            edt_name.setText(event.addonmodel.name!!)
            edt_price.setText(event.addonmodel!!.price!!.toString())
            btn_edit.isEnabled = true
        } else {
            btn_edit.isEnabled = false
        }
    }


}
