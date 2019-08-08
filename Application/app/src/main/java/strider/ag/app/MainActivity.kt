package strider.ag.app


import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.*
import com.fasterxml.jackson.databind.ObjectMapper

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import android.net.Uri
import android.os.Environment
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    val PENDING_RESULT_EXTRA = "pending_result"
    val URL_EXTRA = "url"
    val RSS_RESULT_EXTRA = "result"
    val REQUEST_TYPE_EXTRA = "request"
    val BODY_EXTRA = "body"
    val GET_REQUEST = "pending"
    val PUT_REQUEST = "taskDone"
    val IMAGE = "image"
    val RESULT_CODE = 0
    val HTTP_REQUEST = 1
    val ERROR_CODE = 2
    val PUT_RESULT_CODE = 3
    val PUT_ERROR_CODE = 4

    val REQUEST_IMAGE_CAPTURE = 300
    val PERMISSION_CODE = 100
    val LOCATION_PERMISSION_CODE = 200

    var currentPhotoPath: String? = null
    var tasks: MutableList<Task> = mutableListOf()
    var taskSelected = 0
    lateinit var popupWindow: PopupWindow
    lateinit var popupView: View
    lateinit var view: View
    var timer = Timer()
    private var onBackground: Boolean = false
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    val baseUrl = "http://192.168.0.42:8080/tasks/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackground = false
        setContentView(R.layout.activity_main)
        view = this.window.decorView
        // Request location updates
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        Log.d("Main","onCreate")
        timer.schedule(object :TimerTask(){
            override fun run() {
                if(!onBackground){
                    Log.d("Request", "HTTP")
                    httpRequest(GET_REQUEST, null,null)
                }

            }
        },0,10000)
    }

    private fun httpRequest(type: String,body:  Task?, id: String?){
        //Form the url based on the http request type
        var url: String? = null
        if (id == null){
            url  = baseUrl+type
        }
        else{
            url = baseUrl+type + "/" +id
        }
        val intent = Intent(applicationContext, InternetService::class.java)
        val resultPendingIntent= createPendingResult(HTTP_REQUEST, Intent(), 0)
        intent.putExtra(URL_EXTRA, url)
        intent.putExtra(REQUEST_TYPE_EXTRA,type)

        if(body != null){
            val mapper = ObjectMapper()
            val jsonString = mapper.writeValueAsString(body)
            intent.putExtra(BODY_EXTRA,jsonString)
            intent.putExtra(IMAGE,body.imageLocation)
        }

        intent.putExtra(PENDING_RESULT_EXTRA,resultPendingIntent)
        this.startService(intent)
    }


    // Map the received json String to object Task
    private fun parseJson(msg: String){
        var mapper = ObjectMapper()
        tasks = mapper.readValue(msg, mapper.typeFactory.constructCollectionType(List::class.java, Task::class.java ) )
        for (task in tasks){
            Log.d("Parse",task.name)
        }
        val listView = findViewById<ListView>(R.id.taskList)
        listView.adapter = TaskAdapter(this, this)
    }

    //First checks the Location Permissions, then checks the camera Permission
    fun checkLocationPermission(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED
                || checkSelfPermission(Manifest.permission.INTERNET) == PackageManager.PERMISSION_DENIED){
                val permission = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.INTERNET)
                requestPermissions(permission, LOCATION_PERMISSION_CODE)
            }else{
                this.checkCameraPermissions()
            }
        }else{
            this.checkCameraPermissions()
        }
    }

    //Checks camera permissions. If they're granted, opens the camera
    private fun checkCameraPermissions(){
        //If system os > Marshmallow, runtime permission request is needed
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED ||
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
                || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED){
                //permission not enabled
                val permission = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                requestPermissions(permission, PERMISSION_CODE)
            }else{
                //permission already granted
                this.openCamera()
            }
        }else{
            this.openCamera()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode){
            PERMISSION_CODE-> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    this.openCamera()
                } else {
                    Toast.makeText(this, "Permissão negada", Toast.LENGTH_SHORT).show()
                }
            }
            LOCATION_PERMISSION_CODE->{
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    this.checkCameraPermissions()
                } else {
                    Toast.makeText(this, "Permissão negada", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openCamera() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    null
                }
                // Continue only if the File was successfully created
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "com.example.android.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation(){
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                tasks[taskSelected].latitude =  location?.latitude?.toFloat()
                tasks[taskSelected].longitude  = location?.longitude?.toFloat()
                val msg = "Latitude: "  + tasks[taskSelected].latitude + "  Longitude: " + tasks[taskSelected].longitude
                Log.d("Location", msg)
                sendTaskSolution()
            }
    }

    private fun sendTaskSolution(){
        tasks[taskSelected].imageLocation = currentPhotoPath
        Log.d("PUT",File(currentPhotoPath).length().toString())
        httpRequest(PUT_REQUEST,tasks[taskSelected],tasks[taskSelected].id.toString())
        showPopupWindowClick()
    }

    private fun removeConcludedTask(){
        tasks.removeAt(taskSelected)
        val listView = findViewById<ListView>(R.id.taskList)
        listView.adapter = TaskAdapter(this, this)
    }

    @SuppressLint("SetTextI18n")
    private fun showPopupWindowClick() {

        // inflate the layout of the popup window
        val inflater = LayoutInflater.from(this)
        popupView = inflater.inflate(R.layout.popup_window, null)

        // create the popup window
        val width = LinearLayout.LayoutParams.WRAP_CONTENT
        val height = LinearLayout.LayoutParams.WRAP_CONTENT
        val focusable = false
        popupWindow = PopupWindow(popupView, width, height, focusable)

        // show the popup window
        // which view you pass in doesn't matter, it is only used for the window tolken
        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.shadow))
        } else {
            popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.shadow))
        }
        popupWindow.elevation = 100F
        val textMsg = popupView.findViewById<TextView>(R.id.textMsg)
        textMsg.text = textMsg.text.toString() + ": " + tasks[taskSelected].name

        // dismiss the popup window when touched
    }

    @SuppressLint("SetTextI18n")
    private fun changePopupWindow(success: Boolean){
        val progressBar = popupView.findViewById<ProgressBar>(R.id.progressBar1)
        progressBar.visibility = View.GONE
        val button = popupView.findViewById<Button>(R.id.cancelButton)
        button.visibility = View.VISIBLE
        button.setOnClickListener{
            popupWindow.dismiss()
        }
        val textMsg = popupView.findViewById<TextView>(R.id.textMsg)
        if(success){
            textMsg.text = "Solução enviada"
            removeConcludedTask()
        }else{
            textMsg.text = "Erro no envio da solução"
        }



    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_IMAGE_CAPTURE) {
            getLastLocation()
        }else if(requestCode == HTTP_REQUEST){
            if(resultCode == RESULT_CODE){
                var msg = data!!.getStringExtra(RSS_RESULT_EXTRA)
                parseJson(msg)
            }else if(resultCode == ERROR_CODE){
                Log.d("Activity Result","error")
            }else if(resultCode == PUT_RESULT_CODE){
                changePopupWindow(true)
            }else if(resultCode == PUT_ERROR_CODE){
                changePopupWindow(false)
            }
            else{
                Log.d("Activity Result","shit")
            }

        }else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private class TaskAdapter(context: Context, activity: MainActivity): BaseAdapter(){

        private val mContext: Context
        private val mActivity: MainActivity

        init{
            mContext = context
            mActivity = activity
        }

        override fun getView(position: Int, convertView: View?, viewGroup: ViewGroup?): View {
            val layoutInflater = LayoutInflater.from(mContext)
            val row = layoutInflater.inflate(R.layout.row_layout, viewGroup,false)

            val positionTextView = row.findViewById<TextView>(R.id.textView)
            positionTextView.text = this.mActivity.tasks[position].name

            val doButton = row.findViewById<Button>(R.id.button)
            doButton.setOnClickListener{
                this.mActivity.taskSelected = position
                this.mActivity.checkLocationPermission()
            }

            return row
        }

        override fun getItem(position: Int): Any {
            return "Test"
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getCount(): Int {
            return this.mActivity.tasks.size
        }

    }

    override fun onResume() {
        super.onResume()
        Log.d("Main","onResume")
        onBackground = false

    }
    override fun onPause() {
        super.onPause()
        Log.d("Main","onPause")
        onBackground = true
    }

    override fun onDestroy() {
        Log.d("Main","OnDestroy")
        super.onDestroy()
    }

}
