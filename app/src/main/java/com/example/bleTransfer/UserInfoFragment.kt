package com.example.bleTransfer

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.bleTransfer.databinding.FragmentUserInfoBinding
import pub.devrel.easypermissions.EasyPermissions

class UserInfoFragment : Fragment(R.layout.fragment_user_info),
  EasyPermissions.PermissionCallbacks {

  private lateinit var binding: FragmentUserInfoBinding
  private lateinit var sharedPref: SharedPreferences

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      if (!EasyPermissions.hasPermissions(
          requireContext(),
          Manifest.permission.BLUETOOTH_ADVERTISE,
          Manifest.permission.BLUETOOTH,
          Manifest.permission.BLUETOOTH_CONNECT,
          Manifest.permission.ACCESS_FINE_LOCATION
        )
      ) EasyPermissions.requestPermissions(
        this,
        "location and bluetooth permission is must",
        101,
        Manifest.permission.BLUETOOTH_ADVERTISE,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_CONNECT
      )
    } else {
      if (!EasyPermissions.hasPermissions(
          requireContext(),
          Manifest.permission.BLUETOOTH,
          Manifest.permission.BLUETOOTH_ADMIN,
          Manifest.permission.ACCESS_FINE_LOCATION
        )
      ) EasyPermissions.requestPermissions(
        this,
        "location and bluetooth permission is must",
        101,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN
      )
    }

    sharedPref = requireContext().getSharedPreferences("BLE", Context.MODE_PRIVATE)
    if (sharedPref.getBoolean("isLoggedIn", false)) {
     findNavController().navigate(R.id.scanReceiveFragment, ScanReceiveFragmentArgs(
      null,
       null,
       null
     ).toBundle())
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    binding = FragmentUserInfoBinding.bind(view)

    binding.btnNext.setOnClickListener {
      if (binding.editDeviceName.text != null && binding.editPhoneNo.text != null && binding.editUpiId.text != null) {
        sharedPref.edit().putString("name", binding.editDeviceName.text.toString()).apply()
        sharedPref.edit().putString("phone", binding.editPhoneNo.text.toString()).apply()
        sharedPref.edit().putString("upi", binding.editUpiId.text.toString()).apply()
        findNavController().navigate(
          R.id.scanReceiveFragment, ScanReceiveFragmentArgs(
            binding.editDeviceName.text.toString(),
            binding.editPhoneNo.text.toString(),
            binding.editUpiId.text.toString()
          ).toBundle()
        )
      } else {
        Toast.makeText(requireContext(), "Enter all fields first!", Toast.LENGTH_SHORT).show()
      }
    }
  }

  override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {

  }

  override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {

  }
}