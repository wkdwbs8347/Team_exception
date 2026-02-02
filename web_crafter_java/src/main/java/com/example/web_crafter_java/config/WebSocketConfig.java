package com.example.web_crafter_java.config;

import java.security.Principal;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        ThreadPoolTaskScheduler te = new ThreadPoolTaskScheduler();
        te.setPoolSize(1);
        te.setThreadNamePrefix("wss-heartbeat-");
        te.initialize();

        // ✅ /topic + /queue 를 브로커로 열어야 /user/queue/... 도 안정적으로 동작함
        config.enableSimpleBroker("/topic", "/queue")
              .setHeartbeatValue(new long[]{10000, 10000})
              .setTaskScheduler(te);

        // ✅ @MessageMapping("/chat/send") 같은 app prefix
        config.setApplicationDestinationPrefixes("/app");

        // ✅ /user/queue/... 유저 라우팅 prefix
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/wsproject")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new HttpSessionHandshakeInterceptor())
                .withSockJS();
    }

    // ✅ CONNECT 때 x-user-id를 Principal로 세팅해야 /user/queue 가 “유저별”로 정확히 감
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor acc = StompHeaderAccessor.wrap(message);

                if (StompCommand.CONNECT.equals(acc.getCommand())) {
                    String userId = acc.getFirstNativeHeader("x-user-id");

                    if (userId != null) {
                        // ✅ 세션 attributes에도 넣어두면 디버깅/참조용으로 좋음
                        if (acc.getSessionAttributes() != null) {
                            acc.getSessionAttributes().put("userId", userId);
                        }

                        // ✅ 핵심: Principal 세팅
                        Principal p = new Principal() {
                            @Override
                            public String getName() {
                                return userId;
                            }
                        };
                        acc.setUser(p);

                        System.out.println("✅ STOMP CONNECT Principal set userId=" + userId);
                    } else {
                        System.out.println("❌ STOMP CONNECT missing x-user-id");
                    }
                }

                return message;
            }
        });
    }
}
