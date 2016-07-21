package com.yuan.house.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import com.dimo.utils.BitmapUtil;
import com.dimo.utils.StringUtil;
import com.edmodo.cropper.CropImageView;
import com.yuan.house.R;
import com.yuan.house.application.Injector;
import com.yuan.house.base.BaseFragmentActivity;
import com.yuan.house.common.Constants;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Alsor Zhou on 16/6/15.
 */

public class CropActivity extends BaseFragmentActivity {
    @BindView(R.id.cropImageView)
    CropImageView cropImageView;

    private String imagePath;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getIntent().getExtras();

        String imageName = null;
        int imageType = Constants.kActivityRequestCodeImagePickThenCropRectangle;

        if (bundle != null) {
            imageType = bundle.getInt(Constants.kBundleExtraCropImageType);
            imageName = bundle.getString(Constants.kBundleExtraCropImageName);
        }

        setContentView(R.layout.activity_crop, true);

        Injector.inject(this);
        ButterKnife.bind(this);

        setRightItem("保存", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                readyToCrop();

                Intent intent = new Intent();
                intent.putExtra("data", imagePath);
                setResult(RESULT_OK, intent);

                finish();
            }
        });

        setTitleItem(R.string.title_crop_photo);

        setLeftItem(R.drawable.btn_back, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        configureCropImageView(imageType, imageName);
    }

    private void configureCropImageView(int imageType, String imageName) {
        Bitmap bm = BitmapFactory.decodeFile(imageName);
        cropImageView.setImageBitmap(bm);

        imagePath = imageClippedPath();
        cropImageView.setFixedAspectRatio(true);

        int width, height;
        if (Constants.kActivityRequestCodeImagePickThenCropRectangle == imageType) {
            width = 3;
            height = 2;
        } else {
            width = 1;
            height = 1;
        }
        cropImageView.setAspectRatio(width, height);
    }
    private void readyToCrop() {

        Bitmap bm = cropImageView.getCroppedImage();
        Bitmap newBitmap = BitmapUtil.zoomImg(bm, 0.8f);
        BitmapUtil.saveBitmap(this, newBitmap, imagePath);
    }

    private String imageClippedPath() {
        return com.dimo.utils.FileUtil.getClipPath(this) + "/" + StringUtil.randomAlphabetString(5) + ".jpg";
    }
}
