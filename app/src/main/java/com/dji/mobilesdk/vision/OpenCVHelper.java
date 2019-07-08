package com.dji.mobilesdk.vision;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import java.util.ArrayList;
import java.util.List;
import org.opencv.android.Utils;
import org.opencv.aruco.Aruco;
import org.opencv.aruco.Dictionary;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import static org.opencv.core.Core.BORDER_DEFAULT;
import static org.opencv.core.Core.bitwise_not;
import static org.opencv.core.Core.extractChannel;

public class OpenCVHelper {
    private static final Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);
    private Context context;
    private MatOfPoint3f objPoints;
    private Mat intrinsic;
    private MatOfDouble distortion;
    private Mat logoImg;
    private MatOfPoint3f objectPoints;

    public OpenCVHelper(Context context) {
        this.context = context;
    }

    public Mat defaultImageProcessing(Mat input) {
        Imgproc.putText(input, "Default", new Point(150, 40), 1, 4, new Scalar(255, 255, 255), 2, 8, false);
        return input;
    }

    public Mat convertToGray(Mat input) {
        Mat output = new Mat();
        Imgproc.cvtColor(input, output, Imgproc.COLOR_RGBA2GRAY);
        return output;
    }

    public Mat detectEdgesUsingCanny(Mat input) {
        Mat output = new Mat();
        Imgproc.Canny(input, output, 80, 100);
        return output;
    }

    public Mat detectEdgesUsingLaplacian(Mat input) {
        Mat grayImg = new Mat();
        Mat intermediateMat = new Mat();
        Mat output = new Mat();
        Imgproc.cvtColor(input, grayImg, Imgproc.COLOR_RGBA2GRAY);
        Imgproc.GaussianBlur(grayImg, grayImg, new Size(3, 3), 0, 0);
        Imgproc.Laplacian(grayImg, intermediateMat, CvType.CV_8U, 3, 1, 0, BORDER_DEFAULT);
        Core.convertScaleAbs(intermediateMat, intermediateMat, 10, 0);
        Imgproc.cvtColor(intermediateMat, output, Imgproc.COLOR_GRAY2RGBA, 4);
        grayImg.release();
        intermediateMat.release();
        return output;
    }

    public Mat blurImage(Mat input) {
        Mat grayMat = new Mat();
        Mat output = new Mat();
        Imgproc.cvtColor(input, grayMat, Imgproc.COLOR_RGBA2GRAY);
        Imgproc.GaussianBlur(grayMat, output, new Size(35, 35), 0, 0);
        grayMat.release();
        return output;
    }

    public Mat detectFaces(Mat input, CascadeClassifier faceDetector) {
        Mat grayImgMat = new Mat();
        MatOfRect faces = new MatOfRect();
        Mat output;
        Imgproc.cvtColor(input, grayImgMat, Imgproc.COLOR_RGBA2GRAY);
        if (faceDetector != null) {
            faceDetector.detectMultiScale(grayImgMat, faces, 1.1, 2, 2, new Size(60, 60), new Size());
        }
        output = input;
        Rect[] facesArray = faces.toArray();
        for (Rect rect : facesArray) {
            Imgproc.rectangle(output, rect.tl(), rect.br(), FACE_RECT_COLOR, 3);
        }
        return output;
    }

    public Mat detectArucoTags(Mat input, Dictionary dictionary, DroneHelper droneHelper) {
        Mat grayImgMat = new Mat();
        Mat intermediateImgMat = new Mat();
        Mat output = new Mat();
        Imgproc.cvtColor(input, grayImgMat, Imgproc.COLOR_RGBA2GRAY);
        Imgproc.cvtColor(input, intermediateImgMat, Imgproc.COLOR_RGBA2RGB);
        Mat ids = new Mat();
        List<Mat> corners = new ArrayList<>();
        Aruco.detectMarkers(grayImgMat, dictionary, corners, ids);
        if (ids.depth() > 0) {
            Aruco.drawDetectedMarkers(intermediateImgMat, corners, ids, new Scalar(255, 0, 255));
            moveOnArucoDetected(ids, corners, droneHelper, intermediateImgMat.width(), intermediateImgMat.height());
        }
        Imgproc.cvtColor(intermediateImgMat, output, Imgproc.COLOR_RGB2BGR);
        Imgproc.cvtColor(output, output, Imgproc.COLOR_BGR2RGBA, 4);
        grayImgMat.release();
        intermediateImgMat.release();
        return output;
    }

    private void moveOnArucoDetected(Mat ids,
                                     List<Mat> corners,
                                     DroneHelper droneHelper,
                                     int imageWidth,
                                     int imageHeight) {
        //TODO
        // Implement your logic to decide where to move the drone
        // Below snippet is an example of how you can calculate the center of the marker
        /*Scalar markerCenter = new Scalar(0, 0);
        for (Mat corner : corners) {
            markerCenter = Core.mean(corner);
        }*/

        // Codes commented below show how to drive the drone to move to the direction
        // such that desired tag is in the center of image frame

        // Calculate the image vector relative to the center of the image
        //Scalar imageVector = new Scalar(markerCenter.val[0] - imageWidth / 2f, markerCenter.val[0] - imageHeight / 2f);

        // Convert vector from image coordinate to drone navigation coordinate
        //Scalar motionVector = convertImageVectorToMotionVector(imageVector);

        // If there's no tag detected, no motion required
        /*if (ids.size().empty()) {
            motionVector = new Scalar(0, 0);
        }*/

        // Use MoveVxVyYawrateVz(...) or MoveVxVyYawrateHeight(...)
        // depending on the mode you choose at the beginning of this function

        /*Log.d("OpenCVHelper", "Moving By: " + motionVector.toString());
        if ((imageVector.val[0] * imageVector.val[0] + imageVector.val[1] * imageVector.val[1]) < 900) {
            droneHelper.moveVxVyYawrateVz((float) motionVector.val[0], (float) motionVector.val[1], 0f, -0.2f);
        } else {
            droneHelper.moveVxVyYawrateVz((float) motionVector.val[0], (float) motionVector.val[1], 0f, 0f);
        }*/

        // Sample functions to help you control the drone such as takeoff and land
        //droneHelper.takeoff();
        //droneHelper.land();

    }

    public void doDroneMoveUsingImage(Mat input, DroneHelper droneHelper) {
        /*
         * Remember this function is called every time
         * a frame is available. So don't do long loop here.
         */
        Imgproc.cvtColor(input, input, Imgproc.COLOR_BGR2YUV);
        extractChannel(input, input, 0);
        FlightControlData controlData =
            new FlightControlData(0.1f, 0.0f, 0.0f, 0.0f); //pitch, roll, yaw, verticalThrottle
        droneHelper.sendMovementCommand(controlData);
    }

    public Mat doAROnImage(Mat input, Dictionary dictionary, DroneHelper droneHelper) {
        //TODO:
        // Since this is the bonus part, only high-level instructions will be provided
        // One way you can do this is to:
        // 1. Identify the Aruco tags with corner pixel location
        //    Hint:Aruco.detectMarkers(...)
        // 2. For each corner in 3D space, define their 3D locations
        //    The 3D locations you defined here will determine the origin of your coordinate frame
        // 3. Given the 3D locations you defined, their 2D pixel location in the image, and camera parameters
        //    You can calculate the 6 DOF of the camera relative to the tag coordinate frame
        //    Hint: Calib3d.solvePnP(...)
        // 4. To put artificial object in the image, you need to create 3D points first and project them into 2D image
        //    With the projected image points, you can draw lines or polygon
        //    Hint: Calib3d.projectPoints(...);
        // 5. To put dji image on a certain location,
        //    you need find the homography between the projected 4 corners and the 4 corners of the logo image
        //    Hint: Calib3d.findHomography(...);
        // 6. Once the homography is found, warp the image with perspective
        //    Hint: Imgproc.warpPerspective(...);
        // 7. Now you have the warped logo image in the right location, just overlay them on top of the camera image
        Mat output = new Mat();
        Mat grayMat = convertToGray(input);

        //TODO: Do your magic!!!
        // Hint how to overlay warped logo onto the original camera image
        /*
            Imgproc.cvtColor(logoWarped, grayMat, Imgproc.COLOR_BGR2GRAY);
            Imgproc.threshold(grayMat, grayMat, 0, 255, Imgproc.THRESH_BINARY);
            bitwise_not(grayMat, grayInv);
            input.copyTo(src1Final, grayInv);
            logoWarped.copyTo(src2Final, grayMat);
            Core.add(src1Final, src2Final, output);
         */
        //end magic

        if (output.empty()) {
            output = grayMat;
        }

        return output;
    }

    public void startDetectAruco(DroneHelper droneHelper) {
        // Virtual stick mode is a control interface
        // allows user to programmatically control the drone's movement
        droneHelper.enterVirtualStickMode();

        // This will change the behavior in the z-axis of the drone
        // If you call change set vertical mode to absolute height
        // Use moveVxVyYawrateHeight(...)
        // Otherwise use moveVxVyYawrateVz(...)
        droneHelper.setVerticalModeToAbsoluteHeight();

        // Move the camera to look down so you can see the tags if needed
        droneHelper.setGimbalPitchDegree(-75.0f);
    }

    public void startDroneMove(DroneHelper droneHelper) {
        droneHelper.enterVirtualStickMode();
        droneHelper.setVerticalModeToVelocity();
    }

    public void startDoAR(DroneHelper droneHelper) {
        droneHelper.enterVirtualStickMode();
        droneHelper.setVerticalModeToAbsoluteHeight();
        Bitmap bMap = BitmapFactory.decodeResource(context.getResources(), R.drawable.dji_logo);
        logoImg = new Mat();
        Utils.bitmapToMat(bMap, logoImg);

        //Camera calibration code
        intrinsic = new Mat(3, 3, CvType.CV_32F);
        intrinsic.put(0,
                      0,
                      1.2702029303551683e+03,
                      0.,
                      7.0369652952332717e+02,
                      0.,
                      1.2682183239938338e+03,
                      3.1342369745005681e+02,
                      0.,
                      0.,
                      1.);

        distortion = new MatOfDouble(3.2177759275048554e-02,
                                     1.1688831035623757e+00,
                                     -1.6742357543049650e-02,
                                     1.4173384809091350e-02,
                                     -6.1914718831876847e+00);

        // Please measure the marker size in Meter and enter it here
        double markerSizeMeters = 0.13;
        double halfMarkerSize = markerSizeMeters * 0.5;

        // Self-defined tag location in 3D, this is used in step 2 in doAR
        objPoints = new MatOfPoint3f();
        List<Point3> point3List = new ArrayList<>();
        point3List.add(new Point3(-halfMarkerSize, -halfMarkerSize, 0));
        point3List.add(new Point3(-halfMarkerSize, halfMarkerSize, 0));
        point3List.add(new Point3(halfMarkerSize, halfMarkerSize, 0));
        point3List.add(new Point3(halfMarkerSize, -halfMarkerSize, 0));
        objPoints.fromList(point3List);

        // AR object points in 3D, this is used in step 4 in doAR
        objectPoints = new MatOfPoint3f();

        List<Point3> point3DList = new ArrayList<>();
        point3DList.add(new Point3(-halfMarkerSize, -halfMarkerSize, 0));
        point3DList.add(new Point3(-halfMarkerSize, halfMarkerSize, 0));
        point3DList.add(new Point3(halfMarkerSize, halfMarkerSize, 0));
        point3DList.add(new Point3(halfMarkerSize, -halfMarkerSize, 0));
        point3DList.add(new Point3(-halfMarkerSize, -halfMarkerSize, markerSizeMeters));
        point3DList.add(new Point3(-halfMarkerSize, halfMarkerSize, markerSizeMeters));
        point3DList.add(new Point3(halfMarkerSize, halfMarkerSize, markerSizeMeters));
        point3DList.add(new Point3(halfMarkerSize, -halfMarkerSize, markerSizeMeters));

        objectPoints.fromList(point3DList);
    }

    private Scalar convertImageVectorToMotionVector(Scalar imageVector) {
        double pX = -imageVector.val[1];
        double pY = imageVector.val[0];
        double divisor = Math.sqrt((pX * pX) + (pY * pY));
        pX = pX / divisor;
        pY = pY / divisor;

        return new Scalar(pX * 0.2, pY * 0.2);
    }
}
