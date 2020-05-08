package io.sfrei.tracksearch.clients.setup;

import lombok.extern.slf4j.Slf4j;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

@Slf4j
public class ResponseProviderFactory extends Converter.Factory {

    public static ResponseProviderFactory create() {
        return new ResponseProviderFactory();
    }

    @Override
    public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
        return StringProvider.INSTANCE;
    }

    @Slf4j
    final static class StringProvider implements Converter<ResponseBody, ResponseWrapper> {
        static final StringProvider INSTANCE = new StringProvider();

        @Override
        public ResponseWrapper convert(ResponseBody responseBody) {
            return getWrapper(responseBody);
        }
    }

    public static ResponseWrapper getWrapper(ResponseBody responseBody) {
        if (responseBody != null) {
            try {
                String body = new String(responseBody.string().getBytes(StandardCharsets.UTF_8));
                return ResponseWrapper.of(Client.OK, body);
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
        return ResponseWrapper.empty();
    }

}
