package com.allen.spring_websocket;


import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CopyOnWriteArraySet;



@ServerEndpoint(value = "/websocket")
@Component
public class WebSocketService {

    private static Logger logger = LoggerFactory
            .getLogger(WebSocketService.class);

    //静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。
    private static int onlineCount = 0;

    //concurrent包的线程安全Set，用来存放每个客户端对应的MyWebSocket对象。
    private static CopyOnWriteArraySet<WebSocketService> webSocketSet = new CopyOnWriteArraySet<>();

    //与某个客户端的连接会话，需要通过它来给客户端发送数据
    private Session session;

//    private long startTime =  System.currentTimeMillis();
//
//    private long endTime =  System.currentTimeMillis();
//    long usedTime = (endTime-startTime)/1000;

    /**
     * 连接建立成功调用的方法*/
    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        webSocketSet.add(this);     //加入set中
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
        webSocketSet.remove(this);  //从set中删除
        subOnlineCount();           //在线数减1
        logger.info("有一连接关闭！当前在线人数为" + getOnlineCount());
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息*/
    @OnMessage
    public void onMessage(String message) throws Exception {
        logger.info("来自客户端的消息:" + message);
        byte[] b = "ping".getBytes();
        ByteBuffer bytebuffer = ByteBuffer.wrap(b);

        //群发消息
        for (WebSocketService item : webSocketSet) {
            if (message.equals("push")){
                Thread.sleep(1000);
                int count = 0;
                while (count < 10) {
                    try {
                        Thread.sleep(1000);
                        item.sendMessage("Spring WebSocket");

                        count++;

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }else {
                try {
                    item.sendMessage("SUCCESS");

                    sendPing(bytebuffer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
        String pong_message = new String(content);
        logger.info("Get Pong message: " + pong_message);
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
//         logger.info("发生错误");
         error.printStackTrace();
//         System.out.println(error.getClass().getName());
     }


     private void sendMessage(String message) throws IOException {
         this.session.getBasicRemote().sendText(message);
         //this.session.getAsyncRemote().sendText(message);
     }

     /** 发送心跳包 */
     private void sendPing(ByteBuffer message) throws IOException{
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
