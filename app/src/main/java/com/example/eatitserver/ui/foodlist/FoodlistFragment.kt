package com.example.eatitserver.ui.foodlist

import android.annotation.SuppressLint
import android.app.Activity
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.DisplayMetrics
import android.view.*
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.eatitserver.R
import com.example.eatitserver.SizeAddonActivity
import com.example.eatitserver.adapter.MyFoodListAdapter
import com.example.eatitserver.callback.IMyButtonCallback
import com.example.eatitserver.common.Common
import com.example.eatitserver.common.MySwipeHelper
import com.example.eatitserver.eventbus.AddoSizeEditEvent
import com.example.eatitserver.eventbus.ChangeMenuClick
import com.example.eatitserver.eventbus.ToastEvent
import com.example.eatitserver.model.FoodModel
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import dmax.dialog.SpotsDialog
import org.greenrobot.eventbus.EventBus
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class FoodlistFragment : Fragment() {

    private lateinit var foodlistViewModel: FoodlistViewModel

    var recycler_food: RecyclerView? = null
    var layoutAnimationController: LayoutAnimationController? = null
    var adapter: MyFoodListAdapter? = null
    var foodModelList: List<FoodModel> = ArrayList<FoodModel>()

    internal lateinit var img_food: ImageView
    internal lateinit var storage: FirebaseStorage
    internal lateinit var storageReference: StorageReference
    private var imageUri: Uri?=null
    private val PICK_IMAGE_REQUEST:Int=333
    private lateinit var dialog: android.app.AlertDialog

    @SuppressLint("FragmentLiveDataObserve")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        foodlistViewModel =
            ViewModelProviders.of(this).get(FoodlistViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_foodlist, container, false)
        initView(root)
        foodlistViewModel.getMutableFoodModelList().observe(this, Observer {
            if(it !=null) {
                foodModelList= it!!
                adapter = MyFoodListAdapter(context!!, foodModelList)
                recycler_food!!.adapter = adapter
                recycler_food!!.layoutAnimation = layoutAnimationController
            }
        })
        return root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.food_list_menu, menu)
        val menuItem = menu.findItem(R.id.action_search)
        val searchManager = activity!!.getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView = menuItem.actionView as androidx.appcompat.widget.SearchView

        searchView.setSearchableInfo(searchManager.getSearchableInfo(activity!!.componentName))

        searchView.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(search: String?): Boolean {
                startSearchFood(search)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }

        })
        val closeButton = searchView.findViewById<View>(R.id.search_close_btn) as ImageView
        closeButton.setOnClickListener {
            val ed = searchView.findViewById<View>(R.id.search_src_text) as EditText
            ed.setText("")
            searchView.setQuery("", false)
            searchView.onActionViewCollapsed()
            menuItem.collapseActionView()
            foodlistViewModel.getMutableFoodModelList().value = Common.categorySelected!!.foods
        }

    }

    private fun startSearchFood(search: String?) {
        val resultFood: MutableList<FoodModel> = ArrayList()
        for (i in Common.categorySelected!!.foods!!.indices) {
            val foodModel = Common.categorySelected!!.foods!![i]
            if (foodModel.name!!.toLowerCase().contains(search.toString())) {
                foodModel.positioInList = i
                resultFood.add(foodModel)
            }
        }

        foodlistViewModel!!.getMutableFoodModelList().value = resultFood
    }

    private fun initView(root: View?) {

        setHasOptionsMenu(true)
        storage= FirebaseStorage.getInstance()
        storageReference= storage.reference
        dialog = SpotsDialog.Builder().setContext(context).setCancelable(false).build()

        recycler_food = root!!.findViewById(R.id.recylcer_food_list) as RecyclerView
        recycler_food!!.setHasFixedSize(true)
        recycler_food!!.layoutManager = LinearLayoutManager(context)
        layoutAnimationController =
            AnimationUtils.loadLayoutAnimation(context, R.anim.layout_item_from_left)

        (activity as AppCompatActivity).supportActionBar!!.title = Common.categorySelected!!.name

        val displayMetrics = DisplayMetrics()
        activity!!.windowManager.defaultDisplay.getMetrics(displayMetrics)
        val width = displayMetrics.widthPixels

        var swipe = object : MySwipeHelper(context!!, recycler_food!!, width / 6) {
            override fun instaniateMyButton(
                viewHolder: RecyclerView.ViewHolder,
                buffer: MutableList<MyButton>
            ) {
                buffer.add(
                    MyButton(
                        context!!,
                        "Delete",
                        30,
                        0,
                        Color.parseColor("#9b0000"),
                        object : IMyButtonCallback {
                            override fun onClick(pos: Int) {
                                Common.foodSelected= foodModelList[pos]
                                val buider = AlertDialog.Builder(context!!)
                                buider.setTitle("Delete")
                                    .setMessage("Dp you really want to delete food?")
                                    .setNegativeButton("CANCEL", { dialogInterface, _ -> dialogInterface.dismiss() })
                                    .setPositiveButton("OK"){dialogInterface, i ->
                                        val foodModel = adapter!!.getItemAtPosition(pos)
                                        if (foodModel.positioInList == -1) {
                                            Common.categorySelected!!.foods!!.removeAt(pos)
                                        } else
                                            Common.categorySelected!!.foods!!.removeAt(foodModel.positioInList)
                                        updateFood(Common.categorySelected!!.foods!!,true)
                                    }
                                val deleteDialog= buider.create()
                                deleteDialog.show()
                            }

                        })
                )

                buffer.add(
                    MyButton(
                        context!!,
                        "Update",
                        30,
                        0,
                        Color.parseColor("#560027"),
                        object : IMyButtonCallback {
                            override fun onClick(pos: Int) {
                                val foodModel = adapter!!.getItemAtPosition(pos)
                                if (foodModel.positioInList == -1) {
                                    showUpdateDialog(pos, foodModel)
                                } else {
                                    showUpdateDialog(foodModel.positioInList, foodModel)
                                }
                            }

                        })
                )

                buffer.add(
                    MyButton(
                        context!!,
                        "Size",
                        30,
                        0,
                        Color.parseColor("#12005e"),
                        object : IMyButtonCallback {
                            override fun onClick(pos: Int) {
                                val foodModel = adapter!!.getItemAtPosition(pos)
                                if (foodModel.positioInList == -1) {
                                    Common.foodSelected = foodModelList[pos]
                                } else {
                                    Common.foodSelected = foodModel
                                }
                                startActivity(Intent(context, SizeAddonActivity::class.java))
                                if (foodModel.positioInList == -1) {
                                    EventBus.getDefault().postSticky(AddoSizeEditEvent(false, pos))
                                } else {
                                    EventBus.getDefault().postSticky(
                                        AddoSizeEditEvent(
                                            false,
                                            foodModel.positioInList
                                        )
                                    )
                                }
                            }

                        })
                )

                buffer.add(
                    MyButton(
                        context!!,
                        "Addon",
                        30,
                        0,
                        Color.parseColor("#333639"),
                        object : IMyButtonCallback {
                            override fun onClick(pos: Int) {
                                val foodModel = adapter!!.getItemAtPosition(pos)
                                if (foodModel.positioInList == -1) {
                                    Common.foodSelected = foodModelList[pos]
                                } else {
                                    Common.foodSelected = foodModel
                                }
                                startActivity(Intent(context, SizeAddonActivity::class.java))
                                if (foodModel.positioInList == -1) {
                                    EventBus.getDefault().postSticky(AddoSizeEditEvent(true, pos))
                                } else {
                                    EventBus.getDefault().postSticky(
                                        AddoSizeEditEvent(
                                            true,
                                            foodModel.positioInList
                                        )
                                    )
                                }
                            }

                        })
                )
            }

        }
    }

    private fun showUpdateDialog(pos: Int, foodModel: FoodModel) {
        var builder = androidx.appcompat.app.AlertDialog.Builder(context!!)
        builder.setTitle("Update Category")
        builder.setMessage("Please fill information")

        val itemvView= LayoutInflater.from(context!!).inflate(R.layout.layout_update_food,null)
        val edt_food_name= itemvView.findViewById<View>(R.id.edt_food_name) as EditText
        val edt_food_price= itemvView.findViewById<View>(R.id.edt_food_price) as EditText
        val edt_food_description= itemvView.findViewById<View>(R.id.edt_food_description) as EditText
        img_food= itemvView.findViewById<View>(R.id.img_food) as ImageView

        edt_food_name.setText(foodModel.name)
        edt_food_price.setText(foodModel.price.toString())
        edt_food_description.setText(foodModel.description)

        Glide.with(context!!).load(foodModel.image).into(img_food)

        img_food.setOnClickListener{
            val intent= Intent()
            intent.type= "image/*"
            intent.action= Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST)
        }

        builder.setNegativeButton("CANCEL"){dialogInterface, _ -> dialog.dismiss() }
        builder.setPositiveButton("OK"){dialogInterface, i ->
            val updateFood = foodModel
            updateFood.name= edt_food_name.text.toString()
            updateFood.price= if( TextUtils.isEmpty(edt_food_price.text))
                0
            else
                edt_food_price.text.toString().toLong()
            updateFood.description= edt_food_description.text.toString()

            if(imageUri !=null){
                dialog.setMessage("Uploading")
                dialog.show()

                val imageName= UUID.randomUUID().toString()
                val imageFolder= storageReference.child("images/$imageName")
                imageFolder.putFile(imageUri!!)
                    .addOnFailureListener{ e ->
                        dialog.dismiss()
                        Toast.makeText(context, ""+e.message,Toast.LENGTH_SHORT).show()
                    }
                    .addOnProgressListener { taskSnapshot ->
                        val progress= 100.0*taskSnapshot.bytesTransferred/taskSnapshot.totalByteCount
                        dialog.setMessage("Uploadded $progress%")}
                    .addOnSuccessListener {
                        dialogInterface.dismiss()
                        imageFolder.downloadUrl.addOnSuccessListener { uri ->
                            dialog.dismiss()
                            updateFood.image = uri.toString()
                            Common.categorySelected!!.foods!![pos] = updateFood
                            updateFood(Common.categorySelected!!.foods!!, false)
                        }
                    }
            }
            else{
                Common.categorySelected!!.foods!![pos]= updateFood
                updateFood(Common.categorySelected!!.foods!!,false)
            }
        }
        builder.setView(itemvView)
        var updateDialog= builder.create()
        updateDialog.show()
    }

    private fun updateFood(foods: MutableList<FoodModel>?, isDelete: Boolean) {
        val updateData = HashMap<String, Any>()
        updateData["foods"]= foods!!
        FirebaseDatabase.getInstance()
            .getReference(Common.CATEGORY_REF)
            .child(Common.categorySelected!!.menu_id!!)
            .updateChildren(updateData)
            .addOnFailureListener{ e ->
                Toast.makeText(context, ""+e.message, Toast.LENGTH_SHORT).show()
            }.addOnCompleteListener{task ->
                if( task.isSuccessful){
                    dialog.dismiss()
                    foodlistViewModel.getMutableFoodModelList()
                    EventBus.getDefault().postSticky(ToastEvent(!isDelete,true))
                }
            }
    }

    override fun onDestroy() {
        EventBus.getDefault().postSticky(ChangeMenuClick(true))
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if( requestCode== PICK_IMAGE_REQUEST && resultCode== Activity.RESULT_OK) {
            if (data != null && data.data != null) {
                imageUri = data.data
                img_food.setImageURI(imageUri)
            }
        }
    }


}
