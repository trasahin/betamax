/*
 * Copyright 2011 Rob Fletcher
 *
 * Converted from Groovy to Java by Sean Freitag
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.freeside.betamax.handler;

import co.freeside.betamax.Recorder;
import co.freeside.betamax.message.Request;
import co.freeside.betamax.message.Response;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;

/**
 * The default handler chain used by all Betamax implementations.
 */
public class DefaultHandlerChain extends ChainedHttpHandler {
    public DefaultHandlerChain(Recorder recorder, HttpClient httpClient) {
        this.leftShift(new ViaSettingHandler())
            .leftShift(new TapeReader(recorder))
            .leftShift(new TapeWriter(recorder))
            .leftShift(new HeaderFilter())
            .leftShift(new TargetConnector(httpClient));
    }

    public DefaultHandlerChain(Recorder recorder) {
        this(recorder, newHttpClient());
    }

    private static DefaultHttpClient newHttpClient() {
        PoolingClientConnectionManager connectionManager = new PoolingClientConnectionManager();
        return new DefaultHttpClient(connectionManager);
    }

    @Override
    public Response handle(Request request) {
        return chain(request);
    }
}
