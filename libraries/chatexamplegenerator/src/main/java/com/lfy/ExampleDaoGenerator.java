package com.lfy;

import de.greenrobot.daogenerator.DaoGenerator;
import de.greenrobot.daogenerator.Entity;
import de.greenrobot.daogenerator.Schema;

public class ExampleDaoGenerator {

    public static void main(String[] args) throws Exception {
        Schema schema = new Schema(1,"com.lfy.bean");
        schema.setDefaultJavaPackageDao("com.lfy.dao");
        addChatMessage(schema);
        DaoGenerator generator = new DaoGenerator();
        generator.generateAll(schema, System.getProperty("user.dir")+"/libraries/chatexamplegenerator/src/main/java-gen");
    }

    /**
     * 更新Message
     * @param schema
     */
    private static void addChatMessage(Schema schema){
        Entity message = schema.addEntity("Message");
        //conv_id
        message.addStringProperty("conv_id").primaryKey();
        //msg_id
        message.addStringProperty("message_id");
        //message_text
        message.addStringProperty("message_text");
    }


}
