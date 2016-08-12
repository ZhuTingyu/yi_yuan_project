package com.avoscloud.leanchatlib.controller;

import com.avos.avoscloud.AVGeoPoint;
import com.avos.avoscloud.im.v2.AVIMConversation;
import com.avos.avoscloud.im.v2.AVIMException;
import com.avos.avoscloud.im.v2.AVIMTypedMessage;
import com.avos.avoscloud.im.v2.callback.AVIMConversationCallback;
import com.avos.avoscloud.im.v2.messages.AVIMAudioMessage;
import com.avos.avoscloud.im.v2.messages.AVIMImageMessage;
import com.avos.avoscloud.im.v2.messages.AVIMLocationMessage;
import com.avoscloud.leanchatlib.db.MsgsTable;
import com.avoscloud.leanchatlib.utils.Logger;
import com.avoscloud.leanchatlib.utils.PhotoUtils;
import com.avoscloud.leanchatlib.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import timber.log.Timber;

/**
 * Created by lzw on 14/11/23.
 */
public class MessageAgent {
    private AVIMConversation conv;
    private MsgsTable msgsTable;
    private ChatManager chatManager;
    private SendCallback sendCallback;

    public MessageAgent(AVIMConversation conv) {
        this.conv = conv;
        msgsTable = MsgsTable.getCurrentUserInstance();
        chatManager = ChatManager.getInstance();
    }

    public void setSendCallback(SendCallback sendCallback) {
        this.sendCallback = sendCallback;
    }

    public void sendPresence(final AVIMTypedMessage msg) {
        if (conv != null && msg != null) {
            conv.sendMessage(msg, AVIMConversation.TRANSIENT_MESSAGE_FLAG, null);
        }
    }

    private void sendMsg(final AVIMTypedMessage msg, final String originPath, final SendCallback callback) {
        if (!chatManager.isConnect()) {
            Logger.d("im not connect");
        }

        if (conv == null) {
            Timber.e("converation find null");
            return;
        }

        conv.sendMessage(msg, AVIMConversation.RECEIPT_MESSAGE_FLAG, new AVIMConversationCallback() {
            @Override
            public void done(AVIMException e) {
                if (e != null) {
                    e.printStackTrace();
                    msg.setMessageId(Utils.uuid());
                    msg.setTimestamp(System.currentTimeMillis());
                }

                msgsTable.insertMsg(msg);

                // FIXME: 16/6/24 hack, conv.members 第一个位置是发送者,最后一个位置是接收者
                JSONObject object = new JSONObject();
                try {
                    if (MessageHelper.fromMe(msg)) {
                        object.put("lean_id", conv.getMembers().get(conv.getMembers().size() - 1));
                    } else {
                        object.put("lean_id", conv.getMembers().get(0));
                    }
                } catch (JSONException e1) {
                    e1.printStackTrace();
                }

                // TODO: 16/7/20 slow operation
                ChatManager.getInstance().storeLastMessage(msg, object);

                if (e == null && originPath != null) {
                    File tmpFile = new File(originPath);
                    File newFile = new File(com.avoscloud.leanchatlib.utils.PathUtils.getChatFilePath(msg.getMessageId()));
                    boolean result = tmpFile.renameTo(newFile);
                    if (!result) {
                        throw new IllegalStateException("move file failed, can't use local cache");
                    }
                }
                if (callback != null) {
                    if (e != null) {
                        callback.onError(e);
                    } else {
                        callback.onSuccess(msg);
                    }
                }
            }
        });
    }

    public void resendMsg(final AVIMTypedMessage msg, final SendCallback sendCallback) {
        final String tmpId = msg.getMessageId();
        conv.sendMessage(msg, AVIMConversation.RECEIPT_MESSAGE_FLAG, new AVIMConversationCallback() {
            @Override
            public void done(AVIMException e) {
                if (e != null) {
                    sendCallback.onError(e);
                } else {
                    msgsTable.updateFailedMsg(msg, tmpId);
                    sendCallback.onSuccess(msg);
                }
            }
        });
    }

    /**
     * Send Encapsulated AVIMTypedMessage message
     *
     * @param msg AVIMTypedMessage
     */
    public void sendEncapsulatedTypedMessage(AVIMTypedMessage msg) {
        sendMsg(msg, null, sendCallback);
    }

    public void sendImage(String imagePath) {
        final String newPath = com.avoscloud.leanchatlib.utils.PathUtils.getChatFilePath(Utils.uuid());
        PhotoUtils.compressImage(imagePath, newPath);
        try {
            AVIMImageMessage imageMsg = new AVIMImageMessage(newPath);
            sendMsg(imageMsg, newPath, sendCallback);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendLocation(double latitude, double longitude, String address) {
        AVIMLocationMessage locationMsg = new AVIMLocationMessage();
        AVGeoPoint geoPoint = new AVGeoPoint(latitude, longitude);
        locationMsg.setLocation(geoPoint);
        locationMsg.setText(address);
        sendMsg(locationMsg, null, sendCallback);
    }

    public void sendAudio(String audioPath) {
        try {
            AVIMAudioMessage audioMsg = new AVIMAudioMessage(audioPath);
            sendMsg(audioMsg, audioPath, sendCallback);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public interface SendCallback {
        void onError(Exception e);
        void onSuccess(AVIMTypedMessage msg);
    }
}
