package com.lfy;

import de.greenrobot.daogenerator.DaoGenerator;
import de.greenrobot.daogenerator.Entity;
import de.greenrobot.daogenerator.Schema;

public class ExampleDaoGenerator {

    public static void main(String[] args) throws Exception {
        Schema schema = new Schema(2,"com.lfy.bean");
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
        message.addStringProperty("houseId").primaryKey();
        message.addStringProperty("leanId");
        message.addStringProperty("message");
        message.addStringProperty("date");
        message.addStringProperty("is_read");
        message.addStringProperty("auditType");
    }


}
