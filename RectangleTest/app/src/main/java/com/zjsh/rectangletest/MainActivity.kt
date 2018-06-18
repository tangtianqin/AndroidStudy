package com.zjsh.rectangletest

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.widget.EditText
import android.widget.Toast

import org.opencv.android.BaseLoaderCallback
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.JavaCameraView
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

import java.util.ArrayList
import java.util.Collections

class MainActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2 {

    //view holder
    internal var cameraBridgeViewBase: CameraBridgeViewBase? = null

    //camera listener callback
    lateinit internal var baseLoaderCallback: BaseLoaderCallback

    //image holder
    lateinit internal var bwIMG: Mat
    lateinit internal var hsvIMG: Mat
    lateinit internal var lrrIMG: Mat
    lateinit internal var urrIMG: Mat
    lateinit internal var dsIMG: Mat
    lateinit internal var usIMG: Mat
    lateinit internal var cIMG: Mat
    lateinit internal var hovIMG: Mat
    lateinit internal var approxCurve: MatOfPoint2f

    internal var threshold: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //initialize treshold
        threshold = 100

        cameraBridgeViewBase = findViewById<View>(R.id.cameraViewer) as CameraBridgeViewBase?
        cameraBridgeViewBase!!.visibility = SurfaceView.VISIBLE
        cameraBridgeViewBase!!.setCvCameraViewListener(this)

        //create camera listener callback
        baseLoaderCallback = object : BaseLoaderCallback(this) {
            override fun onManagerConnected(status: Int) {
                when (status) {
                    LoaderCallbackInterface.SUCCESS -> {
                        Log.v("aashari-log", "Loader interface success")
                        bwIMG = Mat()
                        dsIMG = Mat()
                        hsvIMG = Mat()
                        lrrIMG = Mat()
                        urrIMG = Mat()
                        usIMG = Mat()
                        cIMG = Mat()
                        hovIMG = Mat()
                        approxCurve = MatOfPoint2f()
                        cameraBridgeViewBase!!.enableView()
                    }
                    else -> super.onManagerConnected(status)
                }
            }
        }

    }

    override fun onCameraViewStarted(width: Int, height: Int) {

    }

    override fun onCameraViewStopped() {

    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame): Mat {

        val gray = inputFrame.gray()
        val dst = inputFrame.rgba()

        Imgproc.pyrDown(gray, dsIMG, Size((gray.cols() / 2).toDouble(), (gray.rows() / 2).toDouble()))
        Imgproc.pyrUp(dsIMG, usIMG, gray.size())

        Imgproc.Canny(usIMG, bwIMG, 0.0, threshold.toDouble())

        Imgproc.dilate(bwIMG, bwIMG, Mat(), Point(-1.0, 1.0), 1)

        val contours = ArrayList<MatOfPoint>()

        cIMG = bwIMG.clone()

        Imgproc.findContours(cIMG, contours, hovIMG, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)


        for (cnt in contours) {

            val curve = MatOfPoint2f(*cnt.toArray())

            Imgproc.approxPolyDP(curve, approxCurve, 0.02 * Imgproc.arcLength(curve, true), true)

            val numberVertices = approxCurve.total().toInt()

            val contourArea = Imgproc.contourArea(cnt)

            if (Math.abs(contourArea) < 100) {
                continue
            }

            //Rectangle detected
            if (numberVertices >= 4 && numberVertices <= 6) {

                val cos = ArrayList<Double>()

                for (j in 2 until numberVertices + 1) {
                    cos.add(angle(approxCurve.toArray()[j % numberVertices], approxCurve.toArray()[j - 2], approxCurve.toArray()[j - 1]))
                }

                Collections.sort(cos)

                val mincos = cos[0]
                val maxcos = cos[cos.size - 1]

                if (numberVertices == 4 && mincos >= -0.1 && maxcos <= 0.3) {
                    setLabel(dst, "X", cnt)
                }

            }


        }

        return dst

    }

    override fun onPause() {
        super.onPause()
        if (cameraBridgeViewBase != null) {
            cameraBridgeViewBase!!.disableView()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!OpenCVLoader.initDebug()) {
        //if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, baseLoaderCallback)) {
            Toast.makeText(applicationContext, "There is a problem", Toast.LENGTH_SHORT).show()
        } else {
            baseLoaderCallback.onManagerConnected(BaseLoaderCallback.SUCCESS)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (cameraBridgeViewBase != null) {
            cameraBridgeViewBase!!.disableView()
        }
    }

    private fun angle(pt1: Point, pt2: Point, pt0: Point): Double {
        val dx1 = pt1.x - pt0.x
        val dy1 = pt1.y - pt0.y
        val dx2 = pt2.x - pt0.x
        val dy2 = pt2.y - pt0.y
        return (dx1 * dx2 + dy1 * dy2) / Math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2) + 1e-10)
    }

    private fun setLabel(im: Mat, label: String, contour: MatOfPoint) {
        val fontface = Core.FONT_HERSHEY_SIMPLEX
        val scale = 3.0//0.4;
        val thickness = 3//1;
        val baseline = IntArray(1)
        val text = Imgproc.getTextSize(label, fontface, scale, thickness, baseline)
        val r = Imgproc.boundingRect(contour)
        val pt = Point(r.x + (r.width - text.width) / 2, r.y + (r.height + text.height) / 2)
        Imgproc.putText(im, label, pt, fontface, scale, Scalar(255.0, 0.0, 0.0), thickness)
    }

}