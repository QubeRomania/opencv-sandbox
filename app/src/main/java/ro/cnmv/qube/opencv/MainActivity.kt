package ro.cnmv.qube.opencv

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.Camera
import android.os.Bundle
import android.util.Log
import android.view.*
import kotlinx.android.synthetic.main.activity_main.*
import org.bytedeco.javacv.AndroidFrameConverter
import org.bytedeco.javacv.OpenCVFrameConverter
import org.bytedeco.javacpp.opencv_core.*
import org.bytedeco.javacpp.opencv_imgproc.*

class MainActivity : Activity() {
    private lateinit var preview: CameraPreview

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_main)

        requestPermissions(arrayOf(Manifest.permission.CAMERA), 0)

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            throw Error("Camera permission not granted")
        }

        val frameConverter = AndroidFrameConverter()
        val converter = OpenCVFrameConverter.ToMat()
        val gray = Mat()
        val contours = MatVector()

        val width = 640
        val height = 480

        preview = CameraPreview(surfaceView, width, height,
            Camera.PreviewCallback { data, camera ->
                val frame = frameConverter.convert(data, width, height)

                val image = converter.convertToMat(frame)

                cvtColor(image, gray, COLOR_BGR2GRAY)

                Canny(gray, gray, 100.0, 200.0, 3, false)

                contours.clear()
                findContours(gray, contours, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE)

                drawContours(image, contours, -1, Scalar(0.0, 255.0, 255.0, 255.0))

                val bitmap = frameConverter.convert(frame)
                imageView.setImageBitmap(bitmap)

                camera.addCallbackBuffer(data)
            }
        )
    }
}

class CameraPreview(sv: SurfaceView, w: Int, h: Int, cb: Camera.PreviewCallback) : SurfaceHolder.Callback {
    private lateinit var camera: Camera
    private val callback: Camera.PreviewCallback = cb

    init {
        val holder = sv.holder
        holder.addCallback(this)
        holder.setFixedSize(w, h)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        camera = Camera.open()
        camera.setPreviewDisplay(holder)

        val picSizes = camera.parameters.supportedPreviewSizes
        picSizes.forEach{ Log.e("Supported size", "${it.width}x${it.height}") }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        val params = camera.parameters

        val sizes = params.supportedPreviewSizes[0]

        with(params) {
            setPreviewSize(width, height)
            previewFormat = ImageFormat.NV21
        }

        camera.parameters = params

        camera.setPreviewCallbackWithBuffer(callback)
        val size = params.previewSize
        val data = ByteArray(size.width * size.height *
                ImageFormat.getBitsPerPixel(params.previewFormat) / 8)
        camera.addCallbackBuffer(data)

        camera.startPreview()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        camera.stopPreview()
        camera.release()
    }
}
