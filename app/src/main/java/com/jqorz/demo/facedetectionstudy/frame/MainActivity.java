package com.jqorz.demo.facedetectionstudy.frame;

import android.Manifest;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.widget.Button;

import com.jqorz.demo.facedetectionstudy.R;
import com.jqorz.demo.facedetectionstudy.base.BaseActivity;
import com.jqorz.demo.facedetectionstudy.util.ToastUtil;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.PermissionListener;
import com.yanzhenjie.permission.Rationale;
import com.yanzhenjie.permission.RationaleListener;

import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

public class MainActivity extends BaseActivity {

    @BindView(R.id.btn_FaceDetection)
    Button btn_FaceDetection;

    @Override
    protected void init() {

    }

    @OnClick({R.id.btn_FaceDetection, R.id.btn_FaceTrack})
    public void onClick(Button btn) {
        Class[] classes = {FaceDetectionActivity.class, FaceTrackActivity.class};
        switch (btn.getId()) {
            case R.id.btn_FaceDetection:
                getPermission(classes[0]);
                break;
            case R.id.btn_FaceTrack:
                getPermission(classes[1]);
                break;
        }
    }

    private void getPermission(final Class cls) {
        AndPermission.with(this).permission(Manifest.permission.CAMERA)
                .requestCode(1001)
                .callback(new PermissionListener() {
                    @Override
                    public void onSucceed(int requestCode, @NonNull List<String> grantPermissions) {

                        startActivity(new Intent(MainActivity.this, cls));
                    }

                    @Override
                    public void onFailed(int requestCode, @NonNull List<String> deniedPermissions) {
                        ToastUtil.showToast(MainActivity.this, "相机权限获取失败");
                    }
                })
                .rationale(new RationaleListener() {
                               @Override
                               public void showRequestPermissionRationale(int requestCode, Rationale rationale) {
                                   AndPermission.rationaleDialog(MainActivity.this, rationale).show();
                               }
                           }
                ).start();
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_main;
    }
}
