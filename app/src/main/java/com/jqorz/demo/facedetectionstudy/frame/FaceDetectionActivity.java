package com.jqorz.demo.facedetectionstudy.frame;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;
import com.jqorz.demo.facedetectionstudy.R;
import com.jqorz.demo.facedetectionstudy.base.BaseActivity;
import com.jqorz.demo.facedetectionstudy.constant.Global;
import com.jqorz.demo.facedetectionstudy.util.BitmapUtil;
import com.jqorz.demo.facedetectionstudy.util.ToastUtil;
import com.jqorz.demo.facedetectionstudy.util.UserDataUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnLongClick;


public class FaceDetectionActivity extends BaseActivity implements Callback, PreviewCallback {
    private final String FACE_ID = "FACE_ID";
    private final String IMAGE_ID = "IMAGE_ID";
    private final String PERSON_ID = "PERSON_ID";
    private final int width = 640;
    private final int height = 480;
    @BindView(R.id.sv_preview)
    SurfaceView svPreview;
    @BindView(R.id.tv_FaceInfo)
    TextView tv_FaceInfo;
    @BindView(R.id.iv_CatchPic)
    ImageView ivImage;
    private Camera mCamera;
    private boolean flag = true;//是否为基准。true-用作基准,false-该图像用于和基准进行对比
    //点击拍照后的回调得到的图片
    private Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);//将捕获到的图像字节数组转为bitmap
            Bitmap rotateBitmap = BitmapUtil.getRotateBitmap(bitmap, -90);//图像旋转90度

            ivImage.setImageBitmap(rotateBitmap);
            camera.startPreview();
            dealWithFaceDetect(BitmapUtil.bitmap2Bytes(rotateBitmap));
        }
    };


    @Override
    protected void init() {
        svPreview.getHolder().addCallback(this);
        svPreview.setKeepScreenOn(true);//保持屏幕常亮
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_face_detect;
    }


    private void dealWithFaceDetect(final byte[] data) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpRequests httpRequests = new HttpRequests(Global.FACEPP_KEY, Global.FACEPP_SECRET, true, true);

                    PostParameters postParameters = new PostParameters();
                    postParameters.setImg(data);

                    if (!flag) {
                        //------ 检测给定图片(Image)中的所有人脸(Face)的位置和相应的面部属性 ------//
                        JSONObject detectionJson = httpRequests.detectionDetect(postParameters);
                        Log.e("---> ", "detectionDetectJson: " + detectionJson.toString());

                        setInfo("detectionDetectJson: ",detectionJson);

                        JSONArray faces = detectionJson.getJSONArray("face");
                        String face_id = faces.optJSONObject(0).getString("face_id");
                        String img_id = detectionJson.getString("img_id");
                        String session_id = detectionJson.getString("session_id");

                        Log.e("---> ", "face_id: " + face_id);
                        Log.e("---> ", "img_id: " + img_id);
                        Log.e("---> ", "session_id: " + session_id);


                        //------ 获取session相关状态和结果 ------//
                        JSONObject sessionJson = httpRequests.getSessionSync(session_id);
                        Log.e("----> ", "status: " + sessionJson.getString("status"));

                        setInfo("status: ",sessionJson);

                        postParameters.setFaceId(face_id);
                        postParameters.setPersonId(UserDataUtil.loadUserData(FaceDetectionActivity.this, PERSON_ID));
                        JSONObject recognitionJson = httpRequests.recognitionVerify(postParameters);
                        Log.e("---> ", "recognitionJson: " + recognitionJson.toString());

                        setInfo("recognitionJson: ",recognitionJson);

                        final boolean isSamePerson = recognitionJson.getBoolean("is_same_person");

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (isSamePerson) {
                                    Toast.makeText(FaceDetectionActivity.this, "验证成功：相同", Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(FaceDetectionActivity.this, "验证成功：不相同", Toast.LENGTH_LONG).show();
                                }
                            }
                        });
                    } else {
                        //------ 检测给定图片(Image)中的所有人脸(Face)的位置和相应的面部属性 ------//
                        JSONObject detectionJson = httpRequests.detectionDetect(postParameters);
                        Log.e("---> ", "detectionDetectJson: " + detectionJson.toString());

                        setInfo("detectionDetectJson: ",detectionJson);

                        JSONArray faces = detectionJson.getJSONArray("face");
                        String face_id = faces.optJSONObject(0).getString("face_id");
                        String img_id = detectionJson.getString("img_id");
                        String session_id = detectionJson.getString("session_id");

                        Log.e("---> ", "face_id: " + face_id);
                        Log.e("---> ", "img_id: " + img_id);
                        Log.e("---> ", "session_id: " + session_id);


                        //------ 获取session相关状态和结果 ------//
                        JSONObject sessionJson = httpRequests.getSessionSync(session_id);
                        Log.e("----> ", "status: " + sessionJson.getString("status"));

                        setInfo("status: ",sessionJson);

                        //------ 创建一个Person ------//
                        PostParameters paramater1 = new PostParameters();
                        paramater1.setPersonName("Person name (" + face_id + ")");
                        paramater1.setTag("Person tag (" + face_id + ")");
                        paramater1.setFaceId(face_id);
                        JSONObject createJson = httpRequests.personCreate(paramater1);
                        Log.e("----> ", "createJson: " + createJson.toString());

                        setInfo("createJson: ",createJson);

                        String person_id = createJson.getString("person_id");
                        Log.e("----> ", "person_id: " + person_id);


                        //------ 针对verify功能对一个person进行训练 ------//
                        PostParameters paramater2 = new PostParameters();
                        paramater2.setPersonId(person_id);
                        JSONObject trainJson = httpRequests.trainVerify(paramater2);
                        Log.e("----> ", "trainJson: " + trainJson.toString());

                        setInfo("trainJson: ",trainJson);

                        //------ 获取session相关状态和结果 ------//
                        JSONObject sessionJson1 = httpRequests.getSessionSync(trainJson.getString("session_id"));
                        Log.e("----> ", "status: " + sessionJson1.getString("status"));
                        final String status = sessionJson1.getString("status");

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (status.equals("SUCC")) {
                                    Toast.makeText(FaceDetectionActivity.this, "扫描人脸成功", Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(FaceDetectionActivity.this, "扫描人脸失败", Toast.LENGTH_LONG).show();
                                }
                            }
                        });
                        UserDataUtil.updateUserData(FaceDetectionActivity.this, FACE_ID, face_id);
                        UserDataUtil.updateUserData(FaceDetectionActivity.this, IMAGE_ID, img_id);
                        UserDataUtil.updateUserData(FaceDetectionActivity.this, PERSON_ID, person_id);
                    }
                    flag = !flag;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void setInfo(final String tag, final JSONObject text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tv_FaceInfo.setText(new StringBuilder(tv_FaceInfo.getText())
                        .append("\n\n").append(tag).append(text.toString()));
            }
        });

    }

    @OnClick({R.id.iv_CatchPic})
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_CatchPic:
                ToastUtil.showToast(this, "捕获~");
                mCamera.takePicture(null, null, pictureCallback);
                break;
        }
    }

    @OnLongClick({R.id.iv_CatchPic})
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case R.id.iv_CatchPic:
                ToastUtil.showToast(this, "清空~");
                tv_FaceInfo.setText("");
                break;
        }
        return true;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mCamera = Camera.open(1);
        Camera.Parameters para = mCamera.getParameters();
        para.setPreviewSize(width, height);
        mCamera.setParameters(para);
        mCamera.setDisplayOrientation(90);

    }


    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
            mCamera.setPreviewCallback(this);
        } catch (IOException e) {
            e.printStackTrace();
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

    }


}
