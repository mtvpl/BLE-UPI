package com.example.bleTransfer

import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.bleTransfer.databinding.ListItemBleFoundBinding

class DevicesAdapter(
  private val devices: List<NearbyBluetooth>,
  private val clickListener: (index:Int) -> Unit
) :
  RecyclerView.Adapter<DevicesAdapter.DeviceViewHolder>() {
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
    val layoutInflater = LayoutInflater.from(parent.context)
    val binding: ListItemBleFoundBinding = DataBindingUtil.inflate(
      layoutInflater,
      R.layout.list_item_ble_found, parent, false
    )
    return DeviceViewHolder(binding)
  }

  @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
  override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
    holder.bind(devices[position], position,clickListener)
  }

  override fun getItemCount(): Int {
    return devices.size
  }

  inner class DeviceViewHolder(val binding: ListItemBleFoundBinding) :
    RecyclerView.ViewHolder(binding.root) {

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun bind(
      device: NearbyBluetooth,
      index:Int,
      clickListener: (index:Int) -> Unit
    ) {
      Log.e("Adapter", device.toString())
      binding.bleSearchName.text = device.name
      binding.bleSearchAddress.text = device.address
      binding.imageConnect.setOnClickListener {
        clickListener(index)
      }
    }
  }
}
