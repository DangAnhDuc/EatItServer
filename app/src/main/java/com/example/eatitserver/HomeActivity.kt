package com.example.eatitserver

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.navigation.NavController
import com.example.eatitserver.common.Common
import com.example.eatitserver.eventbus.CategoryClick
import com.example.eatitserver.eventbus.ChangeMenuClick
import com.example.eatitserver.eventbus.ToastEvent
import com.google.firebase.auth.FirebaseAuth
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class HomeActivity : AppCompatActivity() {

    private var menuClick: Int=-1
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var drawerLayout:DrawerLayout
    private lateinit var navController: NavController
    private lateinit var navView: NavigationView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)


        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_category, R.id.nav_foodlist, R.id.nav_sign_out
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        navView.setNavigationItemSelectedListener(object :
            NavigationView.OnNavigationItemSelectedListener {
            override fun onNavigationItemSelected(p0: MenuItem): Boolean {
                p0.isChecked = true
                drawerLayout!!.closeDrawers()
                if (p0.itemId == R.id.nav_sign_out) {
                    signOut()
                } else if (p0.itemId == R.id.nav_category) {
                    if (menuClick != p0.itemId) {
                        navController.navigate(R.id.nav_category)
                    }
                }
                menuClick=p0!!.itemId
                return true
            }

        })
    }

    private fun signOut() {
        val buider = androidx.appcompat.app.AlertDialog.Builder(this)
        buider.setTitle("Sign out")
            .setMessage("Do you really want to exit")
            .setNegativeButton("CANCEL", { dialogInterface, _ -> dialogInterface.dismiss() })
            .setPositiveButton("OK") { dialogInterface, _ ->
                Common.foodSelected = null
                Common.categorySelected = null
                Common.currenServerUser=null
                FirebaseAuth.getInstance().signOut()

                val intent = Intent(this@HomeActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        val dialog = buider.create()
        dialog.show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.home, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onCategorySelected(event: CategoryClick) {
        if (event.isSuccess) {
            if (menuClick != R.id.nav_foodlist) {
                navController.navigate(R.id.nav_foodlist)
                menuClick= R.id.nav_foodlist
            }
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onChangeMenuEvent(event: ChangeMenuClick) {
        if (event.isFromFoodList) {
            navController!!.navigate(R.id.nav_category)
        }
        menuClick=-1
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onToastEvent(event: ToastEvent) {
        if (event.isUpdate) {
            Toast.makeText(this, "Update Success", Toast.LENGTH_SHORT).show()
        }
        else{
            Toast.makeText(this, "Delete Success", Toast.LENGTH_SHORT).show()
        }

        EventBus.getDefault().postSticky(ChangeMenuClick(event.isBackFromFoodList))

    }
}
