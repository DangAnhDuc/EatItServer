package com.example.eatitserver

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import com.example.eatitserver.common.Common
import com.example.eatitserver.model.ServerUserModel
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import dmax.dialog.SpotsDialog
import kotlinx.android.synthetic.main.app_bar_home.*
import java.util.*

class MainActivity : AppCompatActivity() {

    private var firebaseAuth:FirebaseAuth?=null
    private var listener: FirebaseAuth.AuthStateListener ?=null
    private var dialog: AlertDialog?=null
    private var serverRef: DatabaseReference?=null
    private var  providers: List<AuthUI.IdpConfig>?=null


    companion object{
        private val APP_REQUEST_CODE=7171
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        init()
    }

    private fun init() {
        providers= Arrays.asList<AuthUI.IdpConfig>( AuthUI.IdpConfig.PhoneBuilder().build())
        serverRef= FirebaseDatabase.getInstance().getReference(Common.SERVER_REF)
        firebaseAuth = FirebaseAuth.getInstance()
        dialog= SpotsDialog.Builder().setContext(this).setCancelable(false).build()
        listener= object: FirebaseAuth.AuthStateListener{
            override fun onAuthStateChanged(firebaseAuth: FirebaseAuth) {
                val user= firebaseAuth.currentUser
                if( user !=null){
                    checkUserFromFirebase(user)
                }
                else{
                    phoneLogin()
                }
            }

        }
    }

    private fun checkUserFromFirebase(user: FirebaseUser) {
        dialog!!.show()
        serverRef!!.child(user.uid)
            .addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    dialog!!.dismiss()
                    Toast.makeText(this@MainActivity,""+p0.message,Toast.LENGTH_SHORT).show();
                }

                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if(dataSnapshot.exists()){
                        val userModel= dataSnapshot.getValue(ServerUserModel::class.java)
                        if(userModel!!.isActive){
                            goToHomeActivity(userModel)
                        }
                        else{
                            dialog!!.dismiss()
                            Toast.makeText(this@MainActivity,"You must be allowed from Admin to acces this app",Toast.LENGTH_SHORT).show();

                        }
                    }
                    else{
                        dialog!!.dismiss()
                        showRegisterDialog(user)
                    }
                }
            })
    }

    private fun goToHomeActivity(userModel: ServerUserModel) {
        dialog!!.dismiss()
        Common.currenServerUser= userModel
        startActivity(Intent(this, HomeActivity::class.java))
        finish()

    }

    private fun showRegisterDialog(user: FirebaseUser) {
        val builder= androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("REGISTER")
        builder.setMessage("Please fill information")

        val itemView = LayoutInflater.from(this@MainActivity)
            .inflate(R.layout.layout_register,null)

        val edt_name= itemView.findViewById<EditText>(R.id.edt_name)
        val edt_phone= itemView.findViewById<EditText>(R.id.edt_phone)

        edt_phone.setText(user
            .phoneNumber)

        builder.setNegativeButton("CANCEL") { dialogInterface, i -> dialogInterface.dismiss()}
        builder.setPositiveButton("REGISTER") { dialogInterface, i ->
            if (TextUtils.isEmpty(edt_name.text.toString())) {
                Toast.makeText(this@MainActivity, "Please enter your name", Toast.LENGTH_SHORT)
                    .show()
                return@setPositiveButton
            }


            val serverUserModel = ServerUserModel()
            serverUserModel.uid = user!!.uid
            serverUserModel.name = edt_name.text.toString()
            serverUserModel.phone = edt_phone.text.toString()
            serverUserModel.isActive = false

            serverRef!!.child(user!!.uid)
                .setValue(serverUserModel)
                .addOnFailureListener { e ->
                    dialog!!.dismiss()
                    Toast.makeText(this@MainActivity, "" + e.message, Toast.LENGTH_SHORT).show()
                }
                .addOnCompleteListener { task ->
                    dialog!!.dismiss()
                    Toast.makeText(
                        this@MainActivity,
                        "Register Success! Admin will check and active user soon",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
        builder.setView(itemView)
        val registerDialog= builder.create()
        registerDialog.show()

    }

    private fun phoneLogin() {
        startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder().setAvailableProviders(providers!!).build(), APP_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode== APP_REQUEST_CODE){
            if(resultCode== Activity.RESULT_OK){
                val  user= FirebaseAuth.getInstance().currentUser
            }
            else{
                Toast.makeText(this,"Failed to sign in", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStop() {
        firebaseAuth!!.removeAuthStateListener(listener!!)
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        firebaseAuth!!.addAuthStateListener(listener!!)
    }
}
