package com.yuan.house.adapter;

import android.content.Context;
import android.widget.ImageView;
import android.widget.TextView;

import com.avos.avoscloud.im.v2.AVIMTypedMessage;
import com.avos.avoscloud.im.v2.messages.AVIMAudioMessage;
import com.avos.avoscloud.im.v2.messages.AVIMImageMessage;
import com.avoscloud.leanchatlib.adapter.ChatNewMessageAdapter;
import com.avoscloud.leanchatlib.model.ConversationType;
import com.avoscloud.leanchatlib.view.PlayButton;
import com.yuan.house.utils.ImageUtil;

import org.json.JSONObject;

/**
 * Created by monst on 16/7/3.
 */
public class ProposalListAdapter extends ChatNewMessageAdapter {
    public ProposalListAdapter(Context context, ConversationType conversationType, JSONObject object) {
        super(context, conversationType, object);
    }

    @Override
    protected void initImageView(ImageView imageView, AVIMImageMessage imageMsg) {
        ImageUtil.loadImageThumbnail(imageView, imageMsg.getText(), 200);
        setImageOnClickListener(imageView, imageMsg);
    }

    @Override
    protected void initPlayBtn(AVIMTypedMessage msg, PlayButton playBtn, AVIMAudioMessage audioMessage, TextView timeAudio) {
        super.initPlayBtn(msg, playBtn, audioMessage, timeAudio);
        playBtn.setPath(audioMessage.getLocalFilePath());
    }
}
