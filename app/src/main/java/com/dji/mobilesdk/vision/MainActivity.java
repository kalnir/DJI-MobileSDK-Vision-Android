package com.dji.mobilesdk.vision;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.product.Model;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.flightcontroller.FlightController;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.aruco.Aruco;
import org.opencv.aruco.Dictionary;
import org.opencv.core.Mat;
import org.opencv.objdetect.CascadeClassifier;

public class MainActivity extends Activity implements TextureView.SurfaceTextureListener {
    private static final String TAG = "MainActivity";

    private static final int VIEW_MODE_DEFAULT = 0;
    private static final int VIEW_MODE_GRAY = 1;
    private static final int VIEW_MODE_CANNY = 2;
    private static final int VIEW_MODE_FACE_DETECTOR = 3;
    private static final int VIEW_MODE_LAPLACIAN = 4;
    private static final int VIEW_MODE_BLUR = 5;
    private static final int VIEW_MODE_ARUCO = 6;
    private static final int VIEW_MODE_AR = 7;
    private static final int VIEW_MODE_MOVE = 8;

    private MenuItem mItemPreviewRGBA;
    private MenuItem mItemPreviewGray;
    private MenuItem mItemPreviewCanny;
    private MenuItem mItemPreviewFaceDetector;
    private MenuItem mItemPreviewLaplacian;
    private MenuItem mItemPreviewBlur;
    private MenuItem mItemPreviewAruco;
    private MenuItem mItemPreviewAR;
    private MenuItem mItemPreviewMove;

    private int mode;
    private Dictionary dictionary;
    private CascadeClassifier faceDetector;
    private TextView isFlyingTextView;
    private TextureView videostreamPreview;
    private TextureView modifiedVideostreamPreview;
    protected VideoFeeder.VideoDataListener mReceivedVideoDataListener = null;
    // Codec for video live view
    protected DJICodecManager mCodecManager = null;

    private DroneHelper droneHelper;
    private OpenCVHelper openCVHelper;
    private ActionBar actionBar;

    private Canvas canvas;
    private Bitmap edgeBitmap;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS) {
                Log.i(TAG, "OpenCV loaded successfully");
                try {
                    // load cascade file from application resources
                    InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
                    File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                    File cascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
                    FileOutputStream os = new FileOutputStream(cascadeFile);

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                    is.close();
                    os.close();

                    faceDetector = new CascadeClassifier(cascadeFile.getAbsolutePath());
                    if (faceDetector.empty()) {
                        Log.e(TAG, "Failed to load cascade classifier");
                        showToast("Failed to load cascade classifier fo face detection");
                        faceDetector = null;
                    } else {
                        Log.i(TAG, "Loaded cascade classifier from " + cascadeFile.getAbsolutePath());
                    }
                    cascadeDir.delete();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                }
            } else {
                super.onManagerConnected(status);
            }
        }
    };

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);
        actionBar = getActionBar();
        initUI();
        droneHelper = new DroneHelper();
        openCVHelper = new OpenCVHelper(this);
        // The callback for receiving the raw H264 video data for camera live view
        mReceivedVideoDataListener = new VideoFeeder.VideoDataListener() {
            @Override
            public void onReceive(byte[] videoBuffer, int size) {
                if (mCodecManager != null) {
                    mCodecManager.sendDataToDecoder(videoBuffer, size);
                }
            }
        };
        initFlightController();
    }

    @Override
    public void onPause() {
        uninitPreviewer();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        initPreviewer();
    }

    public void onDestroy() {
        if (mCodecManager != null) {
            mCodecManager.cleanSurface();
            mCodecManager.destroyCodec();
            mCodecManager = null;
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        mItemPreviewRGBA = menu.add("Default");
        mItemPreviewGray = menu.add("Gray");
        mItemPreviewAruco = menu.add("Aruco");
        mItemPreviewAR = menu.add("AR");
        mItemPreviewMove = menu.add("Move");
        mItemPreviewLaplacian = menu.add("Edge");
        mItemPreviewBlur = menu.add("Blur");
        mItemPreviewCanny = menu.add("Edge(Canny)");
        mItemPreviewFaceDetector = menu.add("Face Detector");
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);

        if (item == mItemPreviewRGBA) {
            mode = VIEW_MODE_DEFAULT;
        } else if (item == mItemPreviewGray) {
            mode = VIEW_MODE_GRAY;
        } else if (item == mItemPreviewCanny) {
            mode = VIEW_MODE_CANNY;
        } else if (item == mItemPreviewFaceDetector) {
            mode = VIEW_MODE_FACE_DETECTOR;
        } else if (item == mItemPreviewLaplacian) {
            mode = VIEW_MODE_LAPLACIAN;
        } else if (item == mItemPreviewBlur) {
            mode = VIEW_MODE_BLUR;
        } else if (item == mItemPreviewAruco) {
            openCVHelper.startDetectAruco(droneHelper);
            mode = VIEW_MODE_ARUCO;
        } else if (item == mItemPreviewAR) {
            openCVHelper.startDoAR(droneHelper);
            mode = VIEW_MODE_AR;
        } else if (item == mItemPreviewMove) {
            openCVHelper.startDroneMove(droneHelper);
            mode = VIEW_MODE_MOVE;
        }

        return true;
    }

    private void initUI() {
        isFlyingTextView = findViewById(R.id.is_flying_text_view);
        videostreamPreview = findViewById(R.id.livestream_preview_ttv);
        modifiedVideostreamPreview = findViewById(R.id.modified_livestream_preview_ttv);
        if (videostreamPreview != null) {
            videostreamPreview.setSurfaceTextureListener(this);
        }
    }

    private void initPreviewer() {
        dictionary = Aruco.getPredefinedDictionary(Aruco.DICT_6X6_250);
        final BaseProduct product = DJIVisionApplication.getProductInstance();
        Log.d(TAG,
              "notifyStatusChange: " + (product == null
                                        ? "Disconnect"
                                        : (product.getModel() == null ? "null model" : product.getModel().name())));
        if (product != null && product.isConnected() && product.getModel() != null) {
            updateTitle(product.getModel().name() + " Connected ");
        } else {
            updateTitle("Disconnected");
        }

        if (null == product || !product.isConnected()) {
            showToast("Disconnected");
        } else {
            if (null != videostreamPreview) {
                videostreamPreview.setSurfaceTextureListener(this);
            }
            if (!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
                if (VideoFeeder.getInstance().getPrimaryVideoFeed() != null) {
                    VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(mReceivedVideoDataListener);
                }
            }
        }
    }

    private void uninitPreviewer() {
        Camera camera = DJIVisionApplication.getProductInstance().getCamera();
        if (camera != null) {
            // Reset the callback
            VideoFeeder.getInstance().getPrimaryVideoFeed().removeVideoDataListener(mReceivedVideoDataListener);
        }
    }

    private void showToast(final String message) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateTitle(String message) {
        if (actionBar != null) {
            actionBar.setTitle(message);
        }
    }

    public Mat processImage(Mat input) {
        Mat output;
        //Imgproc.resize(input, input, new Size(480, 360)); //Uncomment this to reduce processing time and make change in drawProcessedVideo() as instructed
        switch (mode) {
            default:
            case VIEW_MODE_DEFAULT:
                output = openCVHelper.defaultImageProcessing(input);
                break;
            case VIEW_MODE_GRAY:
                output = openCVHelper.convertToGray(input);
                break;
            case VIEW_MODE_CANNY:
                output = openCVHelper.detectEdgesUsingCanny(input);
                break;
            case VIEW_MODE_FACE_DETECTOR:
                output = openCVHelper.detectFaces(input, faceDetector);
                break;
            case VIEW_MODE_LAPLACIAN:
                output = openCVHelper.detectEdgesUsingLaplacian(input);
                break;
            case VIEW_MODE_BLUR:
                output = openCVHelper.blurImage(input);
                break;
            case VIEW_MODE_ARUCO:
                output = openCVHelper.detectArucoTags(input, dictionary, droneHelper);
                break;
            case VIEW_MODE_MOVE:
                openCVHelper.doDroneMoveUsingImage(input, droneHelper);
                output = openCVHelper.defaultImageProcessing(input);
                break;
            case VIEW_MODE_AR:
                output = openCVHelper.doAROnImage(input, dictionary, droneHelper);
                break;
        }

        return output;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (mCodecManager == null) {
            mCodecManager = new DJICodecManager(getApplicationContext(), surface, width, height);
        }
    }
    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        //Do nothing
    }
    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (mCodecManager != null) {
            mCodecManager.cleanSurface();
        }
        return false;
    }
    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        mCodecManager.getBitmap(new DJICodecManager.OnGetBitmapListener() {
            @Override
            public void onGetBitmap(Bitmap bitmap) {
                drawProcessedVideo(bitmap);
            }
        });
    }

    public void drawProcessedVideo(Bitmap bitmap) {
        if (bitmap != null) {
            Mat source = new Mat();
            Utils.bitmapToMat(bitmap, source);
            Mat processed = processImage(source);
            edgeBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
            //If you resize the original bitmap in the processImage() function, uncomment the following line and comment the one above this
            //edgeBitmap = Bitmap.createBitmap(480, 360, Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(processed, edgeBitmap);
            canvas = modifiedVideostreamPreview.lockCanvas();
            if (canvas != null) {
                canvas.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR);
                if (BuildConfig.DEBUG) {
                    canvas.drawBitmap(edgeBitmap,
                                      new Rect(0, 0, edgeBitmap.getWidth(), edgeBitmap.getHeight()),
                                      new Rect((canvas.getWidth() - edgeBitmap.getWidth()) / 2,
                                               (canvas.getHeight() - edgeBitmap.getHeight()) / 2,
                                               (canvas.getWidth() - edgeBitmap.getWidth()) / 2 + edgeBitmap.getWidth(),
                                               (canvas.getHeight() - edgeBitmap.getHeight()) / 2
                                                   + edgeBitmap.getHeight()),
                                      null);
                }

                modifiedVideostreamPreview.unlockCanvasAndPost(canvas);
                canvas.setBitmap(null);
                canvas = null;
                edgeBitmap.recycle();
                edgeBitmap = null;
            }
        }
    }

    public void onClickGimbalButton(View view) {
        droneHelper.toggleGimbal();
    }

    public void onClickTakeoffButton(View view) {
        droneHelper.takeoff();
    }

    public void onClickLandButton(View view) {
        droneHelper.land();
    }

    private void initFlightController() {
        FlightController flightController = droneHelper.fetchFlightController();
        if (flightController != null) {
            flightController.setStateCallback(new FlightControllerState.Callback() {
                @Override
                public void onUpdate(@NonNull FlightControllerState flightControllerState) {
                    setCurrentState(flightControllerState);
                }
            });
        }
    }

    public void setCurrentState(final FlightControllerState state) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(state.isFlying()) {
                    isFlyingTextView.setText(getResources().getString(R.string.flying));
                } else {
                    isFlyingTextView.setText(getResources().getString(R.string.landed));
                }

            }
        });
    }
}
