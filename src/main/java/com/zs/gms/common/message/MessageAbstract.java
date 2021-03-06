package com.zs.gms.common.message;

import com.alibaba.fastjson.JSONObject;
import com.zs.gms.common.entity.GmsConstant;
import com.zs.gms.common.entity.GmsResponse;
import com.zs.gms.common.entity.Message;
import com.zs.gms.common.service.GmsService;
import com.zs.gms.common.service.RabbitMqService;
import com.zs.gms.common.utils.HttpContextUtil;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@Slf4j
public abstract class MessageAbstract implements MessageInterface{

    /**
     * 远程服务名称
     * */
    private  String serverName;

    /**
     * 交换机
     * */
    private  String exchangeName;

    public  MessageAbstract(String name,String exchangeName){
        this.serverName=name;
        this.exchangeName=exchangeName;
    }

    /**
     * 不需要等待响应,内部生成消息id
     * */
    @Override
    public void sendMessageNoResNoID(String routKry, String sendMessage) {
        String messageId = MessageFactory.messageIdGenerator(serverName);
        send2MQ(routKry,sendMessage,messageId);
    }

    @Override
    public void sendMessageNoResWithID(String messageId,String routKry, String sendMessage) {
        send2MQ(routKry,sendMessage,messageId);
    }

    /**
     * 内部生成消息id，前段请求，需要等待响应
     * */
    @Override
    public void sendMessageNoID(String routKry, String sendMessage, String resultMessage) {
        HttpServletResponse response = HttpContextUtil.getHttpServletResponse();
        MessageEntry entry = MessageFactory.createMessageEntry(serverName);
        send2MQ(routKry,sendMessage,entry.getMessageId());
        handlerResponse(response,resultMessage, entry.getMessageId());
        waitMqResponse(response, entry.getMessageId());
    }

    /**
     * 从外部转入消息id，前段请求，需要等待响应
     * @param messageId     消息id
     * @param routKry       路由建
     * @param  sendMessage   发送消息
     * @param resultMessage  返回结果
     * */
    @Override
    public void sendMessageWithID(String messageId, String routKry, String sendMessage, String resultMessage) {
        HttpServletResponse response = HttpContextUtil.getHttpServletResponse();
        handlerResponse(response,resultMessage, messageId);
        send2MQ(routKry,sendMessage,messageId);
        waitMqResponse(response, messageId);
    }

    /**
     * 向mq发送消息
     * */
    public void send2MQ(String routKry, String sendMessage,String messageId){
        RabbitMqService.sendMessage(exchangeName, routKry, sendMessage, messageId);
    }


    /**
     * 创建响应实体
     */
    public  void handlerResponse(HttpServletResponse response,String resultMessage, String messageId) {
        GmsResponse gmsResponse = new GmsResponse().message(resultMessage).success();
        Message message = new Message(response, gmsResponse);
        MessageEntry messageEntry = MessageFactory.getMessageEntry(messageId);
        if(messageEntry!=null){
            messageEntry.setMessage(message);
        }
    }


    /**
     * 等待响应
     */
    private  void waitMqResponse(HttpServletResponse response, String messageId) {
        try {
            synchronized (response) {
                response.wait(GmsConstant.WAIT_TIME);
            }
            MessageEntry entry = MessageFactory.getMessageEntry(messageId);
            MessageResult handleResult=null;
            if(entry!=null){
                 handleResult = entry.getHandleResult();
            }
            if (MessageResult.SENDING.equals(handleResult)) {//mq没有消息响应，返回失败信息
                entry.callback();
                GmsService.callResponse(new GmsResponse().badRequest().message("远程服务未响应!"),response);
                entry.setHandleResult(MessageResult.RESPONSE_EXPIRE);
                log.error("远程调用超时:messageId={},routeKey={}",messageId,entry.getRouteKey());
                return;
            }
            log.info("正常响应远程调用:messageId={},routeKey={}",entry.getMessageId(),entry.getRouteKey());
        } catch (InterruptedException e) {
            log.error("线程休眠异常", e);
        }
    }

}
