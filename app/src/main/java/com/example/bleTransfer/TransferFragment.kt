package com.example.bleTransfer

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.google.gson.Gson
import kotlinx.android.synthetic.main.fragment_transfer.*

class TransferFragment : Fragment(R.layout.fragment_transfer) {

  private val args by navArgs<TransferFragmentArgs>()
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val user = Gson().fromJson(args.receivedContact,User::class.java)

    textView.text = user.name+"\n"+user.phone+"\n"+user.upi
  }
}