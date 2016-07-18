package com.yuan.house.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.avos.avoscloud.im.v2.AVIMMessage;
import com.avos.avoscloud.im.v2.AVIMReservedMessageType;
import com.avos.avoscloud.im.v2.AVIMTypedMessage;
import com.avos.avoscloud.im.v2.messages.AVIMAudioMessage;
import com.avos.avoscloud.im.v2.messages.AVIMImageMessage;
import com.avoscloud.leanchatlib.adapter.ChatMessageAdapter;
import com.avoscloud.leanchatlib.model.ConversationType;
import com.avoscloud.leanchatlib.view.PlayButton;
import com.dimo.utils.DateUtil;
import com.lfy.bean.Message;
import com.yuan.house.enumerate.ProposalMessageCategory;
import com.yuan.house.utils.ImageUtil;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by monst on 16/7/3.
 */
public class ProposalListAdapter extends ChatMessageAdapter {

    protected List<AVIMTypedMessage> datasProposal = new ArrayList<AVIMTypedMessage>();
    protected List<AVIMTypedMessage> datasSuggestion = new ArrayList<AVIMTypedMessage>();
    protected List<AVIMTypedMessage> datasBug = new ArrayList<AVIMTypedMessage>();


    public ProposalListAdapter(Context context, ConversationType conversationType, JSONObject object) {
        super(context, conversationType, object);
    }

    public void setCurrentDatas(ProposalMessageCategory type) {
        if (type == ProposalMessageCategory.COMPLAINT) {
            datas = datasProposal;
        } else if (type == ProposalMessageCategory.SUGGESTION) {
            datas = datasSuggestion;
        } else if (type == ProposalMessageCategory.BUG) {
            datas = datasBug;
        }
    }

    public void add(AVIMTypedMessage msg, ProposalMessageCategory type) {
        if (type == ProposalMessageCategory.COMPLAINT) {
            datasProposal.add(msg);
        } else if (type == ProposalMessageCategory.SUGGESTION) {
            datasSuggestion.add(msg);
        } else if (type == ProposalMessageCategory.BUG) {
            datasBug.add(msg);
        }
    }

    public void add2First(AVIMTypedMessage msg, ProposalMessageCategory type) {
            if (type == ProposalMessageCategory.COMPLAINT) {
                datasProposal.add(0, msg);
            } else if (type == ProposalMessageCategory.SUGGESTION) {
                datasSuggestion.add(0, msg);
            } else if (type == ProposalMessageCategory.BUG) {
                datasBug.add(0, msg);
            }
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

    public View getView(int position, View conView, ViewGroup parent) {
        AVIMMessage msg = datas.get(position);

        //if (conView == null)
        {
            Message bean = new Message();
            bean.setDate(DateUtil.getDate(msg.getTimestamp()));
            bean.setLeanId(conversationObject.optString("lean_id"));
            bean.setAuditType(conversationObject.optString("audit_type"));
            bean.setHouseId(conversationObject.optString("house_id"));
            bean.setIs_read(true);

            if (msg instanceof AVIMTypedMessage) {
                AVIMTypedMessage typedMessage = (AVIMTypedMessage) msg;

                boolean others = messageSentByOthers(typedMessage);
                conView = createViewByType(AVIMReservedMessageType.getAVIMReservedMessageType(typedMessage.getMessageType()), others);

                initReservedMessageView(conView, position, typedMessage, others, bean);
            }
        }

        setSendTimeView(conView, position, msg);
//        activity.registerForContextMenu(contentLayout);

        return conView;
    }
}
