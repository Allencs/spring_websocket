package com.allen.spring_websocket;

import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;



@ServerEndpoint(value = "/websocket/{userId}")
@Component
public class WebSocketService {

    private static Logger logger = LoggerFactory
            .getLogger(WebSocketService.class);

    //静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。
    private static int onlineCount = 0;

    //concurrent包的线程安全Set，用来存放每个客户端对应的MyWebSocket对象。
    private static ConcurrentHashMap<String, WebSocketService> webSocketSet = new ConcurrentHashMap<>();

    //与某个客户端的连接会话，需要通过它来给客户端发送数据
    private Session session;

    //当前发消息的人员编号
    private String userId = "";


    /**
     * 连接建立成功调用的方法*/
    @OnOpen
    public void onOpen(@PathParam(value = "userId") String param, Session session) {
        this.userId = param;
        this.session = session;
        webSocketSet.put(param, this);     //加入set中
        addOnlineCount();           //在线数加1
        logger.info("有新连接加入！当前在线人数为" + getOnlineCount());
        try {
            this.sendMessage("Connect Successfully");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        if (!userId.equals("")){
            webSocketSet.remove(userId);  //从set中删除
        }
        subOnlineCount();           //在线数减1
        logger.info("用户id：" + userId + "关闭连接 | 当前在线人数为" + getOnlineCount());
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息*/
    @OnMessage
    public void onMessage(String message) throws IOException {
        logger.info("来自用户" + userId + "的消息 | " + message);
//        byte[] b = "ping".getBytes();
//        ByteBuffer bytebuffer = ByteBuffer.wrap(b);
        sendToUser(userId, "Got the message");
//        sendToAll();

    }


    /**
     * 给指定的人发消息
     * @param s_userId 用户id
     * @param message 用户发送的消息
     */
    public void sendToUser(String s_userId, String message){

        try {
            webSocketSet.get(s_userId).sendMessage("hi user " + s_userId + " | " + message);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    /**
     *发送给所有用户
     */
    public void sendToAll(){
        for (String key : webSocketSet.keySet()) {
            try {
                if (userId.equals(key)) {
                    webSocketSet.get(key).sendMessage("hi user " + key + " | " + "Got the message!");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * 获取客户端的Pong消息
     * @param msg 客户端发送的pong数据
     */
    @OnMessage
    public void echoPongMessage(PongMessage msg){
        ByteBuffer buffer = msg.getApplicationData();
        byte[] content = new byte[buffer.limit()];
        buffer.get(content);
//        String pong_message = new String(content);
//        logger.info("Get Pong message: " + pong_message);
        byte[] b = "ping".getBytes();
        ByteBuffer bytebuffer = ByteBuffer.wrap(b);
        try {
            this.sendPing(bytebuffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        buffer.clear();

    }

    /**
     * 发生错误时调用
     * */
     @OnError
     public void onError(Throwable error) {
//         logger.info("发生错误" + " | " + error.getClass().getName());
         error.printStackTrace();
//         System.out.println(error.getClass().getName());
     }


     private synchronized void sendMessage(String message) throws IOException {
         this.session.getBasicRemote().sendText(message);
//         this.session.getAsyncRemote().sendText(message);
     }

     /** 发送心跳包 */
     private synchronized void sendPing(ByteBuffer message) throws IOException{
         this.session.getBasicRemote().sendPing(message);
     }




     /**
      * 群发自定义消息

    public static void sendInfo(String message) throws IOException {
        for (WebSocketService item : webSocketSet) {
            try {
                item.sendMessage(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }* */

    private static synchronized int getOnlineCount() {
        return onlineCount;
    }

    private static synchronized void addOnlineCount() {
        WebSocketService.onlineCount++;
    }

    private static synchronized void subOnlineCount() {
        WebSocketService.onlineCount--;
    }
}
