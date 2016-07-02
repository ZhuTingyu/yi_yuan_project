package com.yuan.house.adapter;

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.avos.avoscloud.im.v2.AVIMReservedMessageType;
import com.avoscloud.leanchatlib.view.PlayButton;
import com.yuan.house.R;
import com.yuan.house.bean.ProposalInfo;
import com.yuan.house.enumerate.ProposalMediaType;
import com.yuan.house.enumerate.ProposalMessageCategory;
import com.yuan.house.ui.fragment.ProposalFragment;
import com.yuan.house.utils.ImageUtil;

import java.util.ArrayList;

/**
 * Created by monst on 16/6/29.
 */
public class ProposalAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public ArrayList<ProposalInfo> complainDatas = new ArrayList<>();
    public ArrayList<ProposalInfo> proposalDatas = new ArrayList<>();
    public ArrayList<ProposalInfo> bugDatas = new ArrayList<>();

    public ProposalAdapter() {
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            /*View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.chat_item_text, viewGroup,false);
            ViewHolder vh = new ViewHolder(view);
            return vh;*/

        int contentId = R.layout.chat_item_text;
        if (viewType == ProposalMediaType.TEXT.ordinal()) {
            contentId = R.layout.chat_item_text;
        } else if (viewType == ProposalMediaType.IMAGE.ordinal()) {
            contentId = R.layout.chat_item_image;
        } else if (viewType == ProposalMediaType.AUDIO.ordinal()) {
            contentId = R.layout.chat_item_audio;
        }

        View view = LayoutInflater.from(viewGroup.getContext()).inflate(contentId, viewGroup,false);
        RecyclerView.ViewHolder vh = null;

        if (viewType == ProposalMediaType.TEXT.ordinal()) {
            vh = new TextViewHolder(view);
        } else if (viewType == ProposalMediaType.IMAGE.ordinal()) {
            vh = new ImageViewHolder(view);
        } else if (viewType == ProposalMediaType.AUDIO.ordinal()) {
            vh = new VoiceViewHolder(view);
        }

        return vh;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        ArrayList<ProposalInfo> datas = getCurrentDatas();
        ProposalInfo info = datas.get(position);
        int msgType = info.msg_type;

        if (msgType == ProposalMediaType.TEXT.ordinal()) {
            ((TextViewHolder)viewHolder).mTextView.setText(info.content);
        } else if (msgType == ProposalMediaType.IMAGE.ordinal()) {
            ImageUtil.loadImageThumbnail(((ImageViewHolder)viewHolder).mImageView, info.content, 200);
        } else if (msgType == ProposalMediaType.AUDIO.ordinal()) {

        }
    }

    @Override
    public int getItemViewType(int position) {
        int msg_type = getCurrentDatas().get(position).msg_type;
        return msg_type;
    }

    @Override
    public int getItemCount() {
        return getCurrentDatas().size();
    }

    public class TextViewHolder extends RecyclerView.ViewHolder {
        public TextView mTextView;
        public TextViewHolder(View view){
            super(view);
            mTextView = (TextView) view.findViewById(R.id.textContent);
        }
    }

    public class ImageViewHolder extends RecyclerView.ViewHolder {
        public ImageView mImageView;
        public ImageViewHolder(View view){
            super(view);
            mImageView = (ImageView) view.findViewById(R.id.imageView);
        }
    }

    public class VoiceViewHolder extends RecyclerView.ViewHolder {
        public PlayButton mPlayBtnView;
        public VoiceViewHolder(View view){
            super(view);
            mPlayBtnView = (PlayButton) view.findViewById(R.id.playBtn);
        }
    }

    private ArrayList<ProposalInfo> getCurrentDatas() {
        switch (ProposalFragment.category) {
            case COMPLAINT:
                return complainDatas;
            case SUGGESTION:
                return proposalDatas;
            case BUG:
                return bugDatas;
        }
        return null;
    }

    public void addData(ProposalInfo data) {
        int category = data.category;
        if (category == ProposalMessageCategory.COMPLAINT.ordinal()) {
            complainDatas.add(data);
        } else if (category == ProposalMessageCategory.SUGGESTION.ordinal()) {
            proposalDatas.add(data);
        } else if (category == ProposalMessageCategory.BUG.ordinal()) {
            bugDatas.add(data);
        }
    }
}