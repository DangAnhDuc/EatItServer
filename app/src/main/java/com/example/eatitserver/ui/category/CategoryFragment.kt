package com.example.eatitserver.ui.category

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.eatitserver.R
import com.example.eatitserver.adapter.MyCategoriesAdapter
import com.example.eatitserver.callback.IMyButtonCallback
import com.example.eatitserver.common.Common
import com.example.eatitserver.common.MySwipeHelper
import com.example.eatitserver.eventbus.MenuItemback
import com.example.eatitserver.eventbus.ToastEvent
import com.example.eatitserver.model.CategoryModel
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import dmax.dialog.SpotsDialog
import org.greenrobot.eventbus.EventBus
import java.io.InputStream
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class CategoryFragment : Fragment() {

    private lateinit var categoryViewModel: CategoryViewModel
    private lateinit var dialog: AlertDialog
    private lateinit var layoutAnimationController: LayoutAnimationController
    private var adapter: MyCategoriesAdapter? = null
    private var recycler_menu: RecyclerView? = null

    internal var categoryModels: List<CategoryModel> = ArrayList<CategoryModel>()
    internal lateinit var storage: FirebaseStorage
    internal lateinit var storageReference: StorageReference
    private var imageUri: Uri?=null
    internal lateinit var img_category:ImageView
    private val PICK_IMAGE_REQUEST:Int=333

    @SuppressLint("FragmentLiveDataObserve")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        categoryViewModel =
            ViewModelProviders.of(this).get(CategoryViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_category, container, false)

        initViews(root)

        categoryViewModel.getMessageError().observe(this, Observer {
            Toast.makeText(context, "" + it, Toast.LENGTH_SHORT).show()
        })

        categoryViewModel.getCategoryList().observe(this, Observer {
            dialog.dismiss()
            categoryModels= it
            adapter = MyCategoriesAdapter(context!!, it)
            recycler_menu!!.adapter = adapter
            recycler_menu!!.layoutAnimation = layoutAnimationController
        })
        return root
    }

    private fun initViews(root: View) {

        storage= FirebaseStorage.getInstance()
        storageReference= storage.reference
        dialog = SpotsDialog.Builder().setContext(context).setCancelable(false).build()
        dialog.show()
        layoutAnimationController =
            AnimationUtils.loadLayoutAnimation(context, R.anim.layout_item_from_left)
        recycler_menu = root.findViewById(R.id.recycler_menu) as RecyclerView
        recycler_menu!!.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(context)
        layoutManager.orientation = RecyclerView.VERTICAL
        recycler_menu!!.layoutManager = layoutManager
        recycler_menu!!.addItemDecoration(DividerItemDecoration(context,layoutManager.orientation))
        var swipe = object : MySwipeHelper(context!!, recycler_menu!!, 200) {
            override fun instaniateMyButton(
                viewHolder: RecyclerView.ViewHolder,
                buffer: MutableList<MyButton>
            ) {
                buffer.add(
                    MyButton(
                        context!!,
                        "Update",
                        30,
                        0,
                        Color.parseColor("#560027"),
                        object : IMyButtonCallback {
                            override fun onClick(pos: Int) {
                                Common.categorySelected= categoryModels[pos]
                                showUpdateDialog()
                            }

                        })
                )
            }

        }
    }

    private fun showUpdateDialog() {
        var builder = androidx.appcompat.app.AlertDialog.Builder(context!!)
        builder.setTitle("Update Category")
        builder.setMessage("Please fill information")

        val itemvView= LayoutInflater.from(context!!).inflate(R.layout.layout_update_category,null)
        val edt_category_name= itemvView.findViewById<View>(R.id.edt_category_name) as EditText
        img_category= itemvView.findViewById<View>(R.id.img_category) as ImageView

        edt_category_name.setText(Common.categorySelected!!.name)
        Glide.with(context!!).load(Common.categorySelected!!.image).into(img_category)

        img_category.setOnClickListener{
            val intent= Intent()
            intent.type= "image/*"
            intent.action= Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST)
        }

        builder.setNegativeButton("CANCEL"){dialogInterface, _ -> dialog.dismiss() }
        builder.setPositiveButton("UPDATE"){dialogInterface, _ ->
            val updateData= HashMap<String,Any>()
            updateData["name"]= edt_category_name.text.toString()
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
                        dialog.setMessage("Uploading $progress%")
                        dialogInterface.dismiss()
                        imageFolder.downloadUrl.addOnSuccessListener { uri ->
                            updateData["image"]= uri.toString()
                            uploadCategory(updateData)
                        }
                    }
            }
            else{
                uploadCategory(updateData)
            }
        }
        builder.setView(itemvView)
        var updaloaddialog= builder.create()
        updaloaddialog.show()
    }

    private fun uploadCategory(updateData: java.util.HashMap<String, Any>) {
        FirebaseDatabase.getInstance()
            .getReference(Common.CATEGORY_REF)
            .child(Common.categorySelected!!.menu_id!!)
            .updateChildren(updateData)
            .addOnFailureListener{ e ->
                Toast.makeText(context, ""+e.message,Toast.LENGTH_SHORT).show()
            }
            .addOnSuccessListener { task->
                categoryViewModel!!.loadCategory()
             }

    }

    override fun onDestroy() {
        EventBus.getDefault().postSticky(MenuItemback())
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if( requestCode== PICK_IMAGE_REQUEST && resultCode== Activity.RESULT_OK) {
            if (data != null && data.data != null) {
                imageUri = data.data
                img_category.setImageURI(imageUri)
            }
        }
    }
}
