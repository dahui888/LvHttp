package com.lvhttp.test

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.donkingliang.imageselector.utils.ImageSelector
import com.donkingliang.imageselector.utils.UriUtils
import com.donkingliang.imageselector.utils.VersionUtils
import com.lvhttp.net.LvHttp
import com.lvhttp.net.download.DownResponse
import com.lvhttp.net.download.start
import com.lvhttp.net.launch.*
import com.lvhttp.net.param.createFileRequestBody
import com.lvhttp.net.param.createPart
import com.lvhttp.net.param.createParts
import com.lvhttp.net.param.createRequestBody
import com.lvhttp.net.response.ResultState
import com.lvhttp.test.response.ResponseData
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.launch
import java.io.*


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        test.setOnClickListener {

            lifecycleScope.launch {
                launchHttp {
                    Toast.makeText(this@MainActivity, "加载中", Toast.LENGTH_SHORT).show()
                    LvHttp.createApi(Service::class.java).get()
                }.toData {
                    Toast.makeText(this@MainActivity, "${it.data}", Toast.LENGTH_SHORT).show()
                }.toError {
                    //Error
                }
                //Stop loading
            }

//                launchHttpPack {
//                    LvHttp.createApi(Service::class.java).get2()
//                }.toData {
//                    Toast.makeText(this@MainActivity, "${it.data}", Toast.LENGTH_SHORT).show()
//                }
//            }
//            Toast.makeText(this, "加载中", Toast.LENGTH_SHORT).show()

//            //并发
//            val list = arrayListOf<suspend () -> ResponseData<ArticleBean>>()
//            (0..10).forEach { _ ->
//                list.add {
//                    LvHttp.createApi(Service::class.java).get()
//                }
//            }
//
//            lifecycleScope.launch {
//                zipLaunch(list) {
//                    it.forEachIndexed { index, resultState ->
//                        resultState.toData {
//                            Log.e("---345--->$index", "${it.data}")
//                        }.toError {
//                            Log.e("---345--->", "${it.printStackTrace()}");
//                        }
//                    }
//                }
//            }
        }

        downloadButton.setOnClickListener {

            lifecycleScope.launch {
                launchHttpPack {
                    LvHttp.createApi(Service::class.java).download()
                        .start(object : DownResponse("LvHttp", "chebangyang.apk") {
                            override fun create(size: Float) {
                                Log.e("-------->", "create:总大小 ${(size)} ")
                            }

                            @SuppressLint("SetTextI18n")
                            override fun process(process: Float) {
                                downloadPath.setText("$process %")
                            }

                            override fun error(e: Exception) {
                                e.printStackTrace()
                                downloadPath.setText("下载错误")
                            }

                            override fun done(file: File) {
                                //完成
                                Toast.makeText(this@MainActivity, "成功", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        })
                }
            }
        }


    }

    private fun post() {

        lifecycleScope.launch {
            launchHttp {
                LvHttp.createApi(Service::class.java).login("15129379467", "147258369")
            }.toData {
                Toast.makeText(this@MainActivity, it.data.toString(), Toast.LENGTH_SHORT).show()
            }.toError {
                Log.e("---345--->", "${it.printStackTrace()}");
            }
        }
    }

    /**
     * 文件上传
     */
    private fun upload() {
        ImageSelector.builder()
//            .useCamera(true) // 设置是否使用拍照
//            .setSingle(true)  //设置是否单选
            .setCrop(true)
            .onlyTakePhoto(true)
            .start(this, 0x0001)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0x0001 && data != null) {
            val array = data.getStringArrayListExtra(ImageSelector.SELECT_RESULT)
            val file = File(array[0])
            Log.e("-------", array[0])


            Glide.with(this)
                .load(file)
                .into(image)


//            val requestBody = createFileRequestBody(file)

            lifecycleScope.launch {
                launchHttp {
                    LvHttp.createApi(Service::class.java)
                        .postFile(*createParts(mapOf("key" to file, "key2" to file)))
                }.toData {
                    Toast.makeText(this@MainActivity, "成功", Toast.LENGTH_SHORT).show()
                }.toError {
                    Toast.makeText(this@MainActivity, "失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    fun copyUriToExternalFilesDir(uri: Uri, fileName: String): File? {
        val inputStream = contentResolver.openInputStream(uri)
        val tempDir = getExternalFilesDir("temp")
        if (inputStream != null && tempDir != null) {
            val file = File("$tempDir/$fileName")
            val fos = FileOutputStream(file)
            val bis = BufferedInputStream(inputStream)
            val bos = BufferedOutputStream(fos)
            val byteArray = ByteArray(1024)
            var bytes = bis.read(byteArray)
            while (bytes > 0) {
                bos.write(byteArray, 0, bytes)
                bos.flush()
                bytes = bis.read(byteArray)
            }
            bos.close()
            fos.close()
            return file
        }
        return null
    }

}
