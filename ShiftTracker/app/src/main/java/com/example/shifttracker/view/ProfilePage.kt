package com.example.shifttracker.view

import android.Manifest
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.example.shifttracker.LocationServicesStatusReceiver
import com.example.shifttracker.R
import com.example.shifttracker.databinding.FragmentProfilePageBinding
import com.example.shifttracker.model.TimeValue
import com.example.shifttracker.viewmodel.LocationMonitoringService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.net.ntp.NTPUDPClient
import org.apache.commons.net.ntp.TimeInfo
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import kotlin.properties.Delegates


class ProfilePage : Fragment() {
    private lateinit var  binding : FragmentProfilePageBinding
    private lateinit var auth: FirebaseAuth
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var userLocation: android.location.Location? = null
    private lateinit var receiver: LocationServicesStatusReceiver

    private var entryButtonEnabled = true
    private var exitButtonEnabled = false





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_profile_page, container, false)
        binding.profileObject=this
        val email = auth.currentUser!!.email.toString()
        binding.userName.text=email

        checkLocationPerm()

        return binding.root
    }
    fun navigatePastShift(){
        findNavController().navigate(R.id.action_profilePage_to_pastShifts)
    }

    fun entryButton ()
    {
        if (entryButtonEnabled) {


            receiver = LocationServicesStatusReceiver()
            // IntentFilter ile hangi eylemlerin dinleneceğini belirt
            val filter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
            // Receiver'ı kaydet
            requireActivity().registerReceiver(receiver, filter)

            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
            CoroutineScope(Dispatchers.IO).launch {
                val time = getNtpTime()

                val date = Date(time)

                val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss")

                val formattedDate = dateFormat.format(date)
                withContext(Dispatchers.Main) {
                    if (ContextCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        fusedLocationClient.lastLocation
                            .addOnSuccessListener { location : android.location.Location? ->
                                userLocation = location
                                val desiredLatitude =37.8504236
                                val desiredLongitude =27.8421963 //,

                                // Kullanıcı konumu alındıysa ve istenilen konuma yakınsa
                                if (userLocation != null && isLocationCloseEnough(desiredLatitude, desiredLongitude)) {


                                    val db = FirebaseFirestore.getInstance()
                                    val guncelZaman = formattedDate
                                    val user = auth.currentUser
                                    val email = user?.email ?: ""

                                    val userCollectionRef = db.collection(email)
                                    userCollectionRef.add(TimeValue(guncelZaman.toString(),"giriş",System.currentTimeMillis()))
                                        .addOnSuccessListener { documentReference ->
                                            entryButtonEnabled = false
                                            exitButtonEnabled = true

                                            println("Firestore'a eklendi")
                                            Toast.makeText(requireContext(),"Giriş Yapıldı",Toast.LENGTH_LONG).show()


                                        }
                                        .addOnFailureListener { e ->

                                            println("Firestore'a eklenirken hata oluştu: $e")


                                        }
                                    Toast.makeText(requireContext(), "Konum istenilen konumda!", Toast.LENGTH_LONG).show()                        }
                                else {
                                    Toast.makeText(requireContext(), "Konum istenilen konuma yakın değil!", Toast.LENGTH_LONG).show()
                                }
                            }
                    } else {
                        requestPermissions(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                            LOCATION_PERMISSION_REQUEST_CODE
                        )            }

                }
            }




        } else {
            Toast.makeText(requireContext(), "Giriş işlemi zaten yapıldı.", Toast.LENGTH_SHORT).show()
        }
    }



    private suspend fun getNtpTime(): Long {
        return withContext(Dispatchers.IO) {
            // Ağ işlemleri burada yapılır
            try {
                val client = NTPUDPClient()
                val address = InetAddress.getByName("pool.ntp.org")
                val info: TimeInfo = client.getTime(address)
                info.computeDetails()
                info.message.receiveTimeStamp.time
            } catch (e: Exception) {
                e.printStackTrace()
                0L
            }
        }
    }




    fun exitButton () {

        if (exitButtonEnabled) {
            CoroutineScope(Dispatchers.IO).launch {
                val time = getNtpTime()

                val date = Date(time)

                val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss")

                val formattedDate = dateFormat.format(date)

                println("Formatlanmış tarih: $formattedDate")
                withContext(Dispatchers.Main) {

                    val db = FirebaseFirestore.getInstance()
                    val guncelZaman = formattedDate
                    val user = auth.currentUser

                    val email = user?.email ?: ""

                    val userCollectionRef = db.collection(email)
                    userCollectionRef.add(TimeValue(guncelZaman.toString(),"çıkış",System.currentTimeMillis()))
                        .addOnSuccessListener { documentReference ->
                            println("Firestore'a eklendi")
                            Toast.makeText(requireContext(), "Çıkış Yapıldı", Toast.LENGTH_LONG).show()
                        }
                        .addOnFailureListener { e ->
                            println("Firestore'a eklenirken hata oluştu: $e")
                        }

                }
            }

            val serviceIntent = Intent(context, LocationMonitoringService::class.java)
            requireContext().stopService(serviceIntent)

            entryButtonEnabled = true
            exitButtonEnabled = false

        }else{
            Toast.makeText(requireContext(), "Çıkış işlemi zaten yapıldı.", Toast.LENGTH_SHORT).show()
        }
    }

    fun signOut() {
        // Firebase oturumu kapatma işlemi
        auth.signOut()
        Toast.makeText(requireContext(), "Oturum kapatıldı", Toast.LENGTH_LONG).show()

        navigateToLoginScreen()
    }
    fun navigateToLoginScreen() {
        findNavController().navigate(R.id.action_profilePage_to_loginScreen)
    }
    private fun requestLocationPermission() {
        requestPermissions(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Konum izni kullanıcı tarafından verildi
                } else {
                    // Konum izni kullanıcı tarafından reddedildi
                }
            }
        }
    }

    fun checkLocationPerm (){
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Konum izni zaten verilmiş
        } else {
            if(ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(),Manifest.permission.ACCESS_FINE_LOCATION)){
                Snackbar.make(binding.root,"permission needed location",Snackbar.LENGTH_INDEFINITE).setAction("Give Permission"){
                    requestLocationPermission()
                }.show()
            }
            else
            {
                requestLocationPermission()

            }
        }
    }

   /* private fun getLocation() {// izinler varsa fusedlocationı başkatıyo
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location : android.location.Location? ->
                    userLocation = location
                    checkIfCloseEnough()
                }
        } else {
            requestLocationPermission()
        }
    }*/


  /*  private fun checkIfCloseEnough() {// usrlocation geldiyse ve tolerence değeri karşılanıyorsa performu devreye sokuyo
        // İstenilen latitude ve longitude değerleri
        val desiredLatitude = 37.83796721200396
        val desiredLongitude = 27.84553382412314 //,

        // Kullanıcı konumu alındıysa ve istenilen konuma yakınsa
        if (userLocation != null && isLocationCloseEnough(desiredLatitude, desiredLongitude)) {

            performAction()
        } else {
            Toast.makeText(requireContext(), "Konum istenilen konuma yakın değil!", Toast.LENGTH_LONG).show()
        }
    }
*/

    private fun isLocationCloseEnough(desiredLatitude: Double, desiredLongitude: Double): Boolean {
        val tolerance = 1.6// Uygun bir tolerans değeri belirleyin
        return userLocation?.let { location ->
            (Math.abs(desiredLatitude - location.latitude) < tolerance) &&
                    (Math.abs(desiredLongitude - location.longitude) < tolerance)
        } ?: false
    }

 /*  private fun performAction() {//db e kaydediyor
        val currentTimeMillis = getNtpTime()

        val db = FirebaseFirestore.getInstance()
        val guncelZaman = currentTimeMillis
        val user = auth.currentUser
        val email = user?.email ?: ""

        val userCollectionRef = db.collection(email)
        userCollectionRef.add(TimeValue(guncelZaman.toString(),"giriş"))
            .addOnSuccessListener { documentReference ->

                println("Firestore'a eklendi")
                Toast.makeText(requireContext(),"Giriş Yapıldı",Toast.LENGTH_LONG).show()


            }
            .addOnFailureListener { e ->

                println("Firestore'a eklenirken hata oluştu: $e")


            }
        Toast.makeText(requireContext(), "Konum istenilen konumda!", Toast.LENGTH_LONG).show()
    }
*/


}