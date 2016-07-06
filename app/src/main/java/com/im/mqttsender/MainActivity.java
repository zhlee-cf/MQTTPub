package com.im.mqttsender;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private EditText et;
    private TextView tv;
    private SimpleDateFormat ft;
    public static final String BROKER_URL = "tcp://q.emqtt.com:1883";
    public static final String TOPIC = "MQTT-Demo";
    private MqttClient serverClient;
    private String clientITd;
    private MqttClient mqttClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initData();
        subscribe();
    }

    private void subscribe() {
        try {
            mqttClient = new MqttClient(BROKER_URL, clientITd, new MemoryPersistence());
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable throwable) {
                }

                @Override
                public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
                    String message = new String(mqttMessage.getPayload(), "UTF-8");
                    MyLog.showLog("消息内容::" + message);
                    Message msg = handler.obtainMessage();
                    msg.obj = message;
                    handler.sendMessage(msg);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                }
            });
            mqttClient.connect();
            mqttClient.subscribe(TOPIC,2);
            MyLog.showLog("订阅成功");
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void initData() {
        ft = new SimpleDateFormat("HH:mm:ss", Locale.US);
        TelephonyManager telephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String deviceId = telephonyManager.getDeviceId();
        clientITd = deviceId + "#Sender";

        try {
            serverClient = new MqttClient(BROKER_URL, MqttClient.generateClientId(), new MemoryPersistence());
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void initView() {
        et = (EditText) findViewById(R.id.text);
        tv = (TextView) findViewById(R.id.textView);
    }

    public void publishMessage(View view) {

        String text = et.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            return;
        }
        text = "发送时间:" + ft.format(new Date()) + "**" + text;
        try {
            serverClient.connect();
            final MqttTopic temperatureTopic = serverClient.getTopic(TOPIC);
            final MqttMessage message = new MqttMessage(text.getBytes());
            message.setQos(2);
            message.setRetained(true);
            temperatureTopic.publish(message);
            MyLog.showLog("发布消息成功::" + message);
            et.setText("");
            serverClient.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            mqttClient.disconnect(0);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            tv.append("" + msg.obj + '\n');
        }
    };
}
