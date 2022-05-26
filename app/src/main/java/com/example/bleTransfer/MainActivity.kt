package com.example.bleTransfer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI

class MainActivity : AppCompatActivity() {
  private lateinit var navController: NavController

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    val navHostFragment =
      supportFragmentManager.findFragmentById(R.id.nav_host_fragment_container) as NavHostFragment
    navController = navHostFragment.navController
    NavigationUI.setupActionBarWithNavController(this, navController)
  }
}

//ask username device name and mobile name
//
//start advertising when opens
// start scanning at the same time

// ble device name

// when app is killed or background, clear the list and then start scanning.