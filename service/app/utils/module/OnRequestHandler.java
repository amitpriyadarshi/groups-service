package utils.module;

import com.fasterxml.jackson.databind.ObjectMapper;
import controllers.BaseController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sunbird.message.ResponseCode;
import org.sunbird.response.Response;
import org.sunbird.util.JsonKey;
import play.http.ActionCreator;
import play.libs.Json;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class OnRequestHandler implements ActionCreator {
  
  private static Logger logger = LoggerFactory.getLogger(OnRequestHandler.class);

  @Override
  public Action createAction(Http.Request request, Method method) {
    
    return new Action.Simple() {
      @Override
      public CompletionStage<Result> call(Http.Request request) {
        CompletionStage<Result> result = null;
        request.getHeaders();
        //need to know it's necessity
        /*CompletionStage<Result> result = checkForServiceHealth(request);
        if (result != null) return result;*/
        String message = RequestInterceptor.verifyRequestData(request);
        // call method to set all the required params for the telemetry event(log)...
        //initializeRequestInfo(request, message, requestId);
        if (!JsonKey.USER_UNAUTH_STATES.contains(message)) {
          request.flash().put(JsonKey.USER_ID, message);
          request.flash().put(JsonKey.IS_AUTH_REQ, "false");
          result = delegate.call(request);
        } else if (JsonKey.UNAUTHORIZED.equals(message)) {
          result =
              onDataValidationError(request, message, ResponseCode.UNAUTHORIZED.getResponseCode());
        } else {
          result = delegate.call(request);
        }
        return result.thenApply(res -> res.withHeader("Access-Control-Allow-Origin", "*"));
      }
    };
  }

  /**
   * This method will do request data validation for GET method only. As a GET request user must
   * send some key in header.
   *
   * @param request Request
   * @param errorMessage String
   * @return CompletionStage<Result>
   */
  public CompletionStage<Result> onDataValidationError(
      Http.Request request, String errorMessage, int responseCode) {
    logger.error("Data error found--" + errorMessage);
    ResponseCode code = ResponseCode.getResponse(errorMessage);
    ResponseCode headerCode = ResponseCode.CLIENT_ERROR;
    Response resp = BaseController.createFailureResponse(request, code, headerCode);
    return CompletableFuture.completedFuture(Results.status(responseCode, Json.toJson(resp)));
  }
  
}
