package ro.cnmv.qube.opencv

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.Camera
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.activity_main.*
import org.bytedeco.javacpp.Loader

import org.bytedeco.javacv.AndroidFrameConverter
import org.bytedeco.javacv.OpenCVFrameConverter
import org.bytedeco.javacpp.Loader.sizeof
import org.bytedeco.javacpp.helper.opencv_imgproc.cvFindContours
import org.bytedeco.javacpp.opencv_core.*
import org.bytedeco.javacpp.opencv_imgproc.*


class MainActivity : Activity() {
    private lateinit var camera: Camera

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestPermissions(arrayOf(Manifest.permission.CAMERA), 0)

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            throw Error("Camera permission not granted")
        }

        val ctx = applicationContext

        val imagesLayout = LinearLayout(ctx)
        imagesLayout.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        imagesLayout.orientation = LinearLayout.VERTICAL
        root.addView(imagesLayout)

        val surfaceView = SurfaceView(ctx)
        imagesLayout.addView(surfaceView)

        val imageView = ImageView(ctx)
        imagesLayout.addView(imageView)

        camera = Camera.open()
        camera.setDisplayOrientation(0)

        val params = camera.parameters

        //val picSizes = params.supportedPreviewSizes
        //picSizes.forEach { Log.e("PicSize", "${it.width}x${it.height}") }

        val width = 640
        val height = 640

        with(params) {
            setPreviewSize(width, height)
            previewFormat = ImageFormat.NV21

            focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO
        }

        camera.parameters = params

        val surfaceHolder = surfaceView.holder
        surfaceHolder.setFixedSize(width, height)

        surfaceHolder.addCallback(object: SurfaceHolder.Callback {
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
            }

            override fun surfaceCreated(holder: SurfaceHolder) {
                camera.setPreviewDisplay(holder)

                camera.startPreview()

                val frameConverter = AndroidFrameConverter()
                val converter = OpenCVFrameConverter.ToMat()

                camera.setPreviewCallback { buffer, _ ->
                    val frame = frameConverter.convert(buffer, width, height)

                    val image = converter.convertToMat(frame)

                    val gray = Mat()
                    cvtColor(image, gray, COLOR_BGR2GRAY);

                    val thresh = Mat()
                    threshold(gray, thresh, 127.0, 255.0, THRESH_BINARY);

                    val contours = MatVector()
                    findContours(thresh, contours, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE)

                    drawContours(image, contours, -1, Scalar(0.0, 255.0, 255.0, 255.0))

                    val bitmap = frameConverter.convert(frame)
                    imageView.setImageBitmap(bitmap)
                }
            }
        })

    }

    override fun onDestroy() {
        super.onDestroy()
        camera.release()
    }
}
