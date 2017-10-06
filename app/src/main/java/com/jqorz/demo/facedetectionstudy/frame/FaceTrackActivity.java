package com.jqorz.demo.facedetectionstudy.frame;

import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.widget.TextView;

import com.faceplusplus.api.FaceDetecter;
import com.faceplusplus.api.FaceDetecter.Face;
import com.jqorz.demo.facedetectionstudy.R;
import com.jqorz.demo.facedetectionstudy.base.BaseActivity;
import com.jqorz.demo.facedetectionstudy.constant.Global;
import com.jqorz.demo.facedetectionstudy.widget.FaceMask;

import java.io.IOException;

import butterknife.BindView;

public class FaceTrackActivity extends BaseActivity implements Callback, PreviewCallback {
    @BindView(R.id.sv_preview)
    SurfaceView svPreview;
    @BindView(R.id.fm_mask)
    FaceMask fmMask;
    @BindView(R.id.tv_FaceInfo)
    TextView tv_FaceInfo;
    private Camera mCamera;
    private HandlerThread handleThread;
    private Handler detectHandler;
    private SurfaceHolder holder;
    private int width = 640;
    private int height = 480;
    private FaceDetecter facedetecter;

    @Override
    protected void init() {

        handleThread = new HandlerThread("dt");
        handleThread.start();
        detectHandler = new Handler(handleThread.getLooper());
        holder = svPreview.getHolder();
        holder.addCallback(this);
        svPreview.setKeepScreenOn(true);

        facedetecter = new FaceDetecter();
        facedetecter.init(this, Global.FACEPP_KEY);
        facedetecter.setHighAccuracy(true);//设置为高灵敏
        facedetecter.setTrackingMode(true);//设置为跟踪模式
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_face_track;
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            //设置参数 1表示前置摄像头
            mCamera = Camera.open(1);
            //摄像头画面显示在Surface上
            mCamera.setPreviewDisplay(holder);
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewSize(width, height);
            mCamera.setParameters(parameters);
        } catch (IOException e) {
            if (mCamera != null) mCamera.release();
            mCamera = null;
        }

        if (mCamera == null) {
            finish();
            return;
        }
        mCamera.startPreview();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        try {
            //摄像头画面显示在Surface上
            mCamera.setPreviewDisplay(holder);
            mCamera.setDisplayOrientation(90);
            mCamera.startPreview();
            mCamera.setPreviewCallback(this);
        } catch (IOException e) {
            if (mCamera != null) mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }


    @Override
    public void onPreviewFrame(final byte[] data, final Camera camera) {
        camera.setPreviewCallback(null);
        if (mCamera == null) return;
        detectHandler.post(new Runnable() {
            @Override
            public void run() {
                int is = 0;
                byte[] ori = new byte[width * height];
                for (int x = width - 1; x >= 0; x--) {
                    for (int y = height - 1; y >= 0; y--) {
                        ori[is++] = data[y * width + x];//将捕捉到的图像二维数组转为一维数组
                    }
                }
                //调用findFaces方法得到所有识别到的人脸
                final Face[] faceinfo = facedetecter.findFaces(ori, height, width);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        StringBuilder builder = new StringBuilder("trackingID=");
                        if (faceinfo != null) {
                            for (Face face : faceinfo) {
                                builder.append(face.trackingID).append("\n");
                                tv_FaceInfo.setText(builder.toString());
                            }
                        }
                        fmMask.setFaceInfo(faceinfo);
                    }
                });
                try {
                    camera.setPreviewCallback(FaceTrackActivity.this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        facedetecter.release(this);
        handleThread.quit();
        detectHandler = null;
    }

}
