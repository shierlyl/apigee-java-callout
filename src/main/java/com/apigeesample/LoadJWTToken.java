package com.apigeesample;

import java.net.HttpCookie;
import java.util.List;

import com.apigee.flow.execution.Action;
import com.apigee.flow.execution.ExecutionContext;
import com.apigee.flow.execution.ExecutionResult;
import com.apigee.flow.execution.spi.Execution;
import com.apigee.flow.message.Message;
import com.apigee.flow.message.MessageContext;

import redis.clients.jedis.Jedis;

public class LoadJWTToken implements Execution {

  private static final String REDIS_HOST_VAR = "auth.service.redis.host";
  private static final String REDIS_PORT_VAR = "auth.service.redis.port";
  private static final String USE_MOCK_REDIS_HEADER = "UseMockRedis";
  private static final String COOKIE_HEADER = "Cookie";
  private static final String COOKIE_JSESSIONID = "JSessionId";


  @Override
  public ExecutionResult execute(MessageContext messageContext, ExecutionContext executionContext) {
    Message message = messageContext.getMessage();
    boolean useMockRedis = Boolean.parseBoolean(message.getHeader(USE_MOCK_REDIS_HEADER));
    String jwtToken;
    if (useMockRedis) {
      jwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    } else {
      String redisHost = messageContext.getVariable(REDIS_HOST_VAR);
      Integer redisPort = messageContext.<Integer>getVariable(REDIS_PORT_VAR);
      if (redisHost != null) {
        Jedis client = redisPort == null ? new Jedis(redisHost): new Jedis(redisHost, redisPort);
        jwtToken = client.get(extractJSessionId(message.getHeader(COOKIE_HEADER)));
      } else {
        return generateError("Missing redis config");
      }
    }
    ExecutionResult executionResult = new ExecutionResult(true, Action.CONTINUE);
    executionResult.addProperty("redis.jwt", jwtToken);
    return executionResult;
  }

  private String extractJSessionId(String cookieStr) {
    List<HttpCookie> cookieList = HttpCookie.parse(cookieStr);
    String jSessionId = null;
    for (HttpCookie cookie : cookieList) {
      if (COOKIE_JSESSIONID.equals(cookie.getName())) {
        jSessionId = cookie.getValue();
        break;
      }
    }
    return jSessionId;
  }

  private ExecutionResult generateError(String errorMsg) {
    ExecutionResult error = ExecutionResult.ABORT;
    error.setErrorResponse(errorMsg);
    return error;
  }
}
