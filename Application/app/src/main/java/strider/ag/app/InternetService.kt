package strider.ag.app


import android.app.IntentService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.app.PendingIntent
import android.content.Context
import android.content.Entity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.Image
import android.util.Log
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import cz.msebera.android.httpclient.NameValuePair
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity
import cz.msebera.android.httpclient.client.methods.HttpGet
import cz.msebera.android.httpclient.client.methods.HttpPut
import cz.msebera.android.httpclient.entity.mime.HttpMultipartMode
import cz.msebera.android.httpclient.impl.client.HttpClientBuilder
import java.io.InputStream
import java.net.MalformedURLException
import java.net.URL
import cz.msebera.android.httpclient.entity.mime.MultipartEntityBuilder
import cz.msebera.android.httpclient.entity.mime.content.ContentBody
import cz.msebera.android.httpclient.entity.mime.content.StringBody
import cz.msebera.android.httpclient.message.BasicNameValuePair
import android.os.AsyncTask.execute
import android.os.Build
import android.support.v4.app.NotificationCompat
import cz.msebera.android.httpclient.HttpResponse
import cz.msebera.android.httpclient.client.entity.EntityBuilder
import cz.msebera.android.httpclient.entity.ContentType
import cz.msebera.android.httpclient.entity.FileEntity
import cz.msebera.android.httpclient.entity.StringEntity
import cz.msebera.android.httpclient.entity.mime.content.FileBody
import cz.msebera.android.httpclient.message.BasicHeader
import cz.msebera.android.httpclient.message.BasicHeaderElement
import cz.msebera.android.httpclient.protocol.HTTP
import org.json.JSONObject
import java.io.File

class InternetService: IntentService("Internet Service") {

    private val TAG = InternetService::class.java!!.getSimpleName()

    val PENDING_RESULT_EXTRA = "pending_result"
    val URL_EXTRA = "url"
    val RSS_RESULT_EXTRA = "result"
    val REQUEST_TYPE_EXTRA = "request"
    val IMAGE = "image"
    val BODY_EXTRA = "body"
    val GET_REQUEST = "pending"
    val PUT_REQUEST = "taskDone"
    val RESULT_CODE = 0
    val PUT_RESULT_CODE = 3
    val INVALID_URL_CODE = 1
    val ERROR_CODE = 2
    val PUT_ERROR_CODE = 4



    override fun onHandleIntent(intent: Intent?) {
        val reply = intent!!.getParcelableExtra<PendingIntent>(PENDING_RESULT_EXTRA)
        val type = intent!!.getStringExtra(REQUEST_TYPE_EXTRA)
        val url = URL(intent!!.getStringExtra(URL_EXTRA))

        when(type){
            GET_REQUEST->{

                var builder = HttpClientBuilder.create()
                var httpClient = builder.build()

                try {
                    try {

                        var getRequest =  HttpGet(url.toString())

                        val response = httpClient.execute(getRequest)
                        val buffer = response.entity.content

                        val result = Intent()
                        var string: String? = null
                        string = buffer.bufferedReader().readLine()
                        result.putExtra(RSS_RESULT_EXTRA, string)
                        reply.send(this, RESULT_CODE, result)

                    } catch (exc: MalformedURLException) {
                        reply.send(INVALID_URL_CODE);
                    }catch (exc: Exception) {
                        // could do better by treating the different sax/xml exceptions individually
                        reply.send(ERROR_CODE)
                    }

                } catch (exc: PendingIntent.CanceledException) {
                    Log.i(TAG, "reply cancelled", exc)
                }
            }

            PUT_REQUEST->{
                try{
                    var imageLoc = intent!!.getStringExtra(IMAGE)
                    var builder = HttpClientBuilder.create()
                    var httpClient = builder.build()
                    val entityBuilder = MultipartEntityBuilder.create()

                    val body = intent!!.getStringExtra(BODY_EXTRA)
                    var put= HttpPut(url.toString())
                    val stringEntity = StringEntity(body)
                    stringEntity.contentType = BasicHeader(HTTP.CONTENT_TYPE, "application/json")
                    entityBuilder.addPart("task",StringBody(body, ContentType.APPLICATION_JSON))
                    Log.d("PUT",File(imageLoc).length().toString())
                    entityBuilder.addBinaryBody("image",File(imageLoc))
                    put.entity = entityBuilder.build()

                    val response = httpClient.execute(put)
                    val result = Intent()
                    result.putExtra("PutResponse",response.toString())
                    reply.send(this, PUT_RESULT_CODE, result)
                }catch (exc: MalformedURLException) {
                    reply.send(INVALID_URL_CODE)
                }catch (exc: Exception) {
                    reply.send(PUT_ERROR_CODE)
                }
            }
        }
    }
}