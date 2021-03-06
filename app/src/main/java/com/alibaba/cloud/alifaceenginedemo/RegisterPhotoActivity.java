package com.alibaba.cloud.alifaceenginedemo;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.cloud.faceengine.Error;
import com.alibaba.cloud.faceengine.Face;
import com.alibaba.cloud.faceengine.FaceDetect;
import com.alibaba.cloud.faceengine.FaceRegister;
import com.alibaba.cloud.faceengine.Feature;
import com.alibaba.cloud.faceengine.Group;
import com.alibaba.cloud.faceengine.Image;
import com.alibaba.cloud.faceengine.ImageFormat;
import com.alibaba.cloud.faceengine.ImageRotation;
import com.alibaba.cloud.faceengine.Mode;
import com.alibaba.cloud.faceengine.Person;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class RegisterPhotoActivity extends Activity {
    private static final String TAG = "AFE_" + RegisterPhotoActivity.class.getSimpleName();

    private EditText mPersonNameCtrl, mFeatureName;
    private Spinner spinner;
    private ImageView mPhotoCtrl;
    private Button mBtnAdd;
    private FaceRegister mFaceRegister;
    private Group[] mGroups;
    private List<String> mGroupNames;
    private TextView mTitle;
    private static final int ALBUM_OK = 0;
    private Bitmap mPicture;
    private FaceDetect mFaceDetect;
    private ImageButton mBtnBack;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registeredphoto);
        initData();
        init();
        initView();
    }

    private void initView() {
        mTitle.setText(R.string.register_photo);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(RegisterPhotoActivity.this, R.layout.support_simple_spinner_dropdown_item, mGroupNames);
        spinner.setAdapter(adapter);

        mPhotoCtrl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentIv = new Intent(Intent.ACTION_PICK, null);
                intentIv.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                startActivityForResult(intentIv, ALBUM_OK);
            }
        });

        mBtnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mGroups == null || mGroups.length == 0) {
                    Toast.makeText(RegisterPhotoActivity.this, RegisterPhotoActivity.this.getString(R.string.please_create_group_first), Toast.LENGTH_SHORT).show();
                    return;
                }
                String personName = mPersonNameCtrl.getText().toString().trim();
                String featureName = mFeatureName.getText().toString().trim();

                if (personName.equals("") || personName == null || featureName.equals("") || featureName == null) {
                    Toast.makeText(RegisterPhotoActivity.this, RegisterPhotoActivity.this.getString(R.string.please_fill), Toast.LENGTH_SHORT).show();
                    return;
                }

                Person person = new Person();
                person.name = personName;
                String groupId = mGroups[spinner.getSelectedItemPosition()].id;

                int status = mFaceRegister.addPerson(groupId, person);
                if (status != Error.OK && status != Error.ERROR_EXISTED) {
                    Toast.makeText(RegisterPhotoActivity.this,
                            RegisterPhotoActivity.this.getString(R.string.add_failure) + status,
                            Toast.LENGTH_LONG).show();
                    return;
                }

                Image image = new Image();
                image.data = Utils.bitmap2RGB(mPicture);
                image.format = ImageFormat.RGB888;
                image.rotation = ImageRotation.ANGLE_0;
                image.height = mPicture.getHeight();
                image.width = mPicture.getWidth();

                Face[] faces = mFaceDetect.detectPicture(image);
                if (faces == null || faces.length == 0) {
                    Toast.makeText(RegisterPhotoActivity.this,
                            RegisterPhotoActivity.this.getString(R.string.no_detected),
                            Toast.LENGTH_LONG).show();
                    return;
                }

                String featureStr = mFaceRegister.extractFeature(image, faces[0], mGroups[spinner.getSelectedItemPosition()].modelType);
                if (featureStr == null || featureStr.length() == 0) {
                    Toast.makeText(RegisterPhotoActivity.this,
                            RegisterPhotoActivity.this.getString(R.string.extract_feature) +
                                    RegisterPhotoActivity.this.getString(R.string.fail),
                            Toast.LENGTH_LONG).show();
                    return;
                }

                Feature feature = new Feature();
                feature.name = featureName;
                feature.feature = featureStr;

                int result = mFaceRegister.addFeature(person.id, feature);
                if (result == Error.OK) {
                    Toast.makeText(RegisterPhotoActivity.this, RegisterPhotoActivity.this.getString(R.string.add_success), Toast.LENGTH_LONG).show();
                    RegisterPhotoActivity.this.finish();
                } else {
                    Toast.makeText(RegisterPhotoActivity.this, RegisterPhotoActivity.this.getString(R.string.add_failure) + result, Toast.LENGTH_LONG).show();
                }
            }
        });

        mBtnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RegisterPhotoActivity.this.finish();
            }
        });
    }

    private void initData() {
        mFaceRegister = FaceRegister.createInstance();
        mFaceDetect = FaceDetect.createInstance(Mode.TERMINAL);
        mGroups = mFaceRegister.getAllGroups();
        mGroupNames = new ArrayList<String>();
        if (mGroups != null) {
            for (int i = 0; i < mGroups.length; i++) {
                mGroupNames.add(mGroups[i].name);
            }
        }
    }

    private void init() {
        mFeatureName = (EditText) findViewById(R.id.feature_name);
        mPersonNameCtrl = (EditText) findViewById(R.id.person_name);
        spinner = (Spinner) findViewById(R.id.activity_registeredphoto_sp);
        mPhotoCtrl = (ImageView) findViewById(R.id.activity_registeredphoto_photo);
        mBtnAdd = (Button) findViewById(R.id.activity_registeredphoto_confirm);
        mTitle = (TextView) findViewById(R.id.currency_tv_title);
        mBtnBack = (ImageButton) findViewById(R.id.currency_btn_back);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (ALBUM_OK == requestCode) {
            ContentResolver cr = this.getContentResolver();
            if (data != null) {
                Uri uri = data.getData();
                String path = Utils.getFilePathByUri(RegisterPhotoActivity.this, uri);
                float degree = Utils.readPictureDegree(path);
                try {
                    mPicture = BitmapFactory.decodeStream(cr.openInputStream(uri));
                    if (degree != 0) {
                        // 旋转图片
                        Matrix m = new Matrix();
                        m.postRotate(degree);
                        mPicture = Bitmap.createBitmap(mPicture, 0, 0, mPicture.getWidth(),
                                mPicture.getHeight(), m, true);
                    }
                    mPhotoCtrl.setImageBitmap(mPicture);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
