package com.example.bleTransfer

import android.annotation.SuppressLint
import android.app.Activity.BLUETOOTH_SERVICE
import android.app.Activity.RESULT_OK
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.example.bleTransfer.databinding.FragmentScanReceiveBinding
import com.google.gson.Gson
import com.welie.blessed.*
import kotlinx.android.synthetic.main.fragment_transfer.*
import org.json.JSONObject
import pub.devrel.easypermissions.EasyPermissions
import java.util.*
import java.lang.reflect.Method

import android.R.attr.name


private const val REQUEST_READ_CONTACTS = 101

class ScanReceiveFragment : Fragment(R.layout.fragment_scan_receive),
  EasyPermissions.PermissionCallbacks {

  private lateinit var bluetoothPeripheralManager: BluetoothPeripheralManager

  private lateinit var bluetoothCentralManager: BluetoothCentralManager
  private lateinit var bluetoothAdapter: BluetoothAdapter
  private var bleFound: MutableList<NearbyBluetooth> = mutableListOf()
  private var peripheralsFound: MutableList<BluetoothPeripheral> = mutableListOf()
  private lateinit var devicesAdapter: DevicesAdapter
  private lateinit var binding: FragmentScanReceiveBinding
  private var isRunning: Boolean = false
  private var isScanStarted: Boolean = false
  private var readGranted: Boolean = false
  private var isBLEEnabled: Boolean = false
  val COS_SERVICE_UUID: UUID = UUID.fromString("00001808-0000-1000-8000-00805f9b34fb")
  val COS_CONTACT_CHARACTERSTIC_UUID = UUID.fromString("00002A24-0000-1000-8000-00805f9b34fb")
  private lateinit var bluetoothManager: BluetoothManager
  private lateinit var countDownTimer: CountDownTimer
  private var isTimerRunning = false
  private var hasReceivedData = false
  private lateinit var sharedPref: SharedPreferences

  private val args by navArgs<ScanReceiveFragmentArgs>()


  var advertisingCallback: AdvertiseCallback = object : AdvertiseCallback() {
    override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
      super.onStartSuccess(settingsInEffect)
    }

    override fun onStartFailure(errorCode: Int) {
      Log.e("BLE", "Advertising onStartFailure: $errorCode")
      super.onStartFailure(errorCode)
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return super.onCreateView(inflater, container, savedInstanceState)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    binding = FragmentScanReceiveBinding.bind(view)
    sharedPref = requireContext().getSharedPreferences("BLE", Context.MODE_PRIVATE)
    checkBluetoothAndBLESupported()
    try {
      bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
      if (args.deviceName == null) {
        val name = sharedPref.getString("name", "")
        bluetoothAdapter.setName(name)
      } else {
        bluetoothAdapter.setName(args.deviceName)
      }

      if (!bluetoothAdapter.isEnabled) {
        val bleIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(bleIntent, 1001)
      }
    } catch (e: Exception) {
      Toast.makeText(requireContext(), "No bluetooth found in this device", Toast.LENGTH_SHORT)
        .show()
      BleApplication.INSTANCE.onScreenLog.d("BluetoothAdapter", e.localizedMessage)
      e.printStackTrace()
    }


    bluetoothCentralManager = BluetoothCentralManager(
      requireContext(),
      bluetoothCentralManagerCallback,
      Handler(Looper.getMainLooper())
    )

    val bluetoothManager = context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    bluetoothPeripheralManager =
      BluetoothPeripheralManager(requireContext(), bluetoothManager, peripheralManagerCallback)
    devicesAdapter = DevicesAdapter(bleFound) {
      bluetoothCentralManager.connectPeripheral(peripheralsFound[it], bluetoothPeripheralCallback)
    }
    binding.bleDevicesRecyclerView.adapter = devicesAdapter
    setDeviceInfo()

    startScanning()
    startAdvertising()

    binding.btnScanRec.setOnClickListener {
      startScanning()
    }
    countDownTimer = object : CountDownTimer(30000, 1000) {
      override fun onTick(p0: Long) {
        isTimerRunning = true
      }

      override fun onFinish() {
        isTimerRunning = false
        isScanStarted = false
        bluetoothCentralManager.stopScan()
        binding.btnScanRec.setText("Start Scan")
      }

    }

    activity?.onBackPressedDispatcher?.addCallback(object : OnBackPressedCallback(true) {
      override fun handleOnBackPressed() {
        if (hasReceivedData) {
          binding.layoutScanReceive.visibility = View.VISIBLE
          binding.layoutTransfer.visibility = View.GONE
          hasReceivedData = false
        } else {
          activity?.finish()
        }
      }
    })



    sharedPref.edit().putBoolean("isLoggedIn", true).apply()
  }

  private fun checkBluetoothAndBLESupported() {
    bluetoothManager = context?.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
    if (context?.getPackageManager()?.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH) == true) {
      BleApplication.INSTANCE.onScreenLog.d("Ble", "bluetooth supported")
    } else {
      BleApplication.INSTANCE.onScreenLog.d("Ble", "bluetooth not supported")
    }

    if (context?.getPackageManager()
        ?.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) == true
    ) {
      BleApplication.INSTANCE.onScreenLog.d("Ble", "bluetooth_LE supported")
    } else {
      BleApplication.INSTANCE.onScreenLog.d("Ble", "bluetooth_LE not supported")

    }
  }

  private fun startScanning() {
    if (!isScanStarted) {
      isScanStarted = true
      binding.btnScanRec.setText("Stop Scan")
      startScanningBLE()
    } else {
      isScanStarted = false
      bluetoothCentralManager.stopScan()
      binding.btnScanRec.setText("Start Scan")
    }
    var devices = ""
    bluetoothPeripheralManager.connectedCentrals.forEach {
      devices = devices + it.name + it.address + "\n"
    }
    binding.tvConnectedDevices.text = devices
  }

  private fun startAdvertising() {
    if (!isRunning) {
      startAdvertisingBLE()
      isRunning = true
    } else {
      isRunning = false
    }
  }

  private fun setDeviceInfo() {
    val bluetoothManager = context?.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
    binding.senderID.setText(bluetoothManager.adapter.address)
    val name = sharedPref.getString("name", "")
    binding.senderName.setText(args.deviceName ?: name)
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    isBLEEnabled = resultCode == RESULT_OK
    super.onActivityResult(requestCode, resultCode, data)
  }

  private fun startAdvertisingBLE() {
    val blAdapter = bluetoothManager.adapter
    val bleAdvertiser = blAdapter.bluetoothLeAdvertiser ?: return
    val bluetoothGattService = BluetoothGattService(
      COS_SERVICE_UUID,
      BluetoothGattService.SERVICE_TYPE_PRIMARY
    )
    val bluetoothGattCharacteristic = BluetoothGattCharacteristic(
      COS_CONTACT_CHARACTERSTIC_UUID,
      BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_INDICATE,
      BluetoothGattCharacteristic.PERMISSION_READ
    )
    bluetoothGattCharacteristic.setValue(getJsonData())
    bluetoothGattService.addCharacteristic(bluetoothGattCharacteristic)
    bluetoothPeripheralManager.add(bluetoothGattService)
    if (bleAdvertiser != null) {

    } else {
      BleApplication.INSTANCE.onScreenLog.d("Ble", "not supported advertising")

    }
  }

  private fun getJsonData(): String {
    val jsonObject = JSONObject()
    val name = sharedPref.getString("name", "")
    val phone = sharedPref.getString("phone", "")
    val upi = sharedPref.getString("upi", "")
    jsonObject.put("name", args.deviceName ?: name)
    jsonObject.put("phone", args.phoneNo ?: phone)
    jsonObject.put("upi", args.upiId ?: upi)
    return jsonObject.toString()
  }

  private fun startScanningBLE() {
    bluetoothCentralManager.scanForPeripherals()
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
  ) {
    when (requestCode) {
      REQUEST_READ_CONTACTS -> {
        readGranted = grantResults != null && grantResults[0] == PackageManager.PERMISSION_GRANTED
      }
    }
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
  }

  override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {}

  override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {}

  private val bluetoothCentralManagerCallback = object : BluetoothCentralManagerCallback() {
    override fun onDiscoveredPeripheral(peripheral: BluetoothPeripheral, scanResult: ScanResult) {
      //  bluetoothCentralManager.stopScan()
      //   start timer add until 30sec close it after 30 sec empty the previous list
      if (!isTimerRunning) {
        bleFound.clear()
        devicesAdapter.notifyDataSetChanged()
        countDownTimer.start()
      }

      if (!peripheralsFound.any { it.address == peripheral.address }) {
        peripheral.name.let {
          BleApplication.INSTANCE.onScreenLog.d("Ble", "onDiscoveredPeripheral ${it}")
        }
        peripheralsFound.add(peripheral)
        if (peripheral.name.isNotEmpty()) {
          bleFound.add(NearbyBluetooth(peripheral.name, peripheral.address))
          devicesAdapter.notifyDataSetChanged()
        }
      }
      // bluetoothCentralManager.connectPeripheral(peripheral, bluetoothPeripheralCallback)
    }

    override fun onConnectedPeripheral(peripheral: BluetoothPeripheral) {
      BleApplication.INSTANCE.onScreenLog.d("Ble", "onConnectedPeripheral ${peripheral.name}")

    }

    override fun onConnectionFailed(peripheral: BluetoothPeripheral, status: HciStatus) {
      BleApplication.INSTANCE.onScreenLog.d("Ble", "onConnectionFailed ${peripheral.name}")
    }

    override fun onDisconnectedPeripheral(peripheral: BluetoothPeripheral, status: HciStatus) {
      BleApplication.INSTANCE.onScreenLog.d("Ble", "onDisconnectedPeripheral ${peripheral.name}")
    }
  }

  private val bluetoothPeripheralCallback = object : BluetoothPeripheralCallback() {
    override fun onServicesDiscovered(peripheral: BluetoothPeripheral) {
      super.onServicesDiscovered(peripheral)
      BleApplication.INSTANCE.onScreenLog.d("Ble", "onServicesDiscovered ${peripheral.name}")
      peripheral.readCharacteristic(COS_SERVICE_UUID, COS_CONTACT_CHARACTERSTIC_UUID)
    }

    override fun onCharacteristicUpdate(
      peripheral: BluetoothPeripheral,
      value: ByteArray,
      characteristic: BluetoothGattCharacteristic,
      status: GattStatus
    ) {
      BleApplication.INSTANCE.onScreenLog.d(
        "Ble",
        "onCharacteristicUpdate ${peripheral.name} ${status.value}"
      )

      val characteristicUUID = characteristic.uuid
      if (characteristicUUID.equals(COS_CONTACT_CHARACTERSTIC_UUID)) {
        Toast.makeText(requireContext(), value.decodeToString(), Toast.LENGTH_LONG).show()
        //binding.receivedMsg.setText(value.decodeToString()
        // val args = bundleOf(Pair("receivedContact", value.decodeToString()))
        //  findNavController().navigate(R.id.transferFragment, args)
        binding.layoutScanReceive.visibility = View.GONE
        binding.layoutTransfer.visibility = View.VISIBLE
        val user = Gson().fromJson(value.decodeToString(), User::class.java)
        textView.text = user.name + "\n" + user.phone + "\n" + user.upi
        bluetoothCentralManager.cancelConnection(peripheral)
      }
    }
  }

  private val peripheralManagerCallback = object : BluetoothPeripheralManagerCallback() {

    override fun onCharacteristicRead(
      bluetoothCentral: BluetoothCentral,
      characteristic: BluetoothGattCharacteristic
    ) {
      super.onCharacteristicRead(bluetoothCentral, characteristic)
      BleApplication.INSTANCE.onScreenLog.d(
        "Ble",
        "onCharacteristicRead ${bluetoothCentral.name} ${characteristic.value}"
      )
    }

    override fun onServiceAdded(status: GattStatus, service: BluetoothGattService) {
      BleApplication.INSTANCE.onScreenLog.d("Ble", "onServiceAdded ${status.value} $service")

      val advertiseSettings = AdvertiseSettings.Builder()
        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
        .setConnectable(true)
        .setTimeout(0)
        .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
        .build()

      val advertiseData = AdvertiseData.Builder()
        .setIncludeTxPowerLevel(true)
        .addServiceUuid(ParcelUuid(COS_SERVICE_UUID))
        .build()

      val scanResponse = AdvertiseData.Builder()
        .setIncludeDeviceName(true)
        .build()

      val blAdapter = bluetoothManager.adapter
      val bleAdvertiser = blAdapter.bluetoothLeAdvertiser
      bleAdvertiser.startAdvertising(
        advertiseSettings,
        advertiseData,
        scanResponse,
        advertisingCallback
      )
      //bluetoothPeripheralManager.startAdvertising(advertiseSettings, scanResponse, advertiseData)
    }

    override fun onCentralConnected(bluetoothCentral: BluetoothCentral) {
      super.onCentralConnected(bluetoothCentral)
      BleApplication.INSTANCE.onScreenLog.d(
        "Ble",
        "onCentralConnected ${bluetoothCentral.name} ${bluetoothCentral.address}"
      )

    }

    override fun onCentralDisconnected(bluetoothCentral: BluetoothCentral) {
      super.onCentralDisconnected(bluetoothCentral)
      BleApplication.INSTANCE.onScreenLog.d(
        "Ble",
        "onCentralConnected ${bluetoothCentral.name} ${bluetoothCentral.address}"
      )
    }

  }


  override fun onPause() {
    super.onPause()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    stopAdvertising()
  }

  @SuppressLint("MissingPermission")
  fun stopAdvertising() {
    val blAdapter = bluetoothManager.adapter
    val bleAdvertiser = blAdapter.bluetoothLeAdvertiser
    bleAdvertiser.stopAdvertising(advertisingCallback)
  }
}